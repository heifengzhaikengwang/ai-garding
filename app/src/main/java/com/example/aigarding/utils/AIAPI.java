package com.example.aigarding.utils;

import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AIAPI {

    private static final String TAG = "AIAPI";
    private static final String BASE_URL = "https://dashscope.aliyuncs.com/api/text/image";
    
    private String apiKey;
    private OkHttpClient client;

    public AIAPI(String apiKey) {
        this.apiKey = apiKey;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .build();
    }

    public interface OnAIResponseListener {
        void onSuccess(int score);
        void onError(String error);
    }

    public void sendImageForScore(Bitmap bitmap, OnAIResponseListener listener) {
        new Thread(() -> {
            try {
                String base64Image = bitmapToBase64(bitmap);
                JSONObject requestBody = new JSONObject();
                requestBody.put("model", "qwen-vl-plus");
                requestBody.put("input", new JSONObject()
                        .put("image", base64Image)
                        .put("prompt", "请对这份答卷进行评分，满分100分，只返回分数数字"));
                requestBody.put("parameters", new JSONObject()
                        .put("max_tokens", 10));

                RequestBody body = RequestBody.create(
                        requestBody.toString(),
                        MediaType.parse("application/json")
                );

                Request request = new Request.Builder()
                        .url(BASE_URL)
                        .addHeader("Authorization", "Bearer " + apiKey)
                        .addHeader("Content-Type", "application/json")
                        .post(body)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        listener.onError("API请求失败: " + response.code());
                        return;
                    }

                    String responseBody = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    
                    if (jsonResponse.has("output")) {
                        String result = jsonResponse.getJSONObject("output")
                                .getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content");
                        
                        int score = parseScore(result);
                        listener.onSuccess(score);
                    } else {
                        listener.onError("解析响应失败");
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "API请求异常", e);
                listener.onError(e.getMessage());
            }
        }).start();
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
        byte[] byteArray = outputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private int parseScore(String response) {
        try {
            String cleanResponse = response.replaceAll("[^0-9]", "");
            if (!cleanResponse.isEmpty()) {
                return Integer.parseInt(cleanResponse);
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "解析分数失败", e);
        }
        return 0;
    }
}