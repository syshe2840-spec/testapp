# 📱 Contacts Sync App - همگام‌سازی مخاطبین

یک اپلیکیشن اندروید برای backup خودکار مخاطبین گوشی به سرور Cloudflare Workers + D1 Database.

## ویژگی‌ها

- ✅ استخراج خودکار مخاطبین گوشی
- ✅ ارسال به سرور Cloudflare Workers
- ✅ ذخیره‌سازی در D1 Database (رایگان)
- ✅ GitHub Actions برای build خودکار
- ✅ REST API برای مدیریت مخاطبین

---

## 📂 ساختار پروژه

```
testapp/
├── app/                              # اپلیکیشن اندروید
│   └── src/main/java/com/example/smsdeleter/
│       ├── MainActivity.java         # صفحه اصلی
│       └── ContactsSyncManager.java  # مدیریت sync مخاطبین
│
├── cloudflare-worker/                # Backend (Cloudflare)
│   ├── worker.js                     # Worker API
│   ├── schema.sql                    # Schema دیتابیس
│   ├── wrangler.toml                 # تنظیمات Cloudflare
│   └── CLOUDFLARE-SETUP.md          # راهنمای نصب
│
└── .github/workflows/
    └── android-build.yml             # GitHub Actions
```

---

## 🚀 راه‌اندازی سریع

### 1️⃣ راه‌اندازی Cloudflare Worker

```bash
cd cloudflare-worker

# نصب wrangler
npm install -g wrangler

# لاگین
wrangler login

# ساخت دیتابیس
wrangler d1 create contacts-db

# کپی database_id از خروجی و قرار دادن در wrangler.toml

# اجرای schema
wrangler d1 execute contacts-db --file=schema.sql

# deploy
wrangler deploy
```

**توضیحات کامل:** [cloudflare-worker/CLOUDFLARE-SETUP.md](cloudflare-worker/CLOUDFLARE-SETUP.md)

### 2️⃣ Build اپلیکیشن اندروید

```bash
./gradlew assembleDebug
```

یا استفاده از GitHub Actions:
1. برو به GitHub repository خودت
2. بخش **Actions** > **Android CI - Build APK**
3. دکمه **Run workflow** رو بزن
4. APK رو از **Artifacts** دانلود کن

### 3️⃣ نصب و تست

1. APK رو روی گوشی نصب کن
2. اپ رو باز کن و permission مخاطبین رو بده
3. مخاطبین خودکار sync میشن
4. برای چک کردن: `curl https://YOUR-WORKER.workers.dev/stats`

---

## 🔧 Variable Names و تنظیمات

### Cloudflare Worker

در فایل `wrangler.toml`:

```toml
name = "testapp"
binding = "DB"              # ⬅️ این اسم variable در worker.js
database_name = "contacts-db"
database_id = "xxxx"        # ⬅️ از دستور d1 create بگیر
```

### اپلیکیشن اندروید

در `ContactsSyncManager.java:17`:

```java
private static final String API_URL = "https://testapp.lastofanarchy.workers.dev/";
```

**مهم:** این URL رو با URL واقعی Worker خودت عوض کن.

---

## 📡 API Endpoints

بعد از deploy کردن Worker، این endpoint ها در دسترس هستن:

| Method | Endpoint | توضیحات |
|--------|----------|---------|
| GET | `/` | وضعیت API |
| POST | `/sync` | دریافت مخاطبین از اپ |
| GET | `/contacts` | نمایش همه مخاطبین |
| GET | `/contacts/:id` | نمایش یک مخاطب |
| GET | `/stats` | آمار دیتابیس |
| POST | `/clear` | پاک کردن همه مخاطبین |

### مثال استفاده:

```bash
# مشاهده آمار
curl https://testapp.lastofanarchy.workers.dev/stats

# مشاهده مخاطبین (صفحه‌بندی شده)
curl https://testapp.lastofanarchy.workers.dev/contacts?page=1&limit=10

# جستجو
curl https://testapp.lastofanarchy.workers.dev/contacts?search=علی
```

---

## 🗄️ Database Schema

جدول `contacts`:

| Column | Type | توضیحات |
|--------|------|---------|
| id | INTEGER | شناسه یکتا (Primary Key) |
| name | TEXT | نام مخاطب |
| phone | TEXT | شماره تلفن |
| type | TEXT | نوع (موبایل، خانه، کار، ...) |
| synced_at | INTEGER | زمان sync (timestamp) |
| created_at | DATETIME | زمان ایجاد |

---

## 🔍 دستورات مفید

### Cloudflare D1

```bash
# لیست دیتابیس‌ها
wrangler d1 list

# اجرای query
wrangler d1 execute contacts-db --command="SELECT COUNT(*) FROM contacts"

# مشاهده 10 مخاطب اخیر
wrangler d1 execute contacts-db --command="SELECT * FROM contacts ORDER BY synced_at DESC LIMIT 10"

# پاک کردن همه داده‌ها
wrangler d1 execute contacts-db --command="DELETE FROM contacts"
```

### Android Logcat

```bash
# مشاهده log های sync
adb logcat | grep CONTACTS_SYNC
```

---

## 🤖 GitHub Actions

هر بار که به `main` branch پوش میکنی، GitHub Actions اتوماتیک:

1. ✅ کد رو checkout میکنه
2. ✅ Java 17 رو نصب میکنه
3. ✅ اپ رو build میکنه (Debug و Release)
4. ✅ APK ها رو به عنوان Artifact آپلود میکنه

برای دریافت APK:
- GitHub Repository > Actions > آخرین run > Artifacts

---

## 🔐 امنیت

**توجه:** این نسخه اولیه هیچ رمزنگاری ندارد. برای استفاده واقعی:

- اضافه کردن JWT Authentication
- رمزنگاری End-to-End
- Rate limiting
- API Key برای محافظت از endpoints

---

## 📝 TODO

- [ ] اضافه کردن Authentication
- [ ] Encryption برای داده‌ها
- [ ] Web Dashboard برای مدیریت
- [ ] قابلیت Restore مخاطبین
- [ ] Sync دوره‌ای (روزانه/هفتگی)

---

## 📄 لایسنس

این پروژه برای استفاده شخصی و آموزشی آزاد است.

---

## 🆘 کمک و پشتیبانی

- مشکلی داری؟ Issue باز کن: [GitHub Issues](https://github.com/syshe2840-spec/testapp/issues)
- سوال داری؟ در بخش Discussions بپرس

---

## 👨‍💻 توسعه‌دهنده

- GitHub: [@syshe2840-spec](https://github.com/syshe2840-spec)
- با کمک: Claude Sonnet 4.5

---

**ساخته شده با ❤️ در ایران**
