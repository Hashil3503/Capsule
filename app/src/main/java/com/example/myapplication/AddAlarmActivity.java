package com.example.myapplication;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AddAlarmActivity extends AppCompatActivity {

    private PrescriptionRepository prescriptionRepository;
    Button btnStartDate, btnEndDate, btnTime, btnSetAlarm, btnViewAlarms, btnCancelAlarms;
    TextView tvAlarmTimes;

    Calendar startDate;
    Calendar endDate;
    ArrayList<Calendar> alarmTimes = new ArrayList<>();
    ArrayList<AlarmItem> alarmList = new ArrayList<>();

    long pid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_alarm);

        //배터리 최적화에서 제외? 권한 얻기
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        }

        //다른 앱 위에 표시 권한 얻기
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        }

        // Android 13 이상 알림 권한 요청
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }

        // Android 12(API 31) 이상에서 정확한 알람 예약 권한 요청
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (!alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
                Toast.makeText(this, "정확한 알람 권한이 필요합니다.", Toast.LENGTH_LONG).show();
                return;
            }
        }

        prescriptionRepository = new PrescriptionRepository(getApplication());

        pid = getIntent().getLongExtra("prescription_id", -1); //이전 액티비티에서 전달한 처방전 id값 받아오기

        startDate = Calendar.getInstance();
        endDate = Calendar.getInstance();

        btnStartDate = findViewById(R.id.btnStartDate);
        btnEndDate = findViewById(R.id.btnEndDate);
        btnTime = findViewById(R.id.btnTime);
        btnSetAlarm = findViewById(R.id.btnSetAlarm);
        btnViewAlarms = findViewById(R.id.btnViewAlarms);
        btnCancelAlarms = findViewById(R.id.btnCancelAlarms);
        tvAlarmTimes = findViewById(R.id.tvAlarmTimes);

        new Thread(() -> {
            Prescription prescription = prescriptionRepository.getPrescriptionById(pid);
            if (prescription != null) {
                Date date = prescription.getDate();
                int duration = prescription.getDuration();

                runOnUiThread(() -> {

                    startDate.setTime(date);
                    endDate.setTime(date); //setTime()은 Date 타입을 Calendar 타입으로 변환할 때 사용하는 메서드.
                    endDate.add(Calendar.DATE, duration - 1); // 날짜 + 복용일수 - 1 = 마지막 복용일

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREAN);
                    btnStartDate.setText("시작 날짜: " + sdf.format(date));
                    btnEndDate.setText("종료 날짜: " + sdf.format(endDate.getTime()));
                });
            }
        }).start();

        btnStartDate.setOnClickListener(v ->
                showDatePicker(startDate, btnStartDate, "시작 날짜: ", selected -> startDate = selected)
        );

        btnEndDate.setOnClickListener(v ->
                showDatePicker(endDate, btnEndDate, "종료 날짜: ", selected -> endDate = selected)
        );

        btnTime.setOnClickListener(v -> {
            if (alarmTimes.size() >= 5) {
                Toast.makeText(this, "최대 5개의 알람 시간만 설정할 수 있습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            Calendar now = Calendar.getInstance();
            int hour = now.get(Calendar.HOUR_OF_DAY);
            int minute = now.get(Calendar.MINUTE);

            TimePickerDialog dialog = new TimePickerDialog(this, (view, selectedHour, selectedMinute) -> {
                Calendar selectedTime = Calendar.getInstance();
                selectedTime.set(Calendar.HOUR_OF_DAY, selectedHour);
                selectedTime.set(Calendar.MINUTE, selectedMinute);
                selectedTime.set(Calendar.SECOND, 0);
                selectedTime.set(Calendar.MILLISECOND, 0);

                alarmTimes.add(selectedTime);
                updateAlarmTimeDisplay();

                Toast.makeText(this, String.format("추가된 시간: %02d:%02d (%d/5)", selectedHour, selectedMinute, alarmTimes.size()), Toast.LENGTH_SHORT).show();
            }, hour, minute, true);

            dialog.show();
        });

        btnSetAlarm.setOnClickListener(v -> {

            endDate.set(Calendar.HOUR_OF_DAY, 23);
            endDate.set(Calendar.MINUTE, 59);
            endDate.set(Calendar.SECOND, 59);
            endDate.set(Calendar.MILLISECOND, 999);


            Log.d("AlarmDebug", "startDate: " + startDate.getTime());
            Log.d("AlarmDebug", "endDate: " + endDate.getTime());

            if (alarmTimes.isEmpty()) {
                Toast.makeText(this, "알람 시간을 1개 이상 설정해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            new Thread(() -> {
                Calendar tempDate = (Calendar) startDate.clone();
                tempDate.set(Calendar.HOUR_OF_DAY, 0);
                tempDate.set(Calendar.MINUTE, 0);
                tempDate.set(Calendar.SECOND, 0);
                tempDate.set(Calendar.MILLISECOND, 0);

                int count = 0;
                while (!tempDate.after(endDate)) {

                    Log.d("AlarmDebug", "tempDate: " + tempDate.getTime());
                    Log.d("AlarmDebug", "alarmTimes size: " + alarmTimes.size());

                    for (Calendar time : alarmTimes) {
                        Calendar alarmDateTime = (Calendar) tempDate.clone();
                        alarmDateTime.set(Calendar.HOUR_OF_DAY, time.get(Calendar.HOUR_OF_DAY));
                        alarmDateTime.set(Calendar.MINUTE, time.get(Calendar.MINUTE));
                        alarmDateTime.set(Calendar.SECOND, 0);
                        alarmDateTime.set(Calendar.MILLISECOND, 0);

                        Log.d("AlarmDebug", "계산된 알람 시간: " + alarmDateTime.getTime());
                        Log.d("AlarmDebug", "지금 시간: " + System.currentTimeMillis());


                        if (alarmDateTime.getTimeInMillis() <= System.currentTimeMillis()) {
                            Log.d("AlarmDebug", "과거 시간이므로 알람 스킵: " + alarmDateTime.getTime());
                            continue;
                        }

                        setAlarm(alarmDateTime);
                        count++;

                        Log.d("AlarmDebug", "알람 대상 날짜: " + alarmDateTime.getTime());
                        Log.d("AlarmDebug", "현재 시간: " + Calendar.getInstance().getTime());

                    }
                    tempDate.add(Calendar.DATE, 1);
                }
                int finalCount = count;
                runOnUiThread(() -> {
                    Toast.makeText(this, "총 " + finalCount + "개의 알람이 설정되었습니다.", Toast.LENGTH_LONG).show();
                    alarmTimes.clear();
                    updateAlarmTimeDisplay();

                });

            }).start();
        });

        btnViewAlarms.setOnClickListener(v -> {
            Intent intent = new Intent(this, AlarmListActivity.class);
            intent.putExtra("prescription_id", pid); // 이전 액티비티에서 받은 처방전 ID를 다시 다음 액티비티로 전달
            startActivity(intent);
        });

        btnCancelAlarms.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(AddAlarmActivity.this)
                    .setTitle("알람 삭제")
                    .setMessage("알람을 삭제합니다.")
                    .setPositiveButton("삭제", (dialog, which) -> {
                        new Thread(() -> {
                            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                            if (alarmManager != null) {
                                // 삭제 대상 외의 알람만 남기기 위한 리스트 생성
                                ArrayList<AlarmItem> remainingAlarms = new ArrayList<>();

                                for (AlarmItem item : alarmList) {
                                    if (item.getPid() == pid) { //현재 처방전 ID와 일치하는 경우만 삭제
                                        Intent intent = new Intent(this, AlarmReceiver.class);
                                        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                                                this,
                                                item.getRequestCode(),
                                                intent,
                                                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                                        );
                                        alarmManager.cancel(pendingIntent);
                                    } else {
                                        // 삭제 대상이 아니면 새 리스트에 유지
                                        remainingAlarms.add(item);
                                    }
                                }

                                // 기존 alarmList를 남겨둘 알람들로 교체 alarmList.clear(); 대신 이것을 사용함.
                                // 조건문에서 삭제할 알람을 지정해서 삭제 했음에도 별도로 남겨둘 알람 리스트를 사용하는 이유는 위 과정에서 삭제되는 것은 알람 리스트가 아닌 시스템 알람이기 때문.
                                // 즉 실제로 동작하는 시스템 알람만 삭제되고, alarmList에서는 삭제되지 않기 때문에 별도로 alarmList를 갱신해주어야 한다.
                                alarmList = remainingAlarms;
                                saveAlarms(); // 저장소에도 반영
                            }
                            runOnUiThread(() -> {
                                alarmTimes.clear();
                                updateAlarmTimeDisplay();
                                Toast.makeText(this, "처방전"+pid+"의 알람이 일괄 취소되었습니다.", Toast.LENGTH_SHORT).show();
                            });
                        }).start();
                    })
                    .setNegativeButton("취소", null)
                    .show();
        });

        loadAlarms(); // 이 메서드로 기존 알람 목록을 불러옴. (선언은 버튼 리스너 보다 늦게 선언했지만, 실제로 호출은 이 메서드가 먼저 호출되기 때문에 작동에 문제는 없음)
        updateAlarmTimeDisplay();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(this, "정확한 알람 권한이 아직 허용되지 않았습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setAlarm(Calendar calendar) {
        long timeInMillis = calendar.getTimeInMillis();
        int requestCode = (calendar.toString() + pid).hashCode();

        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("requestCode", requestCode);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        //Log.d("Alarm", "setAlarm: " + calendar.getTime() + ", requestCode=" + requestCode);

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmManager != null) {
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
            } catch (SecurityException e) {
                e.printStackTrace();
                Toast.makeText(this, "정확한 알람 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        AlarmItem alarm = new AlarmItem(calendar, requestCode);
        alarm.setPid((int) pid);
        alarmList.add(alarm);
        saveAlarms();
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
        ArrayList<AlarmItem> loaded = gson.fromJson(json, type);
        if (loaded != null) {
            alarmList = loaded;
        }
    }

    public interface DatePickerCallback {
        void onDateSet(Calendar selectedDate);
    }
    private void showDatePicker(Calendar initialDate, Button button, String label, DatePickerCallback callback) {
        int year = initialDate.get(Calendar.YEAR);
        int month = initialDate.get(Calendar.MONTH);
        int day = initialDate.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog = new DatePickerDialog(this, (view, y, m, d) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(y, m, d);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            button.setText(label + sdf.format(selected.getTime()));
            callback.onDateSet(selected);
        }, year, month, day);

        dialog.show();
    }



    private void updateAlarmTimeDisplay() {
        StringBuilder builder = new StringBuilder("추가된 알람 시간:\n");
        SimpleDateFormat tf = new SimpleDateFormat("HH:mm", Locale.getDefault());

        for (int i = 0; i < alarmTimes.size(); i++) {
            Calendar c = alarmTimes.get(i);
            builder.append(String.format(Locale.getDefault(), "%d. %s\n", i + 1, tf.format(c.getTime())));
        }

        tvAlarmTimes.setText(builder.toString());
    }
}
