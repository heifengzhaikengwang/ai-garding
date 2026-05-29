package com.example.aigarding.service;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.RequiresApi;

public class AutoClickService extends AccessibilityService {

    private static final String TAG = "AutoClickService";
    private static AutoClickService instance;
    private OnServiceConnectedListener listener;

    public interface OnServiceConnectedListener {
        void onServiceConnected();
    }

    public static AutoClickService getInstance() {
        return instance;
    }

    public void setOnServiceConnectedListener(OnServiceConnectedListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        Log.d(TAG, "无障碍服务已连接");
        if (listener != null) {
            listener.onServiceConnected();
        }
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Path path = new Path();
            path.moveTo(x, y);
            return performGesture(path, 100, 100, null);
        } else {
            return false;
        }
    }

    public boolean clickButtonByText(String text) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            return false;
        }
        return findAndClick(root, text);
    }

    private boolean findAndClick(AccessibilityNodeInfo node, String text) {
        if (node == null) return false;

        if (text.equals(node.getText())) {
            return performClick(node);
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            if (findAndClick(node.getChild(i), text)) {
                return true;
            }
        }
        return false;
    }

    private boolean performClick(AccessibilityNodeInfo node) {
        if (node == null) return false;

        if (node.isClickable()) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        } else {
            Rect rect = new Rect();
            node.getBoundsInScreen(rect);
            int x = rect.centerX();
            int y = rect.centerY();
            return clickAtPosition(x, y);
        }
    }

    public void simulateLongClick(int x, int y) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Path path = new Path();
            path.moveTo(x, y);
            performGesture(path, 1000, 100, null);
        }
    }
}