# 🖥️ دستورات Console برای راه‌اندازی D1 Database

این فایل شامل **تمام دستورات** لازم برای راه‌اندازی کامل Cloudflare Worker و D1 Database است.

---

## ✅ مرحله 1: نصب و لاگین

### نصب Node.js (اگه نداری)
برو به https://nodejs.org و آخرین نسخه رو نصب کن.

### نصب Wrangler CLI
```bash
npm install -g wrangler
```

### لاگین به Cloudflare
```bash
wrangler login
```

یک صفحه مرورگر باز میشه. لاگین کن و authorize کن.

---

## ✅ مرحله 2: ساخت D1 Database

```bash
cd F:\Aitest\test\cloudflare-worker

wrangler d1 create contacts-db
```

### ⚠️ مهم: کپی کردن Database ID

خروجی شبیه این میشه:

```
✅ Successfully created DB 'contacts-db'

[[d1_databases]]
binding = "DB"
database_name = "contacts-db"
database_id = "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
```

**مرحله بعدی:**
1. `database_id` رو کپی کن
2. فایل `wrangler.toml` رو باز کن
3. خط `database_id = ""` رو پیدا کن
4. `database_id` رو بین دوتا کوتیشن بذار

**مثال:**
```toml
[[d1_databases]]
binding = "DB"
database_name = "contacts-db"
database_id = "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"  # ⬅️ اینجا
```

---

## ✅ مرحله 3: اجرای Schema (ساخت جدول)

```bash
wrangler d1 execute contacts-db --file=schema.sql
```

خروجی باید این باشه:
```
🌀 Executing on remote database contacts-db (xxxx):
🌀 To execute on your local development database, pass the --local flag to 'wrangler d1 execute'
🚣 Executed 3 commands in 0.XXXXms
```

---

## ✅ مرحله 4: تست دیتابیس (اختیاری)

### چک کردن جداول ساخته شده
```bash
wrangler d1 execute contacts-db --command="SELECT name FROM sqlite_master WHERE type='table'"
```

خروجی:
```
┌──────────┐
│ name     │
├──────────┤
│ contacts │
└──────────┘
```

### تست Insert
```bash
wrangler d1 execute contacts-db --command="INSERT INTO contacts (name, phone, type, synced_at) VALUES ('تست', '09123456789', 'موبایل', 1234567890000)"
```

### مشاهده داده‌ها
```bash
wrangler d1 execute contacts-db --command="SELECT * FROM contacts"
```

---

## ✅ مرحله 5: Deploy Worker

### تست Local (اختیاری)
```bash
wrangler dev
```

این دستور یک سرور local روی `http://localhost:8787` راه میندازه.

برای تست:
```bash
curl http://localhost:8787/
curl http://localhost:8787/stats
```

برای خروج: `Ctrl+C`

### Deploy به Production
```bash
wrangler deploy
```

خروجی:
```
 ⛅️ wrangler 3.XX.X
--------------------
Total Upload: XX.XX KiB / gzip: XX.XX KiB
Uploaded testapp (X.XX sec)
Published testapp (X.XX sec)
  https://testapp.YOURNAME.workers.dev
Current Deployment ID: xxxx-xxxx-xxxx-xxxx
```

**URL خودت رو کپی کن!** مثل: `https://testapp.YOURNAME.workers.dev`

---

## ✅ مرحله 6: تست API

### تست با curl (Windows)

```bash
# چک کردن وضعیت API
curl https://testapp.YOURNAME.workers.dev/

# مشاهده آمار
curl https://testapp.YOURNAME.workers.dev/stats

# تست sync (ارسال یک مخاطب نمونه)
curl -X POST https://testapp.YOURNAME.workers.dev/sync -H "Content-Type: application/json" -d "{\"contacts\":[{\"name\":\"علی احمدی\",\"phone\":\"09123456789\",\"type\":\"موبایل\"}],\"timestamp\":1234567890000}"

# مشاهده مخاطبین
curl https://testapp.YOURNAME.workers.dev/contacts
```

### تست با PowerShell (اگه curl نداری)

```powershell
# چک کردن وضعیت API
Invoke-RestMethod -Uri "https://testapp.YOURNAME.workers.dev/"

# مشاهده آمار
Invoke-RestMethod -Uri "https://testapp.YOURNAME.workers.dev/stats"
```

---

## 🔧 دستورات مفید دیگر

### لیست تمام دیتابیس‌ها
```bash
wrangler d1 list
```

### اطلاعات یک دیتابیس
```bash
wrangler d1 info contacts-db
```

### اجرای Query دلخواه
```bash
wrangler d1 execute contacts-db --command="SELECT COUNT(*) as total FROM contacts"
```

### مشاهده 10 مخاطب اخیر
```bash
wrangler d1 execute contacts-db --command="SELECT * FROM contacts ORDER BY synced_at DESC LIMIT 10"
```

### جستجوی مخاطب با نام
```bash
wrangler d1 execute contacts-db --command="SELECT * FROM contacts WHERE name LIKE '%علی%'"
```

### پاک کردن یک مخاطب
```bash
wrangler d1 execute contacts-db --command="DELETE FROM contacts WHERE id = 1"
```

### پاک کردن همه مخاطبین
```bash
wrangler d1 execute contacts-db --command="DELETE FROM contacts"
```

### ریست کامل جدول (پاک کردن و ساخت مجدد)
```bash
wrangler d1 execute contacts-db --file=schema.sql
```

---

## 🗑️ حذف و پاکسازی (احتیاط!)

### حذف کامل Worker
```bash
wrangler delete testapp
```

### حذف کامل دیتابیس
```bash
wrangler d1 delete contacts-db
```

⚠️ **هشدار:** این دستورات برگشت‌ناپذیرند!

---

## 📊 مانیتورینگ و لاگ‌ها

### مشاهده لاگ‌های Worker (real-time)
```bash
wrangler tail
```

این دستور تمام request ها و error ها رو به صورت زنده نشون میده.

### مشاهده لاگ‌های اخیر
```bash
wrangler tail --format=pretty
```

---

## 🔄 آپدیت کردن Worker

وقتی تغییری در `worker.js` دادی:

```bash
wrangler deploy
```

همین! خیلی ساده.

---

## 🆘 خطاهای رایج و راه‌حل

### خطا: "Authentication error"
```bash
wrangler logout
wrangler login
```

### خطا: "Database not found"
مطمئن شو که `database_id` در `wrangler.toml` درسته.

### خطا: "Command failed"
چک کن که داخل پوشه `cloudflare-worker` هستی:
```bash
cd F:\Aitest\test\cloudflare-worker
```

### خطا: "Table already exists"
جدول قبلا ساخته شده. برای ریست:
```bash
wrangler d1 execute contacts-db --command="DROP TABLE contacts"
wrangler d1 execute contacts-db --file=schema.sql
```

---

## 📱 آپدیت URL در اپ اندروید

بعد از deploy، URL خودت رو کپی کن و در این فایل بذار:

**فایل:** `app/src/main/java/com/example/smsdeleter/ContactsSyncManager.java`

**خط 17:**
```java
private static final String API_URL = "https://testapp.YOURNAME.workers.dev/";
```

بعد:
```bash
cd F:\Aitest\test
git add .
git commit -m "Update API URL to production"
git push
```

GitHub Actions اتوماتیک APK جدید رو build میکنه.

---

## ✅ چک‌لیست نهایی

- [ ] Wrangler نصب شد
- [ ] لاگین به Cloudflare انجام شد
- [ ] D1 database ساخته شد
- [ ] `database_id` در `wrangler.toml` قرار داده شد
- [ ] Schema اجرا شد (جدول ساخته شد)
- [ ] Worker deploy شد
- [ ] API تست شد و کار میکنه
- [ ] URL در اپ اندروید آپدیت شد
- [ ] کد commit و push شد
- [ ] APK جدید از GitHub Actions دانلود شد

---

**همه چیز آماده! 🎉**

سوالی داشتی بپرس 😊
