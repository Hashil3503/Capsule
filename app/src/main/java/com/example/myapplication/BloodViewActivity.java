package com.example.myapplication;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.materialswitch.MaterialSwitch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BloodViewActivity extends AppCompatActivity {

    private BloodSugarRepository bloodSugarRepository;
    private BloodPressureRepository bloodPressureRepository;
    private int Category; // 1이면 혈당 0이면 혈압, 그 외에는 아무것도 화면에 표시 안할 예정.
    private boolean empty_stomach;

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 MM월 dd일(E) hh:mm", Locale.KOREAN);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_blood_view);

        bloodSugarRepository = new BloodSugarRepository(getApplication());
        bloodPressureRepository = new BloodPressureRepository(getApplication());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Category = 2; // 카테고리 기본값 설정. (초기 화면에 아무것도 띄우지 않기 위함.)

        Button sugarBotton = findViewById(R.id.Category1);
        Button pressureBotton = findViewById(R.id.Category2);

        sugarBotton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Category = 0;
                reload();
            }
        });

        pressureBotton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Category = 1;
                reload();
            }
        });
    }

    private boolean isBloodSugarNormal(float value, boolean emptyStomach) {
        if (emptyStomach) {
            return value >= 70 && value < 100;
        } else {
            return value >= 70 && value < 140;
        }
    }

    private boolean isSystolicNormal(int systolic) {
        return systolic >= 90 && systolic < 120;
    }

    private boolean isDiastolicNormal(int diastolic) {
        return diastolic >= 60 && diastolic < 80;
    }

    private void reload() {
        LinearLayout bloodContainer = findViewById(R.id.bloodContainer);
        bloodContainer.removeAllViews();

        new Thread(() -> { //백그라운드 스레드 생성 (여기서 데이터베이스 관련 메서드가 작동해야함. 안그러면 앱 터짐.

            List<View> bloodFrames = new ArrayList<>();// 반복문에서 UI작업이 많이 일어나기 때문에 일괄처리를 위해 작업을 담아둘 리스트 생성.


            if(Category == 0){
                List<BloodSugar> bloodSugars = bloodSugarRepository.getAllBloodsugars(); //모든 혈당 레코드 가져오기
                for (BloodSugar bloodSugar : bloodSugars) {// 모든 혈당 레코드 순회
                    View sugarFrame = LayoutInflater.from(BloodViewActivity.this).inflate(R.layout.sugar_view_frame, null);

                    TextView sugarDateTextView = sugarFrame.findViewById(R.id.sugarDate);
                    TextView isEmptyTextView = sugarFrame.findViewById(R.id.isEmpty);
                    TextView sugarValueTextView = sugarFrame.findViewById(R.id.sugarValue);



                    sugarDateTextView.setText(sdf.format(bloodSugar.getDate()));

                    boolean isNormal = isBloodSugarNormal(bloodSugar.getValue(), bloodSugar.getEmpty_stomach());
                    runOnUiThread(() -> {
                        sugarValueTextView.setText("혈당치: " + bloodSugar.getValue());
                        isEmptyTextView.setText("공복 여부: " + bloodSugar.getEmpty_stomach());

                        int normalColor = ContextCompat.getColor(BloodViewActivity.this, R.color.textPrimary);
                        int color = isNormal ? normalColor : Color.RED;
                        sugarValueTextView.setTextColor(color);
                    });

                    bloodFrames.add(sugarFrame); // 나중에 한 번에 UI 추가

                    Button editButton = sugarFrame.findViewById(R.id.editButton);
                    Button deleteButton = sugarFrame.findViewById(R.id.deleteButton);

                    editButton.setOnClickListener(new View.OnClickListener() { //수정 버튼
                        @Override
                        public void onClick(View v) {
                            runOnUiThread(() -> {
                                LayoutInflater dialogInflater = LayoutInflater.from(BloodViewActivity.this);
                                View editView = dialogInflater.inflate(R.layout.blood_sugar_frame, null);

                                MaterialSwitch materialSwitch = editView.findViewById(R.id.switch_empty_stomach);
                                EditText valueEdit = editView.findViewById(R.id.editvalue);

                                //테스트용 버튼 안보이게
                                Button editRegDate = editView.findViewById(R.id.editRegDate);
                                editRegDate.setVisibility(View.GONE);
                                // 팝업창 초기값 표시
                                empty_stomach = bloodSugar.getEmpty_stomach(); //이걸 미리 선언 안하고 아래 메서드에 바로 집어넣으니까 수정할 때 값이 지맘대로 반영됨.
                                materialSwitch.setChecked(empty_stomach);

                                materialSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> { //isChecked는 MaterialSwitch에서 자동으로 할당되는 boolean 타입의 변수임. 1이면 enabled, 0이면 disabled.
                                    empty_stomach = isChecked; //if문 사용해도 되는데 이렇게 하는게 간결하다고 안드로이드 스튜디오가 추천함.
                                });
                                valueEdit.setText(String.valueOf(bloodSugar.getValue()));

                                new androidx.appcompat.app.AlertDialog.Builder(BloodViewActivity.this) //팝업창을 생성하는 도구
                                        .setTitle("혈당 정보 수정")
                                        .setView(editView)
                                        .setPositiveButton("수정", (dialog, which) -> { //dialog와 which는 내가 선언한 변수 아님. 버튼 클릭시 호출되는 매개 변수임. dialog = 현재 클릭된 다이얼로그 객체 자체를 참조. which = 어느 버튼이 눌렸는지 나타내는 int형 변수
                                            new Thread(() -> { // 수정사항을 medication 객체에 저장 (medication은 새로 생성된 객체가 아니고 기존 DB에서 조회한 객체임)
                                                // 또 new Thread를 사용하는 이유 : 코드 초반부의 new Thread는 리스너가 등록된 시점. 여기의 new Thread는 리스너가 실행되는 시점에 초점이 맞춰져 있음.
                                                // 리스너가 실행될 때 DB작업을 다시 수행하려면 또 백그라운드 스레드로 전환해줄 필요가 있음.
                                                bloodSugar.setEmpty_stomach(empty_stomach);
                                                bloodSugar.setValue(CommonMethod.parseFloat(String.valueOf(valueEdit.getText())));

                                                bloodSugarRepository.update(bloodSugar); // DB에 수정사항 반영
                                                boolean isNormal = isBloodSugarNormal(bloodSugar.getValue(), bloodSugar.getEmpty_stomach());
                                                runOnUiThread(() -> { //UI에도 수정사항 반영 (이전에 UI작업이 일어나고 값이 수정되어도 UI가 실시간으로 수정되지 않기 때문에 직접 UI를 업데이트 해야함)
                                                    sugarValueTextView.setText("혈당치: " + bloodSugar.getValue());
                                                    isEmptyTextView.setText("공복 여부: " + bloodSugar.getEmpty_stomach());
                                                    int normalColor = ContextCompat.getColor(BloodViewActivity.this, R.color.textPrimary);
                                                    int color = isNormal ? normalColor : Color.RED;
                                                    sugarValueTextView.setTextColor(color);
                                                    Toast.makeText(BloodViewActivity.this, "혈당 정보를 수정하였습니다!", Toast.LENGTH_SHORT).show();
                                                });
                                            }).start();
                                        })
                                        .setNegativeButton("취소", null)
                                        .show();
                            });
                        }
                    });

                    deleteButton.setOnClickListener(new View.OnClickListener() { //삭제 버튼
                        @Override
                        public void onClick(View v) {
                            runOnUiThread(() -> {
                                new androidx.appcompat.app.AlertDialog.Builder(BloodViewActivity.this) //팝업창을 생성하는 도구
                                        .setTitle("혈당 정보 삭제")
                                        .setMessage("정말로 삭제하시겠습니까?")
                                        .setPositiveButton("삭제", (dialog, which) -> { //dialog와 which는 내가 선언한 변수 아님. 버튼 클릭시 호출되는 매개 변수임. dialog = 현재 클릭된 다이얼로그 객체 자체를 참조. which = 어느 버튼이 눌렸는지 나타내는 int형 변수
                                            new Thread(() -> { // 수정사항을 medication 객체에 저장 (medication은 새로 생성된 객체가 아니고 기존 DB에서 조회한 객체임)
                                                // 또 new Thread를 사용하는 이유 : 코드 초반부의 new Thread는 리스너가 등록된 시점. 여기의 new Thread는 리스너가 실행되는 시점에 초점이 맞춰져 있음.
                                                // 리스너가 실행될 때 DB작업을 다시 수행하려면 또 백그라운드 스레드로 전환해줄 필요가 있음.
                                                bloodSugarRepository.delete(bloodSugar);

                                                runOnUiThread(() -> { // 삭제된 의약품을 UI에도 반영
                                                    bloodContainer.removeView(sugarFrame);
                                                    Toast.makeText(BloodViewActivity.this, "삭제하였습니다!", Toast.LENGTH_SHORT).show();
                                                });
                                            }).start();
                                        })
                                        .setNegativeButton("취소", null)
                                        .show();
                            });
                        }
                    });
                }
            }

            else if(Category == 1){
                List<BloodPressure> bloodPressures = bloodPressureRepository.getAllBloodPressures(); //모든 혈압 레코드 가져오기
                for (BloodPressure bloodPressure : bloodPressures) {// 모든 혈압 레코드 순회
                    View pressureFrame = LayoutInflater.from(BloodViewActivity.this).inflate(R.layout.pressure_view_frame, null);

                    TextView sugarDateTextView = pressureFrame.findViewById(R.id.pressureDate);
                    TextView systolicTextView = pressureFrame.findViewById(R.id.systolicValue);
                    TextView diastolicTextView = pressureFrame.findViewById(R.id.diastolicValue);

                    sugarDateTextView.setText(sdf.format(bloodPressure.getDate()));

                    boolean isNormal1 = isSystolicNormal(bloodPressure.getSystolic());
                    boolean isNormal2 = isDiastolicNormal(bloodPressure.getDiastolic());
                    runOnUiThread(() -> {
                        systolicTextView.setText("수축기: " + bloodPressure.getSystolic());
                        diastolicTextView.setText("이완기: " + bloodPressure.getDiastolic());

                        int normalColor = ContextCompat.getColor(BloodViewActivity.this, R.color.textPrimary);
                        int color1 = isNormal1 ? normalColor : Color.RED;
                        int color2 = isNormal2 ? normalColor : Color.RED;
                        systolicTextView.setTextColor(color1);
                        diastolicTextView.setTextColor(color2);
                    });

                    bloodFrames.add(pressureFrame); // 나중에 한 번에 UI 추가

                    Button editButton = pressureFrame.findViewById(R.id.editButton);
                    Button deleteButton = pressureFrame.findViewById(R.id.deleteButton);

                    editButton.setOnClickListener(new View.OnClickListener() { //수정 버튼
                        @Override
                        public void onClick(View v) {
                            runOnUiThread(() -> {
                                LayoutInflater dialogInflater = LayoutInflater.from(BloodViewActivity.this);
                                View editView = dialogInflater.inflate(R.layout.blood_pressure_frame, null);

                                EditText systalicEdit = editView.findViewById(R.id.editsystolic);
                                EditText diastolicEdit = editView.findViewById(R.id.editdiastolic);

                                //테스트용 버튼 안보이게
                                Button editRegDate = editView.findViewById(R.id.editRegDate);
                                editRegDate.setVisibility(View.GONE);
                                // 팝업창 초기값 표시
                                systalicEdit.setText(String.valueOf(bloodPressure.getSystolic()));
                                diastolicEdit.setText(String.valueOf(bloodPressure.getDiastolic()));

                                new androidx.appcompat.app.AlertDialog.Builder(BloodViewActivity.this) //팝업창을 생성하는 도구
                                        .setTitle("혈압 정보 수정")
                                        .setView(editView)
                                        .setPositiveButton("수정", (dialog, which) -> { //dialog와 which는 내가 선언한 변수 아님. 버튼 클릭시 호출되는 매개 변수임. dialog = 현재 클릭된 다이얼로그 객체 자체를 참조. which = 어느 버튼이 눌렸는지 나타내는 int형 변수
                                            new Thread(() -> { // 수정사항을 medication 객체에 저장 (medication은 새로 생성된 객체가 아니고 기존 DB에서 조회한 객체임)
                                                // 또 new Thread를 사용하는 이유 : 코드 초반부의 new Thread는 리스너가 등록된 시점. 여기의 new Thread는 리스너가 실행되는 시점에 초점이 맞춰져 있음.
                                                // 리스너가 실행될 때 DB작업을 다시 수행하려면 또 백그라운드 스레드로 전환해줄 필요가 있음.
                                                bloodPressure.setSystolic(CommonMethod.parseInteger(String.valueOf(systalicEdit.getText())));
                                                bloodPressure.setDiastolic(CommonMethod.parseInteger(String.valueOf(diastolicEdit.getText())));

                                                bloodPressureRepository.update(bloodPressure); // DB에 수정사항 반영
                                                boolean isNormal1 = isSystolicNormal(bloodPressure.getSystolic());
                                                boolean isNormal2 = isDiastolicNormal(bloodPressure.getDiastolic());
                                                runOnUiThread(() -> {
                                                    systolicTextView.setText("수축기: " + bloodPressure.getSystolic());
                                                    diastolicTextView.setText("이완기: " + bloodPressure.getDiastolic());

                                                    int normalColor = ContextCompat.getColor(BloodViewActivity.this, R.color.textPrimary);
                                                    int color1 = isNormal1 ? normalColor : Color.RED;
                                                    int color2 = isNormal2 ? normalColor : Color.RED;
                                                    systolicTextView.setTextColor(color1);
                                                    diastolicTextView.setTextColor(color2);
                                                });
                                            }).start();
                                        })
                                        .setNegativeButton("취소", null)
                                        .show();
                            });
                        }
                    });

                    deleteButton.setOnClickListener(new View.OnClickListener() { //삭제 버튼
                        @Override
                        public void onClick(View v) {
                            runOnUiThread(() -> {
                                new androidx.appcompat.app.AlertDialog.Builder(BloodViewActivity.this) //팝업창을 생성하는 도구
                                        .setTitle("혈당 정보 삭제")
                                        .setMessage("정말로 삭제하시겠습니까?")
                                        .setPositiveButton("삭제", (dialog, which) -> { //dialog와 which는 내가 선언한 변수 아님. 버튼 클릭시 호출되는 매개 변수임. dialog = 현재 클릭된 다이얼로그 객체 자체를 참조. which = 어느 버튼이 눌렸는지 나타내는 int형 변수
                                            new Thread(() -> { // 수정사항을 medication 객체에 저장 (medication은 새로 생성된 객체가 아니고 기존 DB에서 조회한 객체임)
                                                // 또 new Thread를 사용하는 이유 : 코드 초반부의 new Thread는 리스너가 등록된 시점. 여기의 new Thread는 리스너가 실행되는 시점에 초점이 맞춰져 있음.
                                                // 리스너가 실행될 때 DB작업을 다시 수행하려면 또 백그라운드 스레드로 전환해줄 필요가 있음.
                                                bloodPressureRepository.delete(bloodPressure);

                                                runOnUiThread(() -> { // 삭제된 의약품을 UI에도 반영
                                                    bloodContainer.removeView(pressureFrame);
                                                    Toast.makeText(BloodViewActivity.this, "삭제하였습니다!", Toast.LENGTH_SHORT).show();
                                                });
                                            }).start();
                                        })
                                        .setNegativeButton("취소", null)
                                        .show();
                            });
                        }
                    });

                }
            }

            runOnUiThread(() -> {
                for (View frame : bloodFrames) {
                    bloodContainer.addView(frame);
                }
            });

        }).start();
    }
}
