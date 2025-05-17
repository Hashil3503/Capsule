package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ViewPrescriptionActivity extends AppCompatActivity {

    private PrescriptionRepository prescriptionRepository;
    private MedicationRepository medicationRepository;
    private Prescription_ViewRepository prescription_viewRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_view_prescription);

        prescriptionRepository = new PrescriptionRepository(getApplication());
        medicationRepository = new MedicationRepository(getApplication());
        prescription_viewRepository = new Prescription_ViewRepository(getApplication());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        reload();

    }
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent resultIntent) {
        super.onActivityResult(requestCode, resultCode, resultIntent);

        if (requestCode == 1 && resultCode == RESULT_OK) { //requestCode는 여러개의 요청이 있을 경우, 이를 구분하기 위한 값. RESULT_OK은 작업이 정상적으로 완료되었음을 의미하는 변수.
            boolean prescriptionDeleted = resultIntent != null && resultIntent.getBooleanExtra("prescriptionDeleted", false);
            // prescriptionDeleted resultIntent의 값이 null아닌 경우 getBooleanExtra메서드로 전달받은 prescriptionDeleted의 값(의도대로 동작시 true를 전달함)이 된다. 만약, prescriptionDeleted의 값이 없으면 기본값을 false로 한다.
            if (prescriptionDeleted) {
                reload(); // DB에서 다시 불러와 UI 갱신
                Log.d("삭제결과", "처방전 삭제");
            }

            boolean medicationDeleted = resultIntent != null && resultIntent.getBooleanExtra("medicationDeleted", false);
            // medicationDeleted resultIntent의 값이 null아닌 경우 getBooleanExtra메서드로 전달받은 prescriptionDeleted의 값(의도대로 동작시 true를 전달함)이 된다. 만약, prescriptionDeleted의 값이 없으면 기본값을 false로 한다.
            if (medicationDeleted) {
                reload(); // DB에서 다시 불러와 UI 갱신
                Log.d("삭제결과", "의약품 삭제");
            }

        }
    }

    private void reload() {
        LinearLayout prescriptionContainer = findViewById(R.id.prescriptionContainer); //처방전 컨테이너 불러오기
        prescriptionContainer.removeAllViews();

        new Thread(() -> { //백그라운드 스레드 생성 (여기서 데이터베이스 관련 메서드가 작동해야함. 안그러면 앱 터짐.
            List<Prescription> prescriptions = prescriptionRepository.getAllPrescriptions();
            // 모든 처방전 목록 가져오기 (레파지터리에서 백그라운드에서 작동하도록 설정하지 않았기 때문에 new Thread에서 실행)
            List<Medication> allMedications = medicationRepository.getAllMedications();
            // 모든 의약품 목록 가져오기
            Map<Long, Medication> medicationMap = allMedications.stream().collect(Collectors.toMap(Medication::getId, Function.identity()));
            // 리스트 타입의 의약품 목록을 Map으로 변환. (쉽게 검색하기 위함)
            // .string()은 리스트를 스트림으로 변환하는 메서드. map(), filter(), collect()등의 메서드를 사용하기 위해 변환함.
            // .conllect()는 스트림을 리스트, 맵, 집합 등으로 변환하는 메서드. 위에서는 Collectors.toMap(Key, Value)를 통해 맵으로 변환함.
            // Function.identity()는 입력 값을 그대로 반환하는 메서드? Medication 객체 자체를 Value로 사용.
            // Medication::getId는 Medication.getId()의 메서드 참조(Method Reference) 표현. ( Medication::getId 와 m -> m.getId()는 동일 )

            List<View> prescriptionFrames = new ArrayList<>(); // 반복문에서 UI작업이 많이 일어나기 때문에 일괄처리를 위해 작업을 담아둘 리스트 생성.

            for (Prescription prescription : prescriptions) { //모든 처방전을 순회함. 향상된 for 루프? for(객체 : 리스트) 형식으로 사용하는 듯?
                //Prescription 객체이름 : 리스트이름  -> 반복할 때마다 리스트에 포함된 Prescription 객체를 가져옴. 콜론은 in을 의미. 해당 리스트의 모든 요소를 한번씩 가져옴.
                List<Prescription_View> prescriptionViews = prescription_viewRepository.getMedicationsForPrescription(prescription.getId());
                // 루프에서 현재 처방전과 관련된 의약품 목록을 조회

                StringBuilder medicationNames = new StringBuilder(); // StringBuilder는 가변적으로 문자열을 생성할 수 있는 클래스.
                for (Prescription_View pv : prescriptionViews) { //위에서 조회한 의약품 목록을 순회함. = 모든 처방전의 모든 의약품을 순회함.
                    Medication medication = medicationMap.get(pv.getMedication_id()); //Preicription_View의 getter 사용. Map이름.get(Key) 메서드로 검색.
                    //Preicription_View의 외래키 medication_id를 사용해 각 처방전에 포함된 개별 의약품을 medicationMap에서 조회함. medicationMap은 Medication 객체 리스트를 미리 Map으로 변환한 것.

                    if (medication != null) { // medication이 조회된 경우에만 수행
                        if (medicationNames.length() > 0) { // 이미 의약품 목록에 내용이 있는 경우에만 쉼표 추가.
                            medicationNames.append(", ");
                        }
                        medicationNames.append(medication.getName()); //의약품 이름을 조회해 의약품 이름 문자열에 추가.
                    }
                }
                //View 객체를 사용하는 이유는 중첩 for문으로 UI갱신이 너무 잦아질 수 있기 때문에 View 객체와 리스트를 사용해서 UI 변경점만 만들어두고 for문을 빠져나와서 일괄처리하기 위함.
                View prescriptionFrame = LayoutInflater.from(ViewPrescriptionActivity.this).inflate(R.layout.prescription_frame, null);

                TextView prescriptionIdTextView = prescriptionFrame.findViewById(R.id.prescriptionId);
                TextView prescriptionDateTextView = prescriptionFrame.findViewById(R.id.prescriptionDate);
                TextView medicationsTextView = prescriptionFrame.findViewById(R.id.medications);
                TextView prescriptionDurationTextView = prescriptionFrame.findViewById(R.id.prescriptionDuration);
                Button detailButton = prescriptionFrame.findViewById(R.id.detail);
                Button alarmButton = prescriptionFrame.findViewById(R.id.alarm);

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 MM월 dd일 (E)", Locale.KOREAN); // prescriptionDateTextView에 날짜 형식을 나타내기 전에 내가 원하는 포맷으로 설정.

                prescriptionIdTextView.setText("처방전 ID : " + prescription.getId());
                prescriptionDateTextView.setText("등록 일자 : " + sdf.format(prescription.getDate()));
                medicationsTextView.setText("의약품 : " + medicationNames.toString());
                prescriptionDurationTextView.setText("복용일수 : " + prescription.getDuration());

                prescriptionFrames.add(prescriptionFrame); // 나중에 한 번에 UI 추가

                detailButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(ViewPrescriptionActivity.this, ViewDetailActivity.class);
                        intent.putExtra("prescription_id", prescription.getId()); // 선택한 처방전 ID 전달
                        startActivityForResult(intent, 1); //ViewDetail 액티비티에서 처방전이 삭제되는 경우, 그 결과를 전달받기 위해 사용.
                    }
                });

                alarmButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(ViewPrescriptionActivity.this, AddAlarmActivity.class);
                        intent.putExtra("prescription_id", prescription.getId()); // 선택한 처방전 ID 전달
                        startActivity(intent);
                    }
                });
            }

            runOnUiThread(() -> {
                for (View frame : prescriptionFrames) {
                    prescriptionContainer.addView(frame);
                } // prescriptionFrame 뷰 객체들이 prescriptionFrames 리스트에 저장되었다가 현재 반복문에서 리스트를 순회하며 각 개체들을 prescriptionContainer에 추가하여 UI를 생성함.
            });
        }).start();
    }


}