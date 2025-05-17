package com.example.myapplication;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ChatBotActivity extends AppCompatActivity {
    private static final String SERVER_IP = "/1.251.29.63"; // 공인 IP 주소
    private static final String SERVER_PORT = "8000"; // 고정 포트 번호

    private RecyclerView chatRecyclerView;
    private EditText messageEditText;
    private Button sendButton;
    private ProgressBar progressBar;
    private ChatAdapter chatAdapter;
    private ChatService chatService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        // 네트워크 상태 확인
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "네트워크 연결을 확인해주세요.", Toast.LENGTH_LONG).show();
        }

        // UI 요소 초기화
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        messageEditText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendButton);
        progressBar = findViewById(R.id.progressBar);

        // RecyclerView 설정
        chatAdapter = new ChatAdapter();
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);

        // Retrofit 초기화
        initializeRetrofit();

        // 전송 버튼 클릭 이벤트 설정
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isNetworkAvailable()) {
                    Toast.makeText(ChatBotActivity.this, "네트워크 연결을 확인해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }
                String message = messageEditText.getText().toString().trim();
                if (!message.isEmpty()) {
                    sendMessage(message);
                    messageEditText.setText("");
                }
            }
        });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void initializeRetrofit() {
        String baseUrl = "http://" + SERVER_IP + ":" + SERVER_PORT + "/";
        
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(120, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        chatService = retrofit.create(ChatService.class);
    }

    private void sendMessage(String message) {
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "네트워크 연결을 확인해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        chatAdapter.addMessage(new Message(message, Message.TYPE_USER));

        ChatRequest request = new ChatRequest(message);
        Call<ChatResponse> call = chatService.sendMessage(request);
        
        call.enqueue(new Callback<ChatResponse>() {
            @Override
            public void onResponse(Call<ChatResponse> call, Response<ChatResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    String botResponse = response.body().getGenerated_text();
                    if (botResponse != null && !botResponse.isEmpty()) {
                        chatAdapter.addMessage(new Message(botResponse, Message.TYPE_BOT));
                        chatRecyclerView.scrollToPosition(chatAdapter.getItemCount() - 1);
                    } else {
                        Toast.makeText(ChatBotActivity.this, "응답이 비어있습니다.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String errorMessage = "응답 오류 발생: " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            errorMessage += "\n" + errorBody;
                            System.out.println("Server Error Response: " + errorBody);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Toast.makeText(ChatBotActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ChatResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                String errorMessage = "네트워크 오류: " + t.getMessage();
                Toast.makeText(ChatBotActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                t.printStackTrace();
            }
        });
    }
}
