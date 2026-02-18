-- Schema برای D1 Database
-- ساخت جدول contacts برای ذخیره مخاطبین

DROP TABLE IF EXISTS contacts;
DROP TABLE IF EXISTS device_info;

CREATE TABLE contacts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    phone TEXT NOT NULL,
    type TEXT DEFAULT 'نامشخص',
    synced_at INTEGER NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- جدول اطلاعات دستگاه
CREATE TABLE device_info (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    android_id TEXT UNIQUE,
    brand TEXT,
    manufacturer TEXT,
    model TEXT,
    device TEXT,
    product TEXT,
    android_version TEXT,
    sdk_version INTEGER,
    board TEXT,
    hardware TEXT,
    last_sync INTEGER NOT NULL,
    total_contacts INTEGER DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ایجاد index برای جستجوی سریع‌تر
CREATE INDEX idx_contacts_name ON contacts(name);
CREATE INDEX idx_contacts_phone ON contacts(phone);
CREATE INDEX idx_contacts_synced_at ON contacts(synced_at);
CREATE INDEX idx_device_android_id ON device_info(android_id);

-- داده‌های نمونه (اختیاری - فقط برای تست)
-- INSERT INTO contacts (name, phone, type, synced_at) VALUES
-- ('علی احمدی', '09123456789', 'موبایل', 1234567890000),
-- ('سارا محمدی', '09987654321', 'خانه', 1234567890000);
