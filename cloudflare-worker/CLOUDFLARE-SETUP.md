# راهنمای راه‌اندازی Cloudflare Workers + D1

## مرحله 1: نصب Wrangler CLI

```bash
npm install -g wrangler
```

## مرحله 2: لاگین به Cloudflare

```bash
wrangler login
```

این دستور یک صفحه مرورگر باز میکنه و باید با اکانت Cloudflare خودت لاگین کنی.

## مرحله 3: ساخت D1 Database

```bash
cd cloudflare-worker
wrangler d1 create contacts-db
```

**خروجی این دستور شبیه این میشه:**

```
✅ Successfully created DB 'contacts-db'

[[d1_databases]]
binding = "DB"
database_name = "contacts-db"
database_id = "xxxx-xxxx-xxxx-xxxx-xxxx"
```

**مهم:** `database_id` رو کپی کن و توی فایل `wrangler.toml` جایگزین کن.

## مرحله 4: اجرای Schema (ساخت جدول)

```bash
wrangler d1 execute contacts-db --file=schema.sql
```

این دستور جدول `contacts` رو میسازه.

## مرحله 5: تست Local (اختیاری)

```bash
wrangler dev
```

این دستور یک سرور local روی `http://localhost:8787` میسازه که میتونی تست کنی.

## مرحله 6: Deploy به Production

```bash
wrangler deploy
```

این دستور Worker رو روی Cloudflare deploy میکنه و بهت یک URL میده مثل:
`https://testapp.YOURNAME.workers.dev`

---

## دستورات مفید دیگر:

### مشاهده لیست دیتابیس‌ها
```bash
wrangler d1 list
```

### اجرای Query دستی
```bash
wrangler d1 execute contacts-db --command="SELECT * FROM contacts"
```

### مشاهده جداول
```bash
wrangler d1 execute contacts-db --command="SELECT name FROM sqlite_master WHERE type='table'"
```

### پاک کردن همه داده‌ها
```bash
wrangler d1 execute contacts-db --command="DELETE FROM contacts"
```

### حذف دیتابیس (احتیاط!)
```bash
wrangler d1 delete contacts-db
```

---

## Variable Names مهم:

- **Database Binding:** `DB` (در wrangler.toml و worker.js)
- **Database Name:** `contacts-db`
- **Table Name:** `contacts`

---

## API Endpoints بعد از Deploy:

- `GET /` - وضعیت API
- `POST /sync` - دریافت مخاطبین از اپ
- `GET /contacts` - نمایش همه مخاطبین
- `GET /contacts/:id` - نمایش یک مخاطب
- `GET /stats` - آمار دیتابیس
- `POST /clear` - پاک کردن همه مخاطبین

---

## مثال تست با curl:

```bash
# تست sync
curl -X POST https://testapp.YOURNAME.workers.dev/sync \
  -H "Content-Type: application/json" \
  -d '{
    "contacts": [
      {"name": "علی احمدی", "phone": "09123456789", "type": "موبایل"}
    ],
    "timestamp": 1234567890000
  }'

# مشاهده مخاطبین
curl https://testapp.YOURNAME.workers.dev/contacts

# آمار
curl https://testapp.YOURNAME.workers.dev/stats
```
