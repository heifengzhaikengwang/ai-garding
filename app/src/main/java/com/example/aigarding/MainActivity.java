package com.example.aigarding;

import android.Manifest;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.aigarding.service.AutoClickService;
import com.example.aigarding.utils.BailianAPI;
import com.example.aigarding.utils.ConfigManager;
import com.example.aigarding.utils.ScreenCapture;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_SCREEN_CAPTURE = 1001;
    private static final int REQUEST_CODE_PERMISSIONS = 1002;

    private ConfigManager configManager;
    private ScreenCapture screenCapture;
    private MediaProjection mediaProjection;
    private boolean isRunning = false;
    private int totalGraded = 0;

    private EditText etApiKey, etModelId, etReferenceAnswer, etArea, etInterval;
    private LinearLayout llScoreButtons;
    private TextView tvStatus, tvCount;
    private Button btnStart, btnStop, btnAddScore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        configManager = new ConfigManager(this);
        initViews();
        loadConfig();
        checkPermissions();
    }

    private void initViews() {
        etApiKey = findViewById(R.id.et_api_key);
        etModelId = findViewById(R.id.et_model_id);
        etReferenceAnswer = findViewById(R.id.et_reference_answer);
        etArea = findViewById(R.id.et_area);
        etInterval = findViewById(R.id.et_interval);
        llScoreButtons = findViewById(R.id.ll_score_buttons);
        tvStatus = findViewById(R.id.tv_status);
        tvCount = findViewById(R.id.tv_count);
        btnStart = findViewById(R.id.btn_start);
        btnStop = findViewById(R.id.btn_stop);
        btnAddScore = findViewById(R.id.btn_add_score);

        btnAddScore.setOnClickListener(v -> showAddScoreDialog());
        btnStart.setOnClickListener(v -> startGrading());
        btnStop.setOnClickListener(v -> stopGrading());
    }

    private void loadConfig() {
        etApiKey.setText(configManager.getApiKey());
        etModelId.setText(configManager.getModelId());
        etReferenceAnswer.setText(configManager.getReferenceAnswer());
        etArea.setText(configManager.getAreaLeft() + "," + configManager.getAreaTop() + "," + 
                       configManager.getAreaRight() + "," + configManager.getAreaBottom());
        etInterval.setText(String.valueOf(configManager.getInterval()));
        updateScoreButtonsList();
    }

    private void saveConfig() {
        configManager.setApiKey(etApiKey.getText().toString());
        configManager.setModelId(etModelId.getText().toString());
        configManager.setReferenceAnswer(etReferenceAnswer.getText().toString());
        
        try {
            String[] parts = etArea.getText().toString().split(",");
            if (parts.length == 4) {
                configManager.setAnswerArea(
                    Integer.parseInt(parts[0].trim()),
                    Integer.parseInt(parts[1].trim()),
                    Integer.parseInt(parts[2].trim()),
                    Integer.parseInt(parts[3].trim())
                );
            }
        } catch (Exception e) {
            Toast.makeText(this, "截图区域格式错误", Toast.LENGTH_SHORT).show();
        }
        
        try {
            String intervalStr = etInterval.getText().toString();
            if (!TextUtils.isEmpty(intervalStr)) {
                int interval = Integer.parseInt(intervalStr);
                configManager.setInterval(Math.max(1, interval));
            }
        } catch (Exception e) {
            Toast.makeText(this, "间隔秒数格式错误", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateScoreButtonsList() {
        llScoreButtons.removeAllViews();
        List<ConfigManager.ScoreButton> buttons = configManager.getScoreButtons();
        
        for (int i = 0; i < buttons.size(); i++) {
            ConfigManager.ScoreButton btn = buttons.get(i);
            TextView tv = new TextView(this);
            tv.setText(String.format("分值 %d: (%d, %d) [删除]", btn.score, btn.x, btn.y));
            tv.setPadding(8, 8, 8, 8);
            final int index = i;
            tv.setOnClickListener(v -> {
                buttons.remove(index);
                configManager.setScoreButtons(buttons);
                updateScoreButtonsList();
            });
            llScoreButtons.addView(tv);
        }
    }

    private void showAddScoreDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_score_button, null);
        EditText etScore = dialogView.findViewById(R.id.et_score);
        EditText etX = dialogView.findViewById(R.id.et_x);
        EditText etY = dialogView.findViewById(R.id.et_y);

        new AlertDialog.Builder(this)
            .setTitle("添加分值按钮")
            .setView(dialogView)
            .setPositiveButton("添加", (dialog, which) -> {
                try {
                    int score = Integer.parseInt(etScore.getText().toString());
                    int x = Integer.parseInt(etX.getText().toString());
                    int y = Integer.parseInt(etY.getText().toString());
                    
                    List<ConfigManager.ScoreButton> buttons = configManager.getScoreButtons();
                    buttons.add(new ConfigManager.ScoreButton(score, x, y));
                    configManager.setScoreButtons(buttons);
                    updateScoreButtonsList();
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "请输入有效数字", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void startGrading() {
        if (!isAccessibilityEnabled()) {
            Toast.makeText(this, "请先开启无障碍服务", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            return;
        }

        if (TextUtils.isEmpty(etApiKey.getText()) || TextUtils.isEmpty(etModelId.getText())) {
            Toast.makeText(this, "请配置API Key和模型ID", Toast.LENGTH_SHORT).show();
            return;
        }

        if (configManager.getScoreButtons().isEmpty()) {
            Toast.makeText(this, "请添加分值按钮", Toast.LENGTH_SHORT).show();
            return;
        }

        saveConfig();
        isRunning = true;
        totalGraded = 0;
        updateUI();
        
        MediaProjectionManager mgr = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        startActivityForResult(mgr.createScreenCaptureIntent(), REQUEST_CODE_SCREEN_CAPTURE);
    }

    private void stopGrading() {
        isRunning = false;
        if (screenCapture != null) {
            screenCapture.stopCapture();
            screenCapture = null;
        }
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }
        updateUI();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SCREEN_CAPTURE && resultCode == RESULT_OK) {
            MediaProjectionManager mgr = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
            mediaProjection = mgr.getMediaProjection(resultCode, data);
            
            screenCapture = new ScreenCapture();
            screenCapture.startCapture(this, mediaProjection);
            
            startGradingLoop();
        }
    }

    private void startGradingLoop() {
        if (!isRunning) return;
        
        tvStatus.setText("正在截图...");
        
        int left = configManager.getAreaLeft();
        int top = configManager.getAreaTop();
        int right = configManager.getAreaRight();
        int bottom = configManager.getAreaBottom();
        
        screenCapture.captureCrop(left, top, right, bottom, new ScreenCapture.ScreenshotCallback() {
            @Override
            public void onScreenshot(Bitmap bitmap) {
                tvStatus.setText("正在评分...");
                
                BailianAPI api = new BailianAPI(configManager.getApiKey(), configManager.getModelId());
                api.scoreAnswer(bitmap, configManager.getReferenceAnswer(), new BailianAPI.ScoringCallback() {
                    @Override
                    public void onSuccess(int score) {
                        runOnUiThread(() -> tvStatus.setText("评分: " + score));
                        
                        ConfigManager.ScoreButton btn = configManager.findClosestScoreButton(score);
                        if (btn != null) {
                            AutoClickService service = AutoClickService.getInstance();
                            if (service != null) {
                                service.clickAtPosition(btn.x, btn.y);
                            }
                        }
                        
                        totalGraded++;
                        runOnUiThread(() -> {
                            tvCount.setText("已完成: " + totalGraded);
                            tvStatus.setText("等待 " + configManager.getInterval() + " 秒后继续...");
                        });
                        
                        new android.os.Handler().postDelayed(() -> startGradingLoop(), configManager.getInterval() * 1000L);
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> tvStatus.setText("错误: " + error));
                        new android.os.Handler().postDelayed(() -> startGradingLoop(), configManager.getInterval() * 1000L);
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> tvStatus.setText("截图错误: " + error));
                new android.os.Handler().postDelayed(() -> startGradingLoop(), configManager.getInterval() * 1000L);
            }
        });
    }

    private boolean isAccessibilityEnabled() {
        AccessibilityManager am = (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> services = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
        
        for (AccessibilityServiceInfo service : services) {
            if (service.getId().contains(getPackageName())) {
                return true;
            }
        }
        return false;
    }

    private void updateUI() {
        btnStart.setEnabled(!isRunning);
        btnStop.setEnabled(isRunning);
        tvStatus.setText(isRunning ? "批改中..." : "等待开始");
        if (!isRunning) {
            tvCount.setText("已完成: 0");
        }
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            List<String> permissions = new ArrayList<>();
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            if (!permissions.isEmpty()) {
                ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), REQUEST_CODE_PERMISSIONS);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onDestroy() {
        stopGrading();
        super.onDestroy();
    }
}
