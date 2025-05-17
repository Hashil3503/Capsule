package com.example.myapplication;


import com.example.myapplication.ChatRequest;
import com.example.myapplication.ChatResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ChatService {
    @POST("chat")
    Call<ChatResponse> sendMessage(@Body ChatRequest request);
}

