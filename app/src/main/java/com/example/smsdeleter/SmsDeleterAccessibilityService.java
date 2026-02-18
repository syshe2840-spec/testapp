package com.example.smsdeleter;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.List;

public class SmsDeleterAccessibilityService extends AccessibilityService {

    private static final String TAG = "SMS_ACCESSIBILITY";
    private boolean isDeleting = false;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || isDeleting) return;

        String packageName = event.getPackageName() != null ? 
            event.getPackageName().toString() : "";

        // چک میکنیم که توی اپ پیامک هستیم
        if (packageName.contains("messaging") || 
            packageName.contains("mms") || 
            packageName.contains("sms")) {
            
            Log.d(TAG, "توی اپ پیامک هستیم: " + packageName);
            
            // سعی میکنیم دکمه‌های حذف رو پیدا کنیم
            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            if (rootNode != null) {
                findAndClickDelete(rootNode);
                rootNode.recycle();
            }
        }
    }

    private void findAndClickDelete(AccessibilityNodeInfo node) {
        if (node == null) return;

        // کلمات کلیدی برای حذف
        String[] deleteKeywords = {
            "delete", "حذف", "remove", "trash", "پاک کردن",
            "Delete", "حذف کن", "Remove", "Trash"
        };

        // چک کردن متن این node
        CharSequence text = node.getText();
        CharSequence contentDesc = node.getContentDescription();
        
        String nodeText = text != null ? text.toString().toLowerCase() : "";
        String nodeDesc = contentDesc != null ? contentDesc.toString().toLowerCase() : "";

        for (String keyword : deleteKeywords) {
            if (nodeText.contains(keyword.toLowerCase()) || 
                nodeDesc.contains(keyword.toLowerCase())) {
                
                if (node.isClickable()) {
                    Log.d(TAG, "✓ دکمه حذف پیدا شد: " + nodeText + " / " + nodeDesc);
                    isDeleting = true;
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    
                    // بعد از 2 ثانیه دوباره فعال میشه
                    new android.os.Handler().postDelayed(() -> {
                        isDeleting = false;
                    }, 2000);
                    
                    return;
                }
            }
        }

        // چک کردن فرزندان
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                findAndClickDelete(child);
                child.recycle();
            }
        }
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "Accessibility Service قطع شد");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.d(TAG, "✓ Accessibility Service متصل شد");

        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED | 
                         AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.notificationTimeout = 100;
        
        setServiceInfo(info);
    }
}