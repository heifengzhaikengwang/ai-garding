package com.example.aigarding.utils;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;

public class ConfigManager {
    private static final String PREF_NAME = "AI_GARDING_CONFIG";
    
    private static final String KEY_API_KEY = "api_key";
    private static final String KEY_MODEL_ID = "model_id";
    private static final String KEY_REFERENCE_ANSWER = "reference_answer";
    
    // 答案区域坐标
    private static final String KEY_AREA_TOP = "area_top";
    private static final String KEY_AREA_LEFT = "area_left";
    private static final String KEY_AREA_BOTTOM = "area_bottom";
    private static final String KEY_AREA_RIGHT = "area_right";
    
    // 分值按钮配置
    private static final String KEY_SCORE_BUTTONS_COUNT = "score_buttons_count";
    private static final String KEY_SCORE_BUTTON_PREFIX = "score_button_";

    private SharedPreferences preferences;

    public ConfigManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public String getApiKey() {
        return preferences.getString(KEY_API_KEY, "");
    }

    public void setApiKey(String apiKey) {
        preferences.edit().putString(KEY_API_KEY, apiKey).apply();
    }

    public String getModelId() {
        return preferences.getString(KEY_MODEL_ID, "");
    }

    public void setModelId(String modelId) {
        preferences.edit().putString(KEY_MODEL_ID, modelId).apply();
    }

    public String getReferenceAnswer() {
        return preferences.getString(KEY_REFERENCE_ANSWER, "");
    }

    public void setReferenceAnswer(String answer) {
        preferences.edit().putString(KEY_REFERENCE_ANSWER, answer).apply();
    }

    public int getAreaTop() {
        return preferences.getInt(KEY_AREA_TOP, 200);
    }

    public void setAreaTop(int top) {
        preferences.edit().putInt(KEY_AREA_TOP, top).apply();
    }

    public int getAreaLeft() {
        return preferences.getInt(KEY_AREA_LEFT, 100);
    }

    public void setAreaLeft(int left) {
        preferences.edit().putInt(KEY_AREA_LEFT, left).apply();
    }

    public int getAreaBottom() {
        return preferences.getInt(KEY_AREA_BOTTOM, 800);
    }

    public void setAreaBottom(int bottom) {
        preferences.edit().putInt(KEY_AREA_BOTTOM, bottom).apply();
    }

    public int getAreaRight() {
        return preferences.getInt(KEY_AREA_RIGHT, 700);
    }

    public void setAreaRight(int right) {
        preferences.edit().putInt(KEY_AREA_RIGHT, right).apply();
    }

    public void setAnswerArea(int left, int top, int right, int bottom) {
        setAreaLeft(left);
        setAreaTop(top);
        setAreaRight(right);
        setAreaBottom(bottom);
    }

    public List<ScoreButton> getScoreButtons() {
        List<ScoreButton> buttons = new ArrayList<>();
        int count = preferences.getInt(KEY_SCORE_BUTTONS_COUNT, 0);
        for (int i = 0; i < count; i++) {
            int score = preferences.getInt(KEY_SCORE_BUTTON_PREFIX + i + "_score", 0);
            int x = preferences.getInt(KEY_SCORE_BUTTON_PREFIX + i + "_x", 0);
            int y = preferences.getInt(KEY_SCORE_BUTTON_PREFIX + i + "_y", 0);
            buttons.add(new ScoreButton(score, x, y));
        }
        return buttons;
    }

    public void setScoreButtons(List<ScoreButton> buttons) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(KEY_SCORE_BUTTONS_COUNT, buttons.size());
        for (int i = 0; i < buttons.size(); i++) {
            ScoreButton btn = buttons.get(i);
            editor.putInt(KEY_SCORE_BUTTON_PREFIX + i + "_score", btn.score);
            editor.putInt(KEY_SCORE_BUTTON_PREFIX + i + "_x", btn.x);
            editor.putInt(KEY_SCORE_BUTTON_PREFIX + i + "_y", btn.y);
        }
        editor.apply();
    }

    public ScoreButton findClosestScoreButton(int targetScore) {
        List<ScoreButton> buttons = getScoreButtons();
        if (buttons.isEmpty()) return null;
        
        ScoreButton closest = buttons.get(0);
        int minDiff = Math.abs(targetScore - closest.score);
        
        for (ScoreButton btn : buttons) {
            int diff = Math.abs(targetScore - btn.score);
            if (diff < minDiff) {
                minDiff = diff;
                closest = btn;
            }
        }
        return closest;
    }

    public static class ScoreButton {
        public int score;
        public int x;
        public int y;

        public ScoreButton(int score, int x, int y) {
            this.score = score;
            this.x = x;
            this.y = y;
        }
    }
}
