package com.example.myapplication;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

//처방전별 알람 목록
public class AllAlarmListActivity extends AppCompatActivity {

    ListView listView;
    ArrayList<AlarmItem> alarmList = new ArrayList<>();
    ArrayAdapter<String> adapter;
    ArrayList<String> displayList;

    Button deleteButton;

    private int pid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_alarm_list);

        listView = findViewById(R.id.listViewAlarms);

        deleteButton = findViewById(R.id.deleteAll);
        // 1. 알람 목록 불러오기
        loadAlarms();

        displayList = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd (E) HH:mm", Locale.KOREAN);

        for (AlarmItem item : alarmList) {
            pid = item.getPid();
            String timeStr = "처방전"+pid+" ";
            timeStr += sdf.format(item.getDateTime().getTime());
            if (item.isTaken()) {
                timeStr += "  ✅ 복용완료";
            }
            displayList.add(timeStr);
        }

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, displayList);
        listView.setAdapter(adapter);

        // 2. 길게 누르면 알람 삭제
        listView.setOnItemLongClickListener((adapterView, view, position, id) -> {
            new androidx.appcompat.app.AlertDialog.Builder(AllAlarmListActivity.this)
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
                            saveAlarms();

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



        deleteButton.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(AllAlarmListActivity.this)
                    .setTitle("알람 삭제")
                    .setMessage("알람을 삭제합니다.")
                    .setPositiveButton("삭제", (dialog, which) -> {
                        new Thread(() -> {
                            // 모든 알람 취소
                            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                            if (alarmManager != null) {
                                for (AlarmItem item : alarmList) {
                                    Intent intent = new Intent(this, AlarmReceiver.class);
                                    PendingIntent pendingIntent = PendingIntent.getBroadcast(
                                            this,
                                            item.getRequestCode(),
                                            intent,
                                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                                    );
                                    alarmManager.cancel(pendingIntent);
                                }
                            }

                            // 리스트와 저장소에서 모든 알람 제거
                            alarmList.clear();
                            displayList.clear();

                            // 저장소 반영
                            saveAlarms();

                            runOnUiThread(() -> {
                                adapter.notifyDataSetChanged();
                                Toast.makeText(this, "모든 알람이 삭제되었습니다!", Toast.LENGTH_SHORT).show();
                            });
                        }).start();
                    })
                    .setNegativeButton("취소", null)
                    .show();
        });
    }
    private void saveAlarms() {
        SharedPreferences prefs = getSharedPreferences("alarms", MODE_PRIVATE);
        Gson gson = new Gson();
        String json = gson.toJson(alarmList);
        prefs.edit().putString("alarm_list", json).apply();
    }
    private void loadAlarms() {
        SharedPreferences prefs = getSharedPreferences("alarms", MODE_PRIVATE);
        Gson gson = new Gson();
        String json = prefs.getString("alarm_list", null);
        Type type = new TypeToken<ArrayList<AlarmItem>>() {}.getType();
        alarmList = gson.fromJson(json, type);
        if (alarmList == null) alarmList = new ArrayList<>();
    }

}
