package com.example.myapplication;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.*;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class Llama2ApiClient {
    // 실제 서버 IP 주소로 변경 (예: "http://192.168.200.196:8000/generate")
    private static final String API_URL = "http://192.168.200.196:8000/generate";
    private final OkHttpClient client;
    private final Gson gson;

    public Llama2ApiClient() {
        client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)  // 타임아웃 시간 증가
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)  // 연결 실패 시 재시도
                .build();
        gson = new Gson();
    }

    public void searchMedicineInfo(String medicineName, ApiCallback callback) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("prompt", createMedicinePrompt(medicineName));
        
        RequestBody requestBody = RequestBody.create(
                jsonObject.toString(), MediaType.parse("application/json"));
        
        Request request = new Request.Builder()
                .url(API_URL)
                .post(requestBody)
                .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(new IOException("서버 연결 실패: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    callback.onSuccess(responseData);
                } else {
                    callback.onFailure(new IOException("API 요청 실패: " + response.code()));
                }
                response.close();
            }
        });
    }

    private String createMedicinePrompt(String medicineName) {
        return String.format(
            "다음 약품에 대한 상세 정보를 알려주세요:\n" +
            "약품명: %s\n\n" +
            "다음 형식으로 응답해주세요:\n" +
            "성분: [성분]\n" +
            "효능/효과: [효능/효과]\n" +
            "제형: [제형]\n" +
            "용법/용량: [용법/용량]\n" +
            "주의사항: [주의사항]\n" +
            "투약량/횟수/일수: [투약량/횟수/일수]\n\n" +
            "각 항목은 실제 의약품 정보를 기반으로 작성해주세요.",
            medicineName
        );
    }

    public interface ApiCallback {
        void onSuccess(String response);
        void onFailure(Throwable throwable);
    }
} 