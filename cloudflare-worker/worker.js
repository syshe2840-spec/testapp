/**
 * Cloudflare Worker for Contacts Sync API
 * متصل به D1 Database برای ذخیره‌سازی مخاطبین
 */

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);
    const path = url.pathname;

    // CORS headers
    const corsHeaders = {
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
      'Access-Control-Allow-Headers': 'Content-Type, X-API-Key',
    };

    // Handle CORS preflight
    if (request.method === 'OPTIONS') {
      return new Response(null, { headers: corsHeaders });
    }

    try {
      // Root endpoint - وضعیت API
      if (path === '/' && request.method === 'GET') {
        return jsonResponse({
          status: 'ok',
          message: 'Contacts Sync API is running',
          version: '1.0.0',
          endpoints: {
            'POST /sync': 'دریافت و ذخیره مخاطبین',
            'GET /contacts': 'نمایش همه مخاطبین',
            'GET /contacts/:id': 'نمایش یک مخاطب',
            'GET /stats': 'آمار دیتابیس',
            'POST /clear': 'پاک کردن همه مخاطبین'
          }
        }, corsHeaders);
      }

      // POST /sync - دریافت مخاطبین از اپ
      if (path === '/sync' && request.method === 'POST') {
        const data = await request.json();

        if (!data.contacts || !Array.isArray(data.contacts)) {
          return jsonResponse({ error: 'فرمت داده اشتباه است' }, corsHeaders, 400);
        }

        // پاک کردن مخاطبین قبلی (اختیاری)
        await env.DB.prepare('DELETE FROM contacts').run();

        // ذخیره مخاطبین جدید
        let insertedCount = 0;
        for (const contact of data.contacts) {
          try {
            await env.DB.prepare(
              'INSERT INTO contacts (name, phone, type, synced_at) VALUES (?, ?, ?, ?)'
            ).bind(
              contact.name || '',
              contact.phone || '',
              contact.type || 'نامشخص',
              Date.now()
            ).run();
            insertedCount++;
          } catch (err) {
            console.error('خطا در ذخیره مخاطب:', err);
          }
        }

        return jsonResponse({
          success: true,
          message: `${insertedCount} مخاطب با موفقیت ذخیره شد`,
          timestamp: data.timestamp,
          total: insertedCount
        }, corsHeaders);
      }

      // GET /contacts - نمایش همه مخاطبین
      if (path === '/contacts' && request.method === 'GET') {
        const page = parseInt(url.searchParams.get('page') || '1');
        const limit = parseInt(url.searchParams.get('limit') || '50');
        const search = url.searchParams.get('search') || '';
        const offset = (page - 1) * limit;

        let query = 'SELECT * FROM contacts';
        let countQuery = 'SELECT COUNT(*) as total FROM contacts';
        let params = [];

        if (search) {
          query += ' WHERE name LIKE ? OR phone LIKE ?';
          countQuery += ' WHERE name LIKE ? OR phone LIKE ?';
          const searchParam = `%${search}%`;
          params = [searchParam, searchParam];
        }

        query += ' ORDER BY name ASC LIMIT ? OFFSET ?';

        // دریافت تعداد کل
        const countResult = search
          ? await env.DB.prepare(countQuery).bind(...params).first()
          : await env.DB.prepare(countQuery).first();

        const total = countResult.total;

        // دریافت داده‌ها
        const { results } = search
          ? await env.DB.prepare(query).bind(...params, limit, offset).all()
          : await env.DB.prepare(query).bind(limit, offset).all();

        return jsonResponse({
          success: true,
          data: results,
          pagination: {
            page,
            limit,
            total,
            totalPages: Math.ceil(total / limit)
          }
        }, corsHeaders);
      }

      // GET /contacts/:id - نمایش یک مخاطب
      const contactMatch = path.match(/^\/contacts\/(\d+)$/);
      if (contactMatch && request.method === 'GET') {
        const id = parseInt(contactMatch[1]);
        const contact = await env.DB.prepare(
          'SELECT * FROM contacts WHERE id = ?'
        ).bind(id).first();

        if (!contact) {
          return jsonResponse({ error: 'مخاطب پیدا نشد' }, corsHeaders, 404);
        }

        return jsonResponse({ success: true, data: contact }, corsHeaders);
      }

      // GET /stats - آمار دیتابیس
      if (path === '/stats' && request.method === 'GET') {
        const stats = await env.DB.prepare(`
          SELECT
            COUNT(*) as total_contacts,
            COUNT(DISTINCT phone) as unique_phones,
            MAX(synced_at) as last_sync
          FROM contacts
        `).first();

        const typeStats = await env.DB.prepare(`
          SELECT type, COUNT(*) as count
          FROM contacts
          GROUP BY type
        `).all();

        return jsonResponse({
          success: true,
          stats: {
            ...stats,
            last_sync_date: stats.last_sync
              ? new Date(stats.last_sync).toISOString()
              : null,
            by_type: typeStats.results
          }
        }, corsHeaders);
      }

      // POST /clear - پاک کردن همه مخاطبین (فقط برای تست)
      if (path === '/clear' && request.method === 'POST') {
        const result = await env.DB.prepare('DELETE FROM contacts').run();

        return jsonResponse({
          success: true,
          message: 'همه مخاطبین پاک شدند',
          deleted: result.changes || 0
        }, corsHeaders);
      }

      // 404 - مسیر پیدا نشد
      return jsonResponse({
        error: 'مسیر پیدا نشد',
        path: path,
        method: request.method
      }, corsHeaders, 404);

    } catch (error) {
      console.error('خطای سرور:', error);
      return jsonResponse({
        error: 'خطای داخلی سرور',
        message: error.message
      }, corsHeaders, 500);
    }
  }
};

// کمکی برای ساخت JSON Response
function jsonResponse(data, corsHeaders = {}, status = 200) {
  return new Response(JSON.stringify(data, null, 2), {
    status,
    headers: {
      'Content-Type': 'application/json; charset=utf-8',
      ...corsHeaders
    }
  });
}
