package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import android.os.Handler;
import android.os.Looper;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private MedicineTableRepository medicineTableRepository;
    private DatabaseHelper dbHelper;

    private PrescriptionRepository prescriptionRepository;

    private MedicationRepository medicationRepository;

    private Prescription_ViewRepository prescription_viewRepository;

    private MedicineNameRepository medicineNameRepository;

    private List<Medication> medications = new ArrayList<>(); // 의약품 정보 db 를 위한 리스트

    private List<MedicineName> nameListCheck = new ArrayList<>(); // 기존 의약품 이름 목록 확인을 위한 리스트

    // 의약품 이름 목록 추가가 제대로 되었는지 확인하기 위한 리스트들
    private List<MedicineName> nameList = new ArrayList<>();
    private List<String> medicineNames = new ArrayList<>();

    private FrameLayout loadingOverlay;

    private LinearLayout buttonOCR, buttonViewPrescription, buttonBloodMenu, buttonAllAlarmList, buttonChatBot;

    private MedicationSliderAdapter adapter; //의약품 슬라이더에 사용할 어댑터

    private boolean DB1OK, DB2OK; //데이터베이스 로딩 끝났는지 판별을 위한 불린 자료형
    
    private int infocount = 0; //의약품 정보 몇개 저장했는지 확인용


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // DatabaseHelper 초기화
        dbHelper = DatabaseHelper.getInstance(getApplicationContext());

        medicineTableRepository = new MedicineTableRepository(getApplication());

        prescription_viewRepository = new Prescription_ViewRepository(getApplication());

        prescriptionRepository = new PrescriptionRepository(getApplication());

        medicationRepository = new MedicationRepository(getApplication());

        medicineNameRepository = new MedicineNameRepository(getApplication());

        DB1OK = false;
        DB2OK = false;

        // 약품 정보 데이터베이스 초기화
        initializeMedicineDatabase();
        // 자동완성을 위한 의약품 이름 목록 데이터베이스 초기화
        MakeNameList();

        ViewPager2 medicationSlider = findViewById(R.id.medicationSlider);
        medicationSlider.setOffscreenPageLimit(3); //미리 로딩해둘 페이지 개수 3개로 지정

        reloadPage(); // 의약품 페이지 생성 메서드

        new Thread(() -> {
            // 자동 슬라이드 (5초마다 다음 페이지로 전환)
            Handler handler = new Handler(Looper.getMainLooper());
            Runnable autoSlideRunnable = new Runnable() {
                int currentPage = 0;

                @Override
                public void run() {
                    if (adapter.getItemCount() > 0) {
                        currentPage = (currentPage + 1) % adapter.getItemCount();
                        medicationSlider.setCurrentItem(currentPage, true);
                    }
                    handler.postDelayed(this, 5000); //5초
                }
            };
            handler.postDelayed(autoSlideRunnable, 5000);
        }).start();


        //UI 요소 찾기
        loadingOverlay = findViewById(R.id.loadingOverlay);
        buttonOCR = findViewById(R.id.button_OCR);
        buttonViewPrescription = findViewById(R.id.button_ViewPrescription);
        buttonBloodMenu = findViewById(R.id.button_BloodMenu);
        buttonAllAlarmList = findViewById(R.id.button_AllAlarmList);
        buttonChatBot = findViewById(R.id.button_ChatBot);

        loadingOverlay.setVisibility(View.VISIBLE); // 데이터베이스 초기화 작업 완료까지 보여줄 로딩 바

        buttonOCR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, OCRActivity.class);
                startActivity(intent);
            }
        });

        buttonViewPrescription.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ViewPrescriptionActivity.class);
                startActivity(intent);
            }
        });

        buttonBloodMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, BloodMenuActivity.class);
                startActivity(intent);
            }
        });

        buttonAllAlarmList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AllAlarmListActivity.class);
                startActivity(intent);
            }
        });

        buttonChatBot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ChatBotActivity.class);
                startActivity(intent);
            }
        });
    }

    public void onResume() {
        super.onResume();
        reloadPage();
    }

    private void reloadPage() {
        ViewPager2 medicationSlider = findViewById(R.id.medicationSlider);
        new Thread(() -> {
            // 복용 기간이 지나지 않은 처방전 중, 가장 오래된 처방전 조회
            Prescription oldest = prescriptionRepository.getOldestActivePrescription();
            medications.clear();
            if (oldest == null) {
                // 복용 중인 처방전이 없는 경우, 표시될 페이지를 위한 가짜 medications 리스트
                Medication dummy = new Medication();
                dummy.setName("복용 중인 처방전이 없습니다");
                dummy.setEffects("");
                medications.add(dummy);
            }
            else {
                List<Prescription_View> prescriptionViews = prescription_viewRepository.getMedicationsForPrescription(oldest.getId());
                for (Prescription_View pv : prescriptionViews) {
                    Medication medication = medicationRepository.getMedicationById(pv.getMedication_id());
                    if (medication != null) {
                        medications.add(medication);
                    }
                }
            }
            runOnUiThread(() -> {
                adapter = new MedicationSliderAdapter(medications);
                medicationSlider.setAdapter(adapter);
            });
        }).start();
    }


    public void onBackPressed() {
        super.onBackPressed();
        finishAffinity(); // 모든 액티비티 종료
    }

    private void initializeMedicineDatabase() {

        medicineTableRepository = new MedicineTableRepository(getApplication());

        new Thread(() -> {
            try {
                List<MedicineTable> existingData = medicineTableRepository.getAllMedicineTables();
                if (existingData != null && !existingData.isEmpty()) {
                    Log.i(TAG, "약품 데이터가 이미 존재합니다. 초기화 스킵");
                    DB1OK = true;
                    checkLoadingDone();
                    runOnUiThread(() -> Toast.makeText(this, "데이터베이스 준비 완료", Toast.LENGTH_SHORT).show());
                    return; // 데이터가 존재하면 초기화 안 함
                }

//                if (existingData != null && !existingData.isEmpty()) {
//                    medicineTableRepository.deleteAllMedicineTables();
//                    Log.i(TAG, "약품 데이터 삭제");
//
//                }
                //데이터베이스 업데이트에만 사용


                String fileName = "medicine_info/약품 정보.txt";
                InputStream inputStream = getAssets().open(fileName);

                if (inputStream == null) {
                    Log.e(TAG, "약품 정보 파일을 열 수 없습니다: " + fileName);
                    runOnUiThread(() -> Toast.makeText(this, "약품 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show());
                    return;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                String currentMedicine = null;
                StringBuilder ingredient = new StringBuilder();
                StringBuilder effect = new StringBuilder();
                StringBuilder form = new StringBuilder();
                StringBuilder precaution = new StringBuilder();
                String currentSection = "";

                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;

                    if (line.startsWith("@")) { //의약품 정보 태그일 경우
                        currentSection = line.substring(1).trim().replace(":", "");
                    } else if (line.startsWith("#")) { //의약품 이름일 경우
                        // 이전 약품 저장
                        if (currentMedicine != null) {
                            saveMedicineInfo(currentMedicine, ingredient.toString().trim(), effect.toString().trim(), form.toString().trim(), precaution.toString().trim());
                        }
                        currentMedicine = line.substring(1).trim();
                        ingredient = new StringBuilder();
                        effect = new StringBuilder();
                        form = new StringBuilder();
                        precaution = new StringBuilder();
                        currentSection = "";
                    } else {
                        switch (currentSection) {
                            case "성분":
                                ingredient.append(line).append("\n");
                                break;
                            case "효과":
                                effect.append(line).append("\n");
                                break;
                            case "제형":
                                form.append(line).append("\n");
                                break;
                            case "주의사항":
                                precaution.append(line).append("\n");
                                break;
                        }
                    }
                }

                // 마지막 약품 정보 저장
                if (currentMedicine != null) {
                    saveMedicineInfo(currentMedicine,
                        ingredient.toString().trim(),
                        effect.toString().trim(),
                        form.toString().trim(),
                        precaution.toString().trim());
                }

                Log.i(TAG, "약품 정보 데이터베이스 초기화 완료");
                runOnUiThread(() -> Toast.makeText(this, "약품 정보 데이터베이스가 생성되었습니다.", Toast.LENGTH_SHORT).show());
                DB1OK = true;
                checkLoadingDone();

            } catch (IOException e) {
                Log.e(TAG, "약품 정보 파일 로드 실패", e);
                runOnUiThread(() -> Toast.makeText(this, "약품 정보 데이터베이스 초기화에 실패했습니다.", Toast.LENGTH_SHORT).show());
                finish();
            }
        }).start();
    }

    private void saveMedicineInfo(String medicineName, String ingredient, String effect, String form, String precaution) {
        if (medicineTableRepository.getMedicationByName(medicineName) != null) {
            Log.d(TAG, "중복된 약품 발견: " + medicineName + ", 저장 생략.");
            return;  // 중복된 약품명이 있으면 삽입하지 않고 종료
        }

        MedicineTable medicine = new MedicineTable();
        medicine.setName(medicineName);
        medicine.setIngredients(ingredient);
        medicine.setEffects(effect);
        medicine.setAppearance(form);
        medicine.setCaution(precaution);
        medicine.setMemo("");
        medicine.setSideeffct("");
        medicine.setSe_existence(false);

        medicineTableRepository.insert(medicine);
        infocount++;
        Log.d(TAG, "약품 정보 저장 완료: " + medicineName + " 총 " + infocount + "개");
//        Log.d(TAG, "=== 저장된 약품 정보 ===");
//        Log.d(TAG, "이름: " + medicineName);
//        Log.d(TAG, "성분: " + ingredient);
//        Log.d(TAG, "효과: " + effect);
//        Log.d(TAG, "제형: " + form);
//        Log.d(TAG, "주의사항: " + precaution);
//        Log.d(TAG, "총 저장된 수: " + infocount);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
            dbHelper = null;
        }
    }

    private void MakeNameList() {
        new Thread(() -> {
            nameListCheck = medicineNameRepository.getAllMedicineNames();
            if(nameListCheck != null && !nameListCheck.isEmpty()){
                Log.d(TAG, "자동완성 데이터베이스가 이미 생성됨.");
                runOnUiThread(() -> Toast.makeText(this, "데이터베이스2 준비 완료", Toast.LENGTH_SHORT).show());
                DB2OK = true;
                checkLoadingDone();
                return;
            }
            else {
                try {
                    String fileName = "건강보험심사평가원_ATC코드 매핑 목록_20240630.csv";
                    InputStream inputStream = getAssets().open(fileName);

                    if (inputStream == null) {
                        Log.e(TAG, "CSV 파일을 열 수 없습니다: " + fileName);
                        runOnUiThread(() -> Toast.makeText(this, "의약품 데이터를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show());
                        return;
                    }

                    // CSV 파일을 DataTable로 변환
                    List<Map<String, String>> dataTable = convertCsvToDataTable(inputStream);
                    Log.d(TAG, "CSV 파일 로드 완료: " + dataTable.size() + "개의 행");

                    if (dataTable.isEmpty()) {
                        Log.e(TAG, "CSV 파일에 데이터가 없습니다.");
                        runOnUiThread(() -> Toast.makeText(this, "의약품 데이터가 비어있습니다.", Toast.LENGTH_SHORT).show());
                        return;
                    }

                    // '제품명' 열에 해당하는 목록을 추출
                    List<String> productNames = extractProductNames(dataTable);
                    Log.d(TAG, "제품명 추출 완료: " + productNames.size() + "개의 제품명");

                    if (productNames.isEmpty()) {
                        Log.e(TAG, "추출된 제품명이 없습니다.");
                        runOnUiThread(() -> Toast.makeText(this, "제품명을 추출할 수 없습니다.", Toast.LENGTH_SHORT).show());
                        return;
                    }

                    medicineNameRepository.insertAll(productNames);
                    Log.d(TAG, "데이터베이스 생성 완료");
                    runOnUiThread(() -> Toast.makeText(this, "자동완성 데이터베이스가 생성되었습니다.", Toast.LENGTH_SHORT).show());
                    DB2OK = true;
                    checkLoadingDone();

                    // 데이터베이스에 내용이 제대로 추가되었는지 확인하기 위한 디버깅용 코드
                    nameList = medicineNameRepository.getAllMedicineNames();
                    for (MedicineName name : nameList) {
                        medicineNames.add(name.getName());
                    }

                    Log.i(TAG, String.format("의약품 데이터베이스 로드 완료: %d개의 제품명", medicineNames.size()));
                    // 로드된 약품명 샘플 출력
                    int sampleCount = Math.min(10, medicineNames.size());
                    Log.d(TAG, "로드된 약품명 샘플:");
                    for (int i = 0; i < sampleCount; i++) {
                        Log.d(TAG, "- " + medicineNames.get(i));

                    }

                } catch (IOException e) {
                    Log.e(TAG, "CSV 파일 로드 실패", e);
                    runOnUiThread(() -> Toast.makeText(this, "의약품 데이터베이스 로드에 실패했습니다.", Toast.LENGTH_SHORT).show());
                    finish();
                } catch (Exception e) {
                    Log.e(TAG, "데이터베이스 처리 중 오류 발생", e);
                    runOnUiThread(() -> Toast.makeText(this, "데이터베이스 처리 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show());
                    finish();
                }

            }
        }).start();
    }

    private List<Map<String, String>> convertCsvToDataTable(InputStream inputStream) throws IOException {
        List<Map<String, String>> dataTable = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "EUC-KR"));

        // 헤더(열 이름) 읽기
        String headerLine = reader.readLine();
        if (headerLine == null) {
            reader.close();
            return dataTable;
        }

        // 쉼표로 구분하되, 따옴표 안의 쉼표는 무시
        String[] headers = headerLine.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
        for (int i = 0; i < headers.length; i++) {
            headers[i] = headers[i].trim().replaceAll("\"", "");
        }

        Log.d(TAG, "CSV 헤더: " + Arrays.toString(headers));

        // 데이터 행 읽기
        String line;
        int rowCount = 0;
        while ((line = reader.readLine()) != null) {
            String[] values = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
            Map<String, String> row = new HashMap<>();

            // 각 열의 값을 매핑
            for (int i = 0; i < Math.min(headers.length, values.length); i++) {
                row.put(headers[i], values[i].trim().replaceAll("\"", ""));
            }

            dataTable.add(row);
            rowCount++;

            if (rowCount % 1000 == 0) {
                Log.d(TAG, "CSV 파일 처리 중: " + rowCount + "행");
            }
        }

        reader.close();
        Log.d(TAG, "CSV 파일 처리 완료: 총 " + rowCount + "행");
        return dataTable;
    }
    private List<String> extractProductNames(List<Map<String, String>> dataTable) {
        List<String> productNames = new ArrayList<>();

        for (Map<String, String> row : dataTable) {
            // '제품명' 열의 값을 찾아 리스트에 추가
            String productName = row.get("제품명");
            if (productName != null && !productName.trim().isEmpty()) {
                productNames.add(productName.trim());
            }
        }

        return productNames;
    }

    private void checkLoadingDone() { //두 데이터베이스가 모두 초기화 되었는지 체크하기 위한 메서드
        if (DB1OK && DB2OK) {
            runOnUiThread(() -> {
                loadingOverlay.setVisibility(View.GONE);
            });
        }
    }

}
