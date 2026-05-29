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
import android.util.Log;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.aigarding.service.AutoClickService;
import com.example.aigarding.utils.AIAPI;
import com.example.aigarding.utils.ConfigManager;
import com.example.aigarding.utils.ScreenCapture;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE_SCREEN_CAPTURE = 1001;
    private static final int REQUEST_CODE_PERMISSIONS = 1002;

    private Button btnStart;
    private Button btnStop;
    private TextView tvStatus;
    private TextView tvScore;
    private TextView tvTotal;

    private MediaProjection mediaProjection;
    private ScreenCapture screenCapture;
    private AIAPI aiApi;
    private ConfigManager configManager;

    private boolean isRunning = false;
    private int currentScore = 0;
    private int totalPapers = 0;

    private int[] scoreButtonPositions = {
            100, 95, 90, 85, 80, 75, 70, 65, 60, 55, 50, 45, 40, 35, 30, 25, 20, 15, 10, 5, 0
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnStart = findViewById(R.id.btn_start);
        btnStop = findViewById(R.id.btn_stop);
        tvStatus = findViewById(R.id.tv_status);
        tvScore = findViewById(R.id.tv_score);
        tvTotal = findViewById(R.id.tv_total);

        configManager = new ConfigManager(this);
        aiApi = new AIAPI(configManager.getApiKey());

        btnStart.setOnClickListener(v -> startGrading());
        btnStop.setOnClickListener(v -> stopGrading());

        checkPermissions();
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] permissions = {
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            };

            List<String> missingPermissions = new java.util.ArrayList<>();
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(this, permission) 
                        != PackageManager.PERMISSION_GRANTED) {
                    missingPermissions.add(permission);
                }
            }

            if (!missingPermissions.isEmpty()) {
                ActivityCompat.requestPermissions(this, 
                        missingPermissions.toArray(new String[0]), 
                        REQUEST_CODE_PERMISSIONS);
            }
        }
    }

    private void startGrading() {
        if (!isAccessibilityEnabled()) {
            Toast.makeText(this, R.string.enable_accessibility, Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
            return;
        }

        if (!isRunning) {
            isRunning = true;
            updateUI();
            requestScreenCapture();
        }
    }

    private void stopGrading() {
        if (isRunning) {
            isRunning = false;
            updateUI();
            
            if (screenCapture != null) {
                screenCapture.stopCapture();
                screenCapture = null;
            }
            
            if (mediaProjection != null) {
                mediaProjection.stop();
                mediaProjection = null;
            }
        }
    }

    private void requestScreenCapture() {
        MediaProjectionManager manager = 
                (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        Intent intent = manager.createScreenCaptureIntent();
        startActivityForResult(intent, REQUEST_CODE_SCREEN_CAPTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_CODE_SCREEN_CAPTURE && resultCode == RESULT_OK) {
            mediaProjection = ((MediaProjectionManager) 
                    getSystemService(MEDIA_PROJECTION_SERVICE))
                    .getMediaProjection(resultCode, data);
            
            startScreenCapture();
        }
    }

    private void startScreenCapture() {
        Point screenSize = ScreenCapture.getScreenSize(this);
        
        screenCapture = new ScreenCapture();
        screenCapture.setOnCaptureListener(new ScreenCapture.OnCaptureListener() {
            @Override
            public void onCapture(Bitmap bitmap) {
                processImage(bitmap);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "截图失败: " + error);
                runOnUiThread(() -> {
                    tvStatus.setText("截图失败: " + error);
                });
            }
        });

        screenCapture.startCapture(this, mediaProjection, screenSize.x, screenSize.y);
        captureAndProcess();
    }

    private void captureAndProcess() {
        if (!isRunning) return;

        runOnUiThread(() -> tvStatus.setText(R.string.status_running));

        aiApi.sendImageForScore(captureAnswerArea(), new AIAPI.OnAIResponseListener() {
            @Override
            public void onSuccess(int score) {
                currentScore = score;
                totalPapers++;
                
                runOnUiThread(() -> {
                    tvScore.setText(getString(R.string.score, score));
                    tvTotal.setText(getString(R.string.total_papers, totalPapers));
                });

                clickScoreButton(score);

                scheduleNextCapture();
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "AI评分失败: " + error);
                runOnUiThread(() -> {
                    tvStatus.setText("AI评分失败: " + error);
                });
                
                scheduleNextCapture();
            }
        });
    }

    private Bitmap captureAnswerArea() {
        Point screenSize = ScreenCapture.getScreenSize(this);
        int top = configManager.getAreaTop();
        int left = configManager.getAreaLeft();
        int bottom = configManager.getAreaBottom();
        int right = configManager.getAreaRight();

        int width = right - left;
        int height = bottom - top;

        return Bitmap.createBitmap(
                screenSize.x > right ? right : screenSize.x,
                screenSize.y > bottom ? bottom : screenSize.y,
                Bitmap.Config.ARGB_8888
        );
    }

    private void clickScoreButton(int score) {
        AutoClickService service = AutoClickService.getInstance();
        if (service == null) return;

        int closestScore = findClosestScore(score);
        int buttonIndex = getButtonIndex(closestScore);
        
        int screenWidth = ScreenCapture.getScreenSize(this).x;
        int buttonWidth = screenWidth / scoreButtonPositions.length;
        int x = buttonIndex * buttonWidth + buttonWidth / 2;
        int y = 100;

        service.clickAtPosition(x, y);
    }

    private int findClosestScore(int target) {
        int closest = scoreButtonPositions[0];
        int minDiff = Math.abs(target - closest);
        
        for (int score : scoreButtonPositions) {
            int diff = Math.abs(target - score);
            if (diff < minDiff) {
                minDiff = diff;
                closest = score;
            }
        }
        return closest;
    }

    private int getButtonIndex(int score) {
        for (int i = 0; i < scoreButtonPositions.length; i++) {
            if (scoreButtonPositions[i] == score) {
                return i;
            }
        }
        return 0;
    }

    private void scheduleNextCapture() {
        if (!isRunning) return;

        new android.os.Handler().postDelayed(this::captureAndProcess, 3000);
    }

    private boolean isAccessibilityEnabled() {
        AccessibilityManager am = (AccessibilityManager) 
                getSystemService(ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> services = am.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
        
        for (AccessibilityServiceInfo service : services) {
            if (service.getId().contains(getPackageName())) {
                return true;
            }
        }
        return false;
    }

    private void updateUI() {
        runOnUiThread(() -> {
            btnStart.setEnabled(!isRunning);
            btnStop.setEnabled(isRunning);
            
            if (isRunning) {
                tvStatus.setText(R.string.status_running);
            } else {
                tvStatus.setText(R.string.status_idle);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "需要授予权限才能使用", Toast.LENGTH_SHORT).show();
                    finish();
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