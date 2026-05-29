package com.example.aigarding;

import android.Manifest;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
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
    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE_SCREEN_CAPTURE = 1001;
    private static final int REQUEST_CODE_PERMISSIONS = 1002;

    private ConfigManager configManager;
    private ScreenCapture screenCapture;
    private MediaProjection mediaProjection;
    private boolean isRunning = false;
    private int totalGraded = 0;

    private EditText etApiKey, etModelId, etReferenceAnswer;
    private EditText etAreaLeft, etAreaTop, etAreaRight, etAreaBottom;
    private LinearLayout llScoreButtons;
    private TextView tvStatus, tvCurrentScore, tvTotalGraded;
    private Button btnStart, btnStop, btnSaveConfig, btnAddScoreButton, btnTestClick;

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

        etAreaLeft = findViewById(R.id.et_area_left);
        etAreaTop = findViewById(R.id.et_area_top);
        etAreaRight = findViewById(R.id.et_area_right);
        etAreaBottom = findViewById(R.id.et_area_bottom);

        llScoreButtons = findViewById(R.id.ll_score_buttons);
        tvStatus = findViewById(R.id.tv_status);
        tvCurrentScore = findViewById(R.id.tv_current_score);
        tvTotalGraded = findViewById(R.id.tv_total_graded);

        btnStart = findViewById(R.id.btn_start);
        btnStop = findViewById(R.id.btn_stop);
        btnSaveConfig = findViewById(R.id.btn_save_config);
        btnAddScoreButton = findViewById(R.id.btn_add_score_button);
        btnTestClick = findViewById(R.id.btn_test_click);

        btnSaveConfig.setOnClickListener(v -> saveConfig());
        btnAddScoreButton.setOnClickListener(v -> showAddScoreButtonDialog());
        btnTestClick.setOnClickListener(v -> testClick());
        btnStart.setOnClickListener(v -> startGrading());
        btnStop.setOnClickListener(v -> stopGrading());
    }

    private void loadConfig() {
        etApiKey.setText(configManager.getApiKey());
        etModelId.setText(configManager.getModelId());
        etReferenceAnswer.setText(configManager.getReferenceAnswer());

        etAreaLeft.setText(String.valueOf(configManager.getAreaLeft()));
        etAreaTop.setText(String.valueOf(configManager.getAreaTop()));
        etAreaRight.setText(String.valueOf(configManager.getAreaRight()));
        etAreaBottom.setText(String.valueOf(configManager.getAreaBottom()));

        updateScoreButtonsList();
    }

    private void saveConfig() {
        configManager.setApiKey(etApiKey.getText().toString());
        configManager.setModelId(etModelId.getText().toString());
        configManager.setReferenceAnswer(etReferenceAnswer.getText().toString());

        try {
            int left = Integer.parseInt(etAreaLeft.getText().toString());
            int top = Integer.parseInt(etAreaTop.getText().toString());
            int right = Integer.parseInt(etAreaRight.getText().toString());
            int bottom = Integer.parseInt(etAreaBottom.getText().toString());
            configManager.setAnswerArea(left, top, right, bottom);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "请输入有效的数字坐标", Toast.LENGTH_SHORT).show();
        }

        Toast.makeText(this, "配置已保存", Toast.LENGTH_SHORT).show();
    }

    private void updateScoreButtonsList() {
        llScoreButtons.removeAllViews();
        List<ConfigManager.ScoreButton> buttons = configManager.getScoreButtons();
        
        for (int i = 0; i < buttons.size(); i++) {
            ConfigManager.ScoreButton btn = buttons.get(i);
            View itemView = getLayoutInflater().inflate(R.layout.item_score_button, null);
            
            TextView tvInfo = itemView.findViewById(R.id.tv_score_button_info);
            Button btnDelete = itemView.findViewById(R.id.btn_delete);
            
            tvInfo.setText(String.format("分值: %d, 坐标: (%d, %d)", btn.score, btn.x, btn.y));
            final int index = i;
            btnDelete.setOnClickListener(v -> deleteScoreButton(index));
            
            llScoreButtons.addView(itemView);
        }
    }

    private void showAddScoreButtonDialog() {
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
                    Toast.makeText(this, "请输入有效的数字", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void deleteScoreButton(int index) {
        List<ConfigManager.ScoreButton> buttons = configManager.getScoreButtons();
        buttons.remove(index);
        configManager.setScoreButtons(buttons);
        updateScoreButtonsList();
    }

    private void testClick() {
        if (!isAccessibilityEnabled()) {
            Toast.makeText(this, "请先开启无障碍服务", Toast.LENGTH_SHORT).show();
            return;
        }

        List<ConfigManager.ScoreButton> buttons = configManager.getScoreButtons();
        if (buttons.isEmpty()) {
            Toast.makeText(this, "请先添加分值按钮", Toast.LENGTH_SHORT).show();
            return;
        }

        AutoClickService service = AutoClickService.getInstance();
        if (service != null) {
            ConfigManager.ScoreButton btn = buttons.get(0);
            service.clickAtPosition(btn.x, btn.y);
            Toast.makeText(this, "已尝试点击第一个分值按钮", Toast.LENGTH_SHORT).show();
        }
    }

    private void startGrading() {
        if (!isAccessibilityEnabled()) {
            Toast.makeText(this, "请先开启无障碍服务", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
            return;
        }

        if (TextUtils.isEmpty(etApiKey.getText()) || TextUtils.isEmpty(etModelId.getText())) {
            Toast.makeText(this, "请配置API Key和模型ID", Toast.LENGTH_SHORT).show();
            return;
        }

        if (configManager.getScoreButtons().isEmpty()) {
            Toast.makeText(this, "请添加至少一个分值按钮", Toast.LENGTH_SHORT).show();
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
                tvStatus.setText("正在AI评分...");
                
                BailianAPI api = new BailianAPI(configManager.getApiKey(), configManager.getModelId());
                api.scoreAnswer(bitmap, configManager.getReferenceAnswer(), new BailianAPI.ScoringCallback() {
                    @Override
                    public void onSuccess(int score) {
                        runOnUiThread(() -> {
                            tvCurrentScore.setText("当前评分: " + score);
                            tvStatus.setText("正在点击分值按钮...");
                        });
                        
                        ConfigManager.ScoreButton btn = configManager.findClosestScoreButton(score);
                        if (btn != null) {
                            AutoClickService service = AutoClickService.getInstance();
                            if (service != null) {
                                service.clickAtPosition(btn.x, btn.y);
                            }
                        }
                        
                        totalGraded++;
                        runOnUiThread(() -> {
                            tvTotalGraded.setText("已批阅: " + totalGraded);
                            tvStatus.setText("等待下一份答卷...");
                        });
                        
                        new android.os.Handler().postDelayed(() -> startGradingLoop(), 3000);
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            tvStatus.setText("错误: " + error);
                        });
                        new android.os.Handler().postDelayed(() -> startGradingLoop(), 3000);
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    tvStatus.setText("截图错误: " + error);
                });
                new android.os.Handler().postDelayed(() -> startGradingLoop(), 3000);
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
        
        if (isRunning) {
            tvStatus.setText("阅卷中...");
        } else {
            tvStatus.setText("等待开始");
        }
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            List<String> permissions = new ArrayList<>();
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
            
            if (!permissions.isEmpty()) {
                ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), REQUEST_CODE_PERMISSIONS);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "需要授予存储权限", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        stopGrading();
        super.onDestroy();
    }
}
