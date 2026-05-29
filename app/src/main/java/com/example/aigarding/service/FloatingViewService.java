package com.example.aigarding.service;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.example.aigarding.ConfigurationView;
import com.example.aigarding.R;
import com.example.aigarding.utils.ConfigManager;

public class FloatingViewService extends Service {
    private WindowManager windowManager;
    private ConfigurationView configurationView;
    private View controlPanel;
    private boolean isShowing = false;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        showFloatingView();
    }

    private void showFloatingView() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        
        // 创建配置视图
        configurationView = new ConfigurationView(this);
        
        // 创建控制面板
        controlPanel = LayoutInflater.from(this).inflate(R.layout.floating_control_panel, null);
        
        // 设置控制面板按钮
        setupControlPanel();
        
        // 添加到窗口
        WindowManager.LayoutParams viewParams = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            Build.VERSION.SDK_INT &gt;= Build.VERSION_CODES.O ? 
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : 
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );
        viewParams.gravity = Gravity.TOP | Gravity.LEFT;
        
        WindowManager.LayoutParams panelParams = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            Build.VERSION.SDK_INT &gt;= Build.VERSION_CODES.O ? 
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : 
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | 
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        );
        panelParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        panelParams.y = 100;
        
        windowManager.addView(configurationView, viewParams);
        windowManager.addView(controlPanel, panelParams);
        isShowing = true;
    }

    private void setupControlPanel() {
        Button btnAddButton = controlPanel.findViewById(R.id.btn_add_button);
        Button btnClearButtons = controlPanel.findViewById(R.id.btn_clear_buttons);
        Button btnSave = controlPanel.findViewById(R.id.btn_save);
        Button btnCancel = controlPanel.findViewById(R.id.btn_cancel);
        
        btnAddButton.setOnClickListener(v -&gt; showAddButtonDialog());
        btnClearButtons.setOnClickListener(v -&gt; configurationView.clearScoreButtons());
        btnSave.setOnClickListener(v -&gt; saveAndExit());
        btnCancel.setOnClickListener(v -&gt; exitService());
    }

    private void showAddButtonDialog() {
        // 简单的方式：直接让用户在屏幕上点击，弹出对话框输入分数
        final android.app.AlertDialog.Builder builder = 
            new android.app.AlertDialog.Builder(this);
        builder.setTitle("添加分值按钮");
        
        final EditText input = new EditText(this);
        input.setHint("输入分值");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);
        
        builder.setPositiveButton("下一步", (dialog, which) -&gt; {
            int score = 0;
            try {
                score = Integer.parseInt(input.getText().toString());
            } catch (NumberFormatException e) {
                score = 0;
            }
            final int finalScore = score;
            
            // 提示用户点击屏幕上的按钮位置
            android.app.AlertDialog locationDialog = 
                new android.app.AlertDialog.Builder(FloatingViewService.this)
                    .setTitle("点击屏幕上的分值按钮")
                    .setMessage("点击屏幕上对应分值的按钮位置")
                    .setPositiveButton("确认", (d, w) -&gt; {
                        configurationView.confirmAddingButton(finalScore);
                    })
                    .setNegativeButton("取消", (d, w) -&gt; {
                        configurationView.cancelAddingButton();
                    })
                    .create();
            
            // 添加按钮让用户确认位置
            locationDialog.setOnShowListener(dialog1 -&gt; {
                configurationView.startAddingButton(finalScore);
            });
            
            locationDialog.show();
        });
        
        builder.setNegativeButton("取消", (dialog, which) -&gt; dialog.cancel());
        builder.show();
    }

    private void saveAndExit() {
        configurationView.saveConfig();
        exitService();
    }

    private void exitService() {
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isShowing) {
            if (configurationView != null) {
                windowManager.removeView(configurationView);
            }
            if (controlPanel != null) {
                windowManager.removeView(controlPanel);
            }
            isShowing = false;
        }
    }
}
