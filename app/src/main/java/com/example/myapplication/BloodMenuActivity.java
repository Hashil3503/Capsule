package com.example.myapplication;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.materialswitch.MaterialSwitch;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class BloodMenuActivity extends AppCompatActivity {
    private BloodSugarRepository bloodSugarRepository;
    private BloodPressureRepository bloodPressureRepository;
    private boolean empty_stomach;

    Calendar regDate_pressure, regDate_sugar;
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm", Locale.KOREAN);

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 시스템 설정에 따라 다크모드 또는 라이트모드 자동 적용
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_blood_menu);

        bloodSugarRepository = new BloodSugarRepository(getApplication());
        bloodPressureRepository = new BloodPressureRepository(getApplication());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 혈당 버튼
        Button botton_BloodSugar = findViewById(R.id.botton_BloodSugar);
        botton_BloodSugar.setOnClickListener(v -> runOnUiThread(() -> {
            LayoutInflater dialogInflater = LayoutInflater.from(BloodMenuActivity.this);
            View editView = dialogInflater.inflate(R.layout.blood_sugar_frame, null);

            regDate_sugar = Calendar.getInstance();
            @SuppressLint("CutPasteId") Button editSugarRegDate = editView.findViewById(R.id.editRegDate);
            editSugarRegDate.setText("등록 일시 :" + sdf.format(regDate_sugar.getTime()));

            editSugarRegDate.setOnClickListener(v1 -> showDatePicker(regDate_sugar, editSugarRegDate));

            MaterialSwitch materialSwitch = editView.findViewById(R.id.switch_empty_stomach);
            empty_stomach = false;
            materialSwitch.setChecked(false);
            materialSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> empty_stomach = isChecked);

            EditText valueEdit = editView.findViewById(R.id.editvalue);

            new androidx.appcompat.app.AlertDialog.Builder(BloodMenuActivity.this)
                    .setTitle("혈당 정보 입력")
                    .setView(editView)
                    .setPositiveButton("확인", (dialog, which) -> new Thread(() -> {
                        Date selectedDate = regDate_sugar.getTime();
                        BloodSugar bloodSugar = new BloodSugar(selectedDate);
                        bloodSugar.setEmpty_stomach(empty_stomach);
                        bloodSugar.setValue(CommonMethod.parseFloat(String.valueOf(valueEdit.getText())));
                        bloodSugarRepository.insert(bloodSugar);

                        runOnUiThread(() -> Toast.makeText(BloodMenuActivity.this, "혈당 정보를 추가하였습니다!", Toast.LENGTH_SHORT).show());
                    }).start())
                    .setNegativeButton("취소", null)
                    .show();
        }));

        // 혈압 버튼
        Button botton_BloodPressure = findViewById(R.id.botton_BloodPressure);
        botton_BloodPressure.setOnClickListener(v -> runOnUiThread(() -> {
            LayoutInflater dialogInflater = LayoutInflater.from(BloodMenuActivity.this);
            View editView = dialogInflater.inflate(R.layout.blood_pressure_frame, null);

            regDate_pressure = Calendar.getInstance();
            @SuppressLint("CutPasteId") Button editPressuerRegDate = editView.findViewById(R.id.editRegDate);
            editPressuerRegDate.setText("등록 일시 :" + sdf.format(regDate_pressure.getTime()));

            editPressuerRegDate.setOnClickListener(v1 -> showDatePicker(regDate_pressure, editPressuerRegDate));

            EditText systalicEdit = editView.findViewById(R.id.editsystolic);
            EditText diastolicEdit = editView.findViewById(R.id.editdiastolic);

            new androidx.appcompat.app.AlertDialog.Builder(BloodMenuActivity.this)
                    .setTitle("혈압 정보 입력")
                    .setView(editView)
                    .setPositiveButton("확인", (dialog, which) -> new Thread(() -> {
                        Date selectedDate = regDate_pressure.getTime();
                        BloodPressure bloodPressure = new BloodPressure(selectedDate);
                        bloodPressure.setSystolic(CommonMethod.parseInteger(String.valueOf(systalicEdit.getText())));
                        bloodPressure.setDiastolic(CommonMethod.parseInteger(String.valueOf(diastolicEdit.getText())));
                        bloodPressureRepository.insert(bloodPressure);

                        runOnUiThread(() -> Toast.makeText(BloodMenuActivity.this, "혈압 정보를 추가하였습니다!", Toast.LENGTH_SHORT).show());
                    }).start())
                    .setNegativeButton("취소", null)
                    .show();
        }));

        // 혈압/혈당 보기 버튼
        Button button_ViewBlood = findViewById(R.id.button_ViewBlood);
        button_ViewBlood.setOnClickListener(v -> {
            Intent intent = new Intent(BloodMenuActivity.this, BloodViewActivity.class);
            startActivity(intent);
        });

        // 그래프 보기 버튼
        Button button_ViewGraph = findViewById(R.id.button_ViewGraph);
        button_ViewGraph.setOnClickListener(v -> {
            Intent intent = new Intent(BloodMenuActivity.this, GraphActivity.class);
            startActivity(intent);
        });
    }

    private void showDatePicker(Calendar target, Button button) {
        int year = target.get(Calendar.YEAR);
        int month = target.get(Calendar.MONTH);
        int day = target.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog = new DatePickerDialog(this, (view, y, m, d) -> {
            target.set(y, m, d);

            int hour = target.get(Calendar.HOUR_OF_DAY);
            int minute = target.get(Calendar.MINUTE);

            @SuppressLint("SetTextI18n") TimePickerDialog timeDialog = new TimePickerDialog(this, (view1, h, min) -> {
                target.set(Calendar.HOUR_OF_DAY, h);
                target.set(Calendar.MINUTE, min);
                target.set(Calendar.SECOND, 0);

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA);
                String selectedDateTime = sdf.format(target.getTime());

                button.setText("등록 일시 " + selectedDateTime);
            }, hour, minute, true);

            timeDialog.show();
        }, year, month, day);

        dialog.show();
    }
}

