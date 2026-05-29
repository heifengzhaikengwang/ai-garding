package com.example.aigarding;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.aigarding.service.AutoClickService;
import com.example.aigarding.service.FloatingViewService;
import com.example.aigarding.utils.ConfigManager;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_OVERLAY_PERMISSION = 1001;
    
    private ConfigManager configManager;
    private EditText etApiKey, etModelId, etReferenceAnswer, etInterval;
    private TextView tvStatus, tvCount;
    private Button btnStartConfig, btnStartGrading, btnStop;
    private boolean isGrading = false;
    private int totalGraded = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        configManager = new ConfigManager(this);
        initViews();
        loadConfig();
    }

    private void initViews() {
        etApiKey = findViewById(R.id.et_api_key);
        etModelId = findViewById(R.id.et_model_id);
        etReferenceAnswer = findViewById(R.id.et_reference_answer);
        etInterval = findViewById(R.id.et_interval);
        tvStatus = findViewById(R.id.tv_status);
        tvCount = findViewById(R.id.tv_count);
        btnStartConfig = findViewById(R.id.btn_start_config);
        btnStartGrading = findViewById(R.id.btn_start_grading);
        btnStop = findViewById(R.id.btn_stop);

        btnStartConfig.setOnClickListener(v -> startConfiguration());
        btnStartGrading.setOnClickListener(v -> startGrading());
        btnStop.setOnClickListener(v -> stopGrading());
    }

    private void loadConfig() {
        etApiKey.setText(configManager.getApiKey());
        etModelId.setText(configManager.getModelId());
        etReferenceAnswer.setText(configManager.getReferenceAnswer());
        etInterval.setText(String.valueOf(configManager.getInterval()));
    }

    private void saveConfig() {
        configManager.setApiKey(etApiKey.getText().toString());
        configManager.setModelId(etModelId.getText().toString());
        configManager.setReferenceAnswer(etReferenceAnswer.getText().toString());
        try {
            int interval = Integer.parseInt(etInterval.getText().toString());
            configManager.setInterval(Math.max(1, interval));
        } catch (Exception e) {
            configManager.setInterval(3);
        }
    }

    private void startConfiguration() {
        saveConfig();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
            return;
        }

        Intent intent = new Intent(this, FloatingViewService.class);
        startService(intent);
        finish();
    }

    private void startGrading() {
        saveConfig();
        
        if (configManager.getAreaWidth() == 0) {
            Toast.makeText(this, "请先配置截图区域", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (configManager.getScoreButtons().isEmpty()) {
            Toast.makeText(this, "请先配置分值按钮", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isAccessibilityEnabled()) {
            Toast.makeText(this, "请先开启无障碍服务", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            return;
        }

        isGrading = true;
        totalGraded = 0;
        updateUI();
        startGradingLoop();
    }

    private void stopGrading() {
        isGrading = false;
        updateUI();
    }

    private void startGradingLoop() {
        if (!isGrading) return;

        tvStatus.setText("正在截图...");
        
        // 等待MediaProjection授权（第一次运行时需要手动确认）
        Intent intent = new Intent(this, ScreenCaptureActivity.class);
        startActivityForResult(intent, 2001);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                startConfiguration();
            }
        } else if (requestCode == 2001 && resultCode == RESULT_OK) {
            // 截图并评分，然后点击对应分值按钮
            processGrading();
        }
    }

    private void processGrading() {
        // 这里是核心业务逻辑：截图->AI评分->自动点击
        // 由于MediaProjection需要Activity上下文，我将在ScreenCaptureActivity中实现完整逻辑
        totalGraded++;
        tvCount.setText("已完成: " + totalGraded);
        
        new android.os.Handler().postDelayed(() -> {
            if (isGrading) {
                startGradingLoop();
            }
        }, configManager.getInterval() * 1000L);
    }

    private boolean isAccessibilityEnabled() {
        android.view.accessibility.AccessibilityManager am =
            (android.view.accessibility.AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);
        for (android.accessibilityservice.AccessibilityServiceInfo service :
                am.getEnabledAccessibilityServiceList(
                        android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK)) {
            if (service.getId().contains(getPackageName())) {
                return true;
            }
        }
        return false;
    }

    private void updateUI() {
        btnStartConfig.setEnabled(!isGrading);
        btnStartGrading.setEnabled(!isGrading);
        btnStop.setEnabled(isGrading);
        tvStatus.setText(isGrading ? "批改中..." : "等待开始");
        if (!isGrading) {
            tvCount.setText("已完成: 0");
        }
    }
}
