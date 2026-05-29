package com.example.aigarding.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.os.Build;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

public class AutoClickService extends AccessibilityService {
    private static final String TAG = "AutoClickService";
    private static AutoClickService instance;

    public static AutoClickService getInstance() {
        return instance;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        Log.d(TAG, "无障碍服务已连接");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "无障碍服务被中断");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        Log.d(TAG, "无障碍服务已销毁");
    }

    public boolean clickAtPosition(int x, int y) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Path path = new Path();
                path.moveTo(x, y);
                path.lineTo(x + 1, y + 1);
                
                GestureDescription.StrokeDescription stroke =
                    new GestureDescription.StrokeDescription(path, 0, 100);
                
                GestureDescription.Builder builder = new GestureDescription.Builder();
                builder.addStroke(stroke);
                GestureDescription gesture = builder.build();
                
                return dispatchGesture(gesture, new AccessibilityService.GestureResultCallback() {
                    @Override
                    public void onCompleted(GestureDescription gestureDescription) {
                        super.onCompleted(gestureDescription);
                        Log.d(TAG, "点击完成");
                    }
                    
                    @Override
                    public void onCancelled(GestureDescription gestureDescription) {
                        super.onCancelled(gestureDescription);
                        Log.d(TAG, "点击取消");
                    }
                }, null);
            } else {
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "点击失败", e);
            return false;
        }
    }
}
