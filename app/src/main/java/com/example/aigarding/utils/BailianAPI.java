package com.example.aigarding.utils;

import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class BailianAPI {
    private static final String TAG = "BailianAPI";
    private static final String API_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation";
    
    private String apiKey;
    private String modelId;
    private OkHttpClient client;

    public interface ScoringCallback {
        void onSuccess(int score);
        void onError(String error);
    }

    public BailianAPI(String apiKey, String modelId) {
        this.apiKey = apiKey;
        this.modelId = modelId;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .build();
    }

    public void scoreAnswer(Bitmap image, String referenceAnswer, ScoringCallback callback) {
        new Thread(() -> {
            try {
                String base64Image = bitmapToBase64(image);
                
                JSONObject payload = new JSONObject();
                payload.put("model", modelId);
                
                JSONObject input = new JSONObject();
                JSONArray messages = new JSONArray();
                
                JSONObject message = new JSONObject();
                message.put("role", "user");
                
                JSONArray content = new JSONArray();
                
                // 参考答案文本
                JSONObject textPart = new JSONObject();
                textPart.put("text", "参考答案：" + referenceAnswer + "\n\n请根据上面的参考答案，给图片中的学生答卷评分，只返回得分数字（0-100之间的整数）。");
                content.put(textPart);
                
                // 图片
                JSONObject imagePart = new JSONObject();
                imagePart.put("image", "data:image/jpeg;base64," + base64Image);
                content.put(imagePart);
                
                message.put("content", content);
                messages.put(message);
                input.put("messages", messages);
                payload.put("input", input);
                
                RequestBody body = RequestBody.create(
                    payload.toString(),
                    MediaType.parse("application/json")
                );
                
                Request request = new Request.Builder()
                    .url(API_URL)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();
                
                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        callback.onError("API请求失败: " + response.code());
                        return;
                    }
                    
                    String responseBody = response.body().string();
                    Log.d(TAG, "Response: " + responseBody);
                    
                    int score = parseScore(responseBody);
                    callback.onSuccess(score);
                }
            } catch (Exception e) {
                Log.e(TAG, "API Error", e);
                callback.onError(e.getMessage());
            }
        }).start();
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
        byte[] byteArray = outputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.NO_WRAP);
    }

    private int parseScore(String responseStr) {
        try {
            JSONObject json = new JSONObject(responseStr);
            JSONObject output = json.getJSONObject("output");
            JSONArray choices = output.getJSONArray("choices");
            JSONObject choice = choices.getJSONObject(0);
            JSONObject message = choice.getJSONObject("message");
            String content = message.getString("content");
            
            String cleanContent = content.replaceAll("[^0-9]", "");
            if (!cleanContent.isEmpty()) {
                int score = Integer.parseInt(cleanContent);
                return Math.max(0, Math.min(100, score)); 
            }
        } catch (Exception e) {
            Log.e(TAG, "Parse score error", e);
        }
        return 0;
    }
}
