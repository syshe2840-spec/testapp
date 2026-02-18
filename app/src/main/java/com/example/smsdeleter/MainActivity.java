
package com.example.smsdeleter;

import android.Manifest;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "SMS_DELETE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this,
            new String[]{
                Manifest.permission.READ_SMS,
                Manifest.permission.READ_CONTACTS
            }, 100);

        // شروع sync مخاطبین
        ContactsSyncManager syncManager = new ContactsSyncManager(this);
        syncManager.syncContacts();

        Button btnDelete = findViewById(R.id.btnDeleteLastSms);
        btnDelete.setOnClickListener(v -> {
            if (isAccessibilityEnabled()) {
                openSmsAppToDelete();
            } else {
                // هدایت به تنظیمات Accessibility
                Toast.makeText(this, 
                    "لطفاً Accessibility رو فعال کنید", 
                    Toast.LENGTH_LONG).show();
                
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(intent);
            }
        });

        Button btnCheckAccessibility = findViewById(R.id.btnCheckAccessibility);
        btnCheckAccessibility.setOnClickListener(v -> {
            if (isAccessibilityEnabled()) {
                Toast.makeText(this, "✓ Accessibility فعال هست", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "✗ Accessibility فعال نیست", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(intent);
            }
        });
    }

    private boolean isAccessibilityEnabled() {
        int accessibilityEnabled = 0;
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                getContentResolver(),
                Settings.Secure.ACCESSIBILITY_ENABLED
            );
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }

        if (accessibilityEnabled == 1) {
            String services = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            );
            
            if (services != null) {
                return services.toLowerCase().contains(getPackageName().toLowerCase());
            }
        }
        return false;
    }

    private void openSmsAppToDelete() {
        new Thread(() -> {
            // پیدا کردن آخرین پیامک
            SmsInfo lastSms = getLastSms();
            
            if (lastSms == null) {
                runOnUiThread(() -> 
                    Toast.makeText(this, "پیامکی پیدا نشد", Toast.LENGTH_SHORT).show()
                );
                return;
            }

            Log.d(TAG, "آخرین پیامک: ID=" + lastSms.id + ", از: " + lastSms.address);

            runOnUiThread(() -> {
                try {
                    // باز کردن اپ پیامک روی thread مشخص
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("content://sms/" + lastSms.threadId));
                    intent.putExtra("sms_body", ""); // برای بعضی گوشی‌ها
                    startActivity(intent);
                    
                    Toast.makeText(this, 
                        "اپ پیامک باز شد. Accessibility خودکار حذف میکنه", 
                        Toast.LENGTH_LONG).show();
                    
                } catch (Exception e) {
                    Log.e(TAG, "خطا در باز کردن اپ پیامک: " + e.getMessage());
                    
                    // روش دوم: باز کردن اپ پیامک به صورت کلی
                    try {
                        Intent smsIntent = getPackageManager().getLaunchIntentForPackage("com.google.android.apps.messaging");
                        if (smsIntent == null) {
                            smsIntent = getPackageManager().getLaunchIntentForPackage("com.android.messaging");
                        }
                        if (smsIntent != null) {
                            startActivity(smsIntent);
                        }
                    } catch (Exception ex) {
                        Toast.makeText(this, "نتونستم اپ پیامک رو باز کنم", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }).start();
    }

    private SmsInfo getLastSms() {
        Uri uri = Uri.parse("content://sms/");
        String[] projection = new String[]{"_id", "thread_id", "address", "body"};
        
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(
                uri, projection, null, null, "date DESC LIMIT 1"
            );

            if (cursor != null && cursor.moveToFirst()) {
                SmsInfo info = new SmsInfo();
                info.id = cursor.getString(0);
                info.threadId = cursor.getString(1);
                info.address = cursor.getString(2);
                info.body = cursor.getString(3);
                return info;
            }
        } catch (Exception e) {
            Log.e(TAG, "خطا: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
        }
        return null;
    }

    private static class SmsInfo {
        String id;
        String threadId;
        String address;
        String body;
    }
}