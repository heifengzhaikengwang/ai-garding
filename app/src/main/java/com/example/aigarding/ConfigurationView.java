package com.example.aigarding;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

import com.example.aigarding.utils.ConfigManager;

@SuppressLint("ViewConstructor")
public class ConfigurationView extends LinearLayout {
    private ConfigManager configManager;
    
    private boolean isDraggingRect = false;
    private boolean isResizingRect = false;
    private float lastTouchX = 0f;
    private float lastTouchY = 0f;
    
    private Rect selectionRect = new Rect(100, 100, 500, 400);
    private List&lt;ConfigManager.ScoreButton&gt; scoreButtons = new ArrayList&lt;&gt;();
    private ConfigManager.ScoreButton tempButton = null;
    private boolean isAddingButton = false;
    
    private Paint rectPaint;
    private Paint handlePaint;
    private Paint scorePaint;
    private Paint textPaint;
    
    private static final int HANDLE_SIZE = 40;
    private static final int HANDLE_RADIUS = 20;

    public ConfigurationView(Context context) {
        super(context);
        configManager = new ConfigManager(context);
        
        initPaints();
        loadSavedData();
        setWillNotDraw(false);
        setBackgroundColor(Color.TRANSPARENT);
    }

    private void initPaints() {
        rectPaint = new Paint();
        rectPaint.setColor(Color.RED);
        rectPaint.setStyle(Paint.Style.STROKE);
        rectPaint.setStrokeWidth(4);
        
        handlePaint = new Paint();
        handlePaint.setColor(Color.RED);
        handlePaint.setStyle(Paint.Style.FILL);
        
        scorePaint = new Paint();
        scorePaint.setColor(Color.BLUE);
        scorePaint.setStyle(Paint.Style.FILL);
        
        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(32);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    private void loadSavedData() {
        int left = configManager.getAreaLeft();
        int top = configManager.getAreaTop();
        int right = configManager.getAreaRight();
        int bottom = configManager.getAreaBottom();
        
        if (right &gt; left &amp;&amp; bottom &gt; top) {
            selectionRect.set(left, top, right, bottom);
        }
        
        scoreButtons.addAll(configManager.getScoreButtons());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getRawX();
        float y = event.getRawY();
        
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = x;
                lastTouchY = y;
                
                if (isAddingButton) {
                    // 正在添加按钮模式，点击屏幕任意位置记录
                    tempButton = new ConfigManager.ScoreButton(0, (int)x, (int)y);
                    invalidate();
                    return true;
                }
                
                // 检查是否点击了调整手柄
                if (isOnResizeHandle(x, y)) {
                    isResizingRect = true;
                    return true;
                }
                
                // 检查是否点击了选择框内部
                if (selectionRect.contains((int)x, (int)y)) {
                    isDraggingRect = true;
                    return true;
                }
                break;
                
            case MotionEvent.ACTION_MOVE:
                float dx = x - lastTouchX;
                float dy = y - lastTouchY;
                
                if (isDraggingRect) {
                    selectionRect.offset((int)dx, (int)dy);
                    invalidate();
                } else if (isResizingRect) {
                    selectionRect.right = Math.max(selectionRect.left + 50, (int)x);
                    selectionRect.bottom = Math.max(selectionRect.top + 50, (int)y);
                    invalidate();
                }
                
                lastTouchX = x;
                lastTouchY = y;
                break;
                
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDraggingRect = false;
                isResizingRect = false;
                break;
        }
        
        return super.onTouchEvent(event);
    }

    private boolean isOnResizeHandle(float x, float y) {
        return x &gt;= selectionRect.right - HANDLE_SIZE &amp;&amp;
               x &lt;= selectionRect.right + HANDLE_SIZE &amp;&amp;
               y &gt;= selectionRect.bottom - HANDLE_SIZE &amp;&amp;
               y &lt;= selectionRect.bottom + HANDLE_SIZE;
    }

    public void startAddingButton(int score) {
        isAddingButton = true;
        tempButton = new ConfigManager.ScoreButton(score, 0, 0);
        Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            if (Build.VERSION.SDK_INT &gt;= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(100);
            }
        }
        invalidate();
    }

    public void confirmAddingButton(int score) {
        if (tempButton != null) {
            tempButton.score = score;
            scoreButtons.add(tempButton);
            tempButton = null;
            isAddingButton = false;
            invalidate();
        }
    }

    public void cancelAddingButton() {
        tempButton = null;
        isAddingButton = false;
        invalidate();
    }

    public void clearScoreButtons() {
        scoreButtons.clear();
        invalidate();
    }

    public void saveConfig() {
        configManager.setAnswerArea(
            selectionRect.left, 
            selectionRect.top, 
            selectionRect.right, 
            selectionRect.bottom
        );
        configManager.setScoreButtons(scoreButtons);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        // 绘制选择框
        canvas.drawRect(selectionRect, rectPaint);
        
        // 绘制调整手柄
        canvas.drawCircle(
            selectionRect.right,
            selectionRect.bottom,
            HANDLE_RADIUS,
            handlePaint
        );
        
        // 绘制已保存的按钮
        for (ConfigManager.ScoreButton button : scoreButtons) {
            canvas.drawCircle(button.x, button.y, 25, scorePaint);
            canvas.drawText(
                String.valueOf(button.score),
                button.x,
                button.y + 8,
                textPaint
            );
        }
        
        // 绘制临时按钮（正在添加模式）
        if (tempButton != null &amp;&amp; isAddingButton) {
            canvas.drawCircle(tempButton.x, tempButton.y, 25, scorePaint);
            canvas.drawText(
                "?",
                tempButton.x,
                tempButton.y + 8,
                textPaint
            );
        }
    }
}
