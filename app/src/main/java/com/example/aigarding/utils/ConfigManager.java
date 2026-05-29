package com.example.aigarding.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class ConfigManager {

    private static final String PREF_NAME = "AI_GARDING_CONFIG";
    private static final String KEY_API_KEY = "api_key";
    private static final String KEY_API_URL = "api_url";
    private static final String KEY_TIMEOUT = "timeout";
    private static final String KEY_AREA_TOP = "area_top";
    private static final String KEY_AREA_LEFT = "area_left";
    private static final String KEY_AREA_BOTTOM = "area_bottom";
    private static final String KEY_AREA_RIGHT = "area_right";

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

    public String getApiUrl() {
        return preferences.getString(KEY_API_URL, "https://dashscope.aliyuncs.com/api/text/image");
    }

    public void setApiUrl(String apiUrl) {
        preferences.edit().putString(KEY_API_URL, apiUrl).apply();
    }

    public int getTimeout() {
        return preferences.getInt(KEY_TIMEOUT, 30);
    }

    public void setTimeout(int timeout) {
        preferences.edit().putInt(KEY_TIMEOUT, timeout).apply();
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
}