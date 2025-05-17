package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class AlarmStopActivity extends AppCompatActivity {

    private Ringtone ringtone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        );

        setContentView(R.layout.activity_alarm_stop);

        // 벨소리 울리기
        Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarmUri == null) {
            alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }

        ringtone = RingtoneManager.getRingtone(this, alarmUri);
        if (ringtone != null) {
            ringtone.play();
        }

        //알람 객체 불러오기
        int requestCode = getIntent().getIntExtra("requestCode", -1);

        SharedPreferences prefs = getSharedPreferences("alarms", MODE_PRIVATE);
        Gson gson = new Gson();
        String json = prefs.getString("alarm_list", null);
        Type type = new TypeToken<ArrayList<AlarmItem>>() {}.getType();
        ArrayList<AlarmItem> alarmList = gson.fromJson(json, type);

        TextView textView = findViewById(R.id.prescription);
        if (alarmList != null) {
            for (AlarmItem item : alarmList) {
                if (item.getRequestCode() == requestCode) {
                    textView.setText("처방전" + item.getPid()); // pid 값을 텍스트로 표시
                    break;
                }
            }
        }

        Button btnStop = findViewById(R.id.btnStopAlarm);
        btnStop.setOnClickListener(v -> {
            if (ringtone != null && ringtone.isPlaying()) {
                ringtone.stop();
                stopService(new Intent(this, AlarmForegroundService.class));
            }

            // 🔸 복용 완료 표시
            if (alarmList != null) {
                for (AlarmItem item : alarmList) {
                    if (item.getRequestCode() == requestCode) {
                        item.setTaken(true); // 복용 완료 상태로 변경
                        break;
                    }
                }

                // 다시 저장
                String updatedJson = gson.toJson(alarmList);
                prefs.edit().putString("alarm_list", updatedJson).apply();
            }
            finish();
        });
    }
    @Override
    public void onBackPressed() {
        // 뒤로가기 버튼을 터치해도 아무 동작도 하지 않도록 하기 위함. (뒤로가기로 알람 화면을 나가면 알람이 정상 종료 되지 않아서 의도되지 않은 버그가 발생함.
    }
}
