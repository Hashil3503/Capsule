package com.example.myapplication;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

//처방전별 알람 목록
public class AlarmListActivity extends AppCompatActivity {

    ListView listView;
    ArrayList<AlarmItem> alarmList;
    ArrayAdapter<String> adapter;
    ArrayList<String> displayList;

    private int pid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_list);

        long prescription_id = getIntent().getLongExtra("prescription_id", -1);

        listView = findViewById(R.id.listViewAlarms);

        // 1. 알람 목록 불러오기
        SharedPreferences prefs = getSharedPreferences("alarms", MODE_PRIVATE);
        Gson gson = new Gson();
        String json = prefs.getString("alarm_list", null);
        Type type = new TypeToken<ArrayList<AlarmItem>>() {}.getType();
        alarmList = gson.fromJson(json, type);
        if (alarmList == null) alarmList = new ArrayList<>();

        displayList = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd (E) HH:mm", Locale.KOREAN);

        for (AlarmItem item : alarmList) {
            pid = item.getPid();
            if(pid == (int)prescription_id) { // 선택한 처방전의 알람 목록만 조회하기 위함.
                String timeStr = "처방전" + pid + "  ";
                timeStr += sdf.format(item.getDateTime().getTime());
                if (item.isTaken()) {
                    timeStr += "  ✅ 복용완료";
                }
                displayList.add(timeStr);
            }
        }

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, displayList);
        listView.setAdapter(adapter);

        // 2. 길게 누르면 알람 삭제
        listView.setOnItemLongClickListener((adapterView, view, position, id) -> {
            new androidx.appcompat.app.AlertDialog.Builder(AlarmListActivity.this)
                    .setTitle("알람 삭제")
                    .setMessage("알람을 삭제합니다.")
                    .setPositiveButton("삭제", (dialog, which) -> {
                        new Thread(() -> {
                            AlarmItem removedItem = alarmList.get(position);

                            // 알람 취소
                            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                            if (alarmManager != null) {
                                Intent intent = new Intent(this, AlarmReceiver.class);
                                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                                        this,
                                        removedItem.getRequestCode(),
                                        intent,
                                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                                );
                                alarmManager.cancel(pendingIntent);
                            }

                            // 리스트와 저장소에서 제거
                            alarmList.remove(position);
                            displayList.remove(position);


                            // 저장소 반영
                            String updatedJson = new Gson().toJson(alarmList);
                            prefs.edit().putString("alarm_list", updatedJson).apply();

                            runOnUiThread(() -> {
                                adapter.notifyDataSetChanged();
                                Toast.makeText(this, "알람이 삭제되었습니다!", Toast.LENGTH_SHORT).show();
                            });
                        }).start();
                    })
                    .setNegativeButton("취소", null)
                    .show();
            return true;
        });
    }
}
