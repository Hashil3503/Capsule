package com.example.myapplication;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AddPrescriptionActivity extends AppCompatActivity {

    private static final String TAG = "AddPrescriptionActivity";
    //SQL 쿼리를 위한 각 엔티티의 레파지터리 변수 선언
    private PrescriptionRepository prescriptionRepository;
    private MedicationRepository medicationRepository;
    private Prescription_ViewRepository prescription_viewRepository;
    private MedicineTableRepository medicineTableRepository;
    private MedicineNameRepository medicineNameRepository;
    private List<String> medicineNames = new ArrayList<>(); // 자동완성을 위한 리스트
    private List<MedicineName> nameList = new ArrayList<>(); // 자동완성을 위한 리스트 2
    private List<String> ocrMedicineNames = new ArrayList<>(); //ocr에서 추출한 의약품 이름을 담아두는 리스트

    Calendar regDate;

    private ArrayAdapter<String> adapter; //전역 어댑터 선언

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_prescription);
        // 레파지터리 객체 생성 및 초기화
        prescriptionRepository = new PrescriptionRepository(getApplication());
        medicationRepository = new MedicationRepository(getApplication());
        prescription_viewRepository = new Prescription_ViewRepository(getApplication());
        medicineTableRepository = new MedicineTableRepository(getApplication());
        medicineNameRepository = new MedicineNameRepository(getApplication());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        new Thread(() -> {
            nameList = medicineNameRepository.getAllMedicineNames(); //의약품 이름 목록을 조회해서 medicineNames 문자열 리스트에 추가.

            runOnUiThread(() -> {
                for (MedicineName name : nameList) {
                    medicineNames.add(name.getName());
                }
                // 자동완성을 위한 어댑터 선언
                adapter = new ArrayAdapter<>(
                        AddPrescriptionActivity.this,
                        android.R.layout.simple_dropdown_item_1line,
                        medicineNames
                );
            });
        }).start();

        LinearLayout medicationContainer = findViewById(R.id.medicationContainer); //의약품 컨테이너 불러오기



        // Result 액티비티에서 넘어온 경우, 의약품 개수 만큼 동적으로 의약품 프레임 생성 후, UI 초기화
        ocrMedicineNames  = getIntent().getStringArrayListExtra("medicine_names"); //Result 액티비티에서 OCR 추출한 의약품 목록 받아오기
        if (ocrMedicineNames  != null && !ocrMedicineNames .isEmpty()) {
            for (String medicineName : ocrMedicineNames ) {

                String norName = CommonMethod.normalizeWord(medicineName); //DB 조회를 위한 의약품 이름 정규화

                new Thread(() -> {
                    MedicineTable medicineTable = medicineTableRepository.getMedicationByName(norName);

                    runOnUiThread(() -> {
                        LayoutInflater inflater = LayoutInflater.from(AddPrescriptionActivity.this); //현재 Activity의 Context를 기반으로 LayoutInflater 객체를 생성.
                        View medicationFrame = inflater.inflate(R.layout.medication_frame, medicationContainer, false); // medication_frame을 메모리에 로드해서 자바에서 사용가능한 View 객체로 변환함.
                        medicationContainer.addView(medicationFrame);

                        AutoCompleteTextView nameEditText = medicationFrame.findViewById(R.id.editmedicationName);
                        nameEditText.setAdapter(adapter);
                        nameEditText.setThreshold(1);
                        nameEditText.addTextChangedListener(new TextWatcher() {
                            @Override
                            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                                // 입력 전 상태
                            }

                            @Override
                            public void onTextChanged(CharSequence s, int start, int before, int count) {
                                // 입력 중 상태
                            }

                            @Override
                            public void afterTextChanged(Editable s) {
                                // 입력 완료 후 상태
                                String inputName = s.toString().trim();
                                String normalized = CommonMethod.normalizeWord(inputName);

                                new Thread(() -> {
                                    MedicineTable updatedMedicine = medicineTableRepository.getMedicationByName(normalized);
                                    runOnUiThread(() -> {
                                        TextView viewmedicationIngredients = medicationFrame.findViewById(R.id.viewmedicationIngredients);
                                        TextView viewmedicationAppearance = medicationFrame.findViewById(R.id.viewmedicationAppearance);
                                        TextView viewmedicationEffects = medicationFrame.findViewById(R.id.viewmedicationEffects);
                                        TextView viewmedicationCaution = medicationFrame.findViewById(R.id.viewmedicationCaution);

                                        if (updatedMedicine != null) {
                                            viewmedicationIngredients.setText(updatedMedicine.getIngredients());
                                            viewmedicationAppearance.setText(updatedMedicine.getAppearance());
                                            viewmedicationEffects.setText(updatedMedicine.getEffects());
                                            viewmedicationCaution.setText(updatedMedicine.getCaution());
                                        } else {
                                            // 찾는 약품 없을 때는 모두 초기화
                                            viewmedicationIngredients.setText("");
                                            viewmedicationAppearance.setText("");
                                            viewmedicationEffects.setText("");
                                            viewmedicationCaution.setText("");
                                        }
                                    });
                                }).start();
                            }
                        });

                        TextView viewmedicationIngredients = medicationFrame.findViewById(R.id.viewmedicationIngredients);
                        TextView viewmedicationAppearance = medicationFrame.findViewById(R.id.viewmedicationAppearance);
                        TextView viewmedicationEffects = medicationFrame.findViewById(R.id.viewmedicationEffects);
                        TextView viewmedicationCaution = medicationFrame.findViewById(R.id.viewmedicationCaution);

                        if (medicineTable != null) {
                            nameEditText.setText(medicineName);
                            viewmedicationIngredients.setText(medicineTable.getIngredients());
                            viewmedicationAppearance.setText(medicineTable.getAppearance());
                            viewmedicationEffects.setText(medicineTable.getEffects());
                            viewmedicationCaution.setText(medicineTable.getCaution());
                        }
                    });
                }).start();

//                // 스크롤이 자동으로 아래로 이동하도록 설정 (이제 필요 없음)
//                final ScrollView scrollView = findViewById(R.id.scrollView); // 스크롤뷰 불러오기
//                scrollView.post(new Runnable() { // UI 업데이트시 동작
//                    @Override
//                    public void run() {
//                        scrollView.fullScroll(ScrollView.FOCUS_DOWN); //화면을 아래로 스크롤
//                    }
//                });
            }
        }

        // 의약품 프레임 동적 생성 버튼
        Button medicationAdd = findViewById(R.id.button_medicationAdd); //버튼 불러오기

        medicationAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { //버튼 누르면 새로운 프레임 생성
                LayoutInflater inflater = LayoutInflater.from(AddPrescriptionActivity.this); //현재 Activity의 Context를 기반으로 LayoutInflater 객체를 생성.
                View medicationFrame = inflater.inflate(R.layout.medication_frame, medicationContainer, false); // medication_frame을 메모리에 로드해서 자바에서 사용가능한 View 객체로 변환함.

                medicationContainer.addView(medicationFrame); //로드한 medication_frame을 medicationContainer에 추가
                //위 메서드들 동작 원리 : 현재 액티비티의 context를 기반으로 LayoutInflater 객체 생성. -> 다른 레이아웃을 inflater 객체로 로드해서 view 객체로 변환 -> 새로만든 view 객체를 현재 레이아웃에 추가.


                AutoCompleteTextView nameEditText = medicationFrame.findViewById(R.id.editmedicationName);
                nameEditText.setAdapter(adapter); //어댑터 연결
                nameEditText.setThreshold(1); // 1글자 입력 시 자동완성 시작
                nameEditText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                        // 입력 전 상태
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        // 입력 중 상태
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        // 입력 완료 후 상태
                        String inputName = s.toString().trim();
                        String normalized = CommonMethod.normalizeWord(inputName);

                        new Thread(() -> {
                            MedicineTable updatedMedicine = medicineTableRepository.getMedicationByName(normalized);
                            runOnUiThread(() -> {
                                TextView viewmedicationIngredients = medicationFrame.findViewById(R.id.viewmedicationIngredients);
                                TextView viewmedicationAppearance = medicationFrame.findViewById(R.id.viewmedicationAppearance);
                                TextView viewmedicationEffects = medicationFrame.findViewById(R.id.viewmedicationEffects);
                                TextView viewmedicationCaution = medicationFrame.findViewById(R.id.viewmedicationCaution);

                                if (updatedMedicine != null) {
                                    viewmedicationIngredients.setText(updatedMedicine.getIngredients());
                                    viewmedicationAppearance.setText(updatedMedicine.getAppearance());
                                    viewmedicationEffects.setText(updatedMedicine.getEffects());
                                    viewmedicationCaution.setText(updatedMedicine.getCaution());
                                } else {
                                    // 찾는 약품 없을 때는 모두 초기화
                                    viewmedicationIngredients.setText("");
                                    viewmedicationAppearance.setText("");
                                    viewmedicationEffects.setText("");
                                    viewmedicationCaution.setText("");
                                }
                            });
                        }).start();
                    }
                });


                // 스크롤이 자동으로 아래로 이동하도록 설정
                final ScrollView scrollView = findViewById(R.id.scrollView); // 스크롤뷰 불러오기
                scrollView.post(new Runnable() { // UI 업데이트시 동작
                    @Override
                    public void run() {
                        scrollView.fullScroll(ScrollView.FOCUS_DOWN); //화면을 아래로 스크롤
                    }
                });
            }
        });

        // 처방전 등록 일자 설정
        regDate = Calendar.getInstance(); //현재 액티비티가 생성되는 시점을 기준으로 날짜/시간이 정해짐.
        Button regDateButton = findViewById(R.id.editRegDate); // 테스트용 기능. 처방전 입력 일자를 수정할 수 있는 기능.
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREAN);
        regDateButton.setText("등록 일자 :" + sdf.format(regDate.getTime()));
        regDateButton.setOnClickListener(v -> showDatePicker(regDate, regDateButton, "등록 일자: "));

        //UI기반 데이터베이스 쿼리
        Button prescriptionRegister = findViewById(R.id.button_prescriptionRegister);
        prescriptionRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 현재 날짜를 기반으로 Prescription 객체 생성

                EditText durationEditText = findViewById(R.id.editDuration); //복용 일수를 처방전에 저장하기 위해 값 가져오기
                String durationStr = durationEditText.getText().toString().trim();
                int duration = CommonMethod.parseInteger(durationStr);

                if (durationStr.isEmpty() || duration <= 0) { //복용 일수가 없거나 0 이하면 처방전 저장하지 않음.
                    Toast.makeText(AddPrescriptionActivity.this, "복용 일수를 입력해 주세요", Toast.LENGTH_SHORT).show();
                    return;
                }

                // medicationContainer 내부의 모든 프레임 찾기
                LinearLayout medicationContainer = findViewById(R.id.medicationContainer);
                int count = medicationContainer.getChildCount(); //동적으로 생성된 medication_frame의 갯수

                List<Medication> validMedications = new ArrayList<>(); //이름이 비어있지 않은 의약품만 필터링 하기 위한 리스트

                for (int i = 0; i < count; i++) {
                    View frame = medicationContainer.getChildAt(i);
                    AutoCompleteTextView nameEditText = frame.findViewById(R.id.editmedicationName);
                    EditText dosageEditText = frame.findViewById(R.id.editmedicationDosage);
                    EditText frequencyEditText = frame.findViewById(R.id.editmedicationFrequency);
                    TextView viewmedicationIngredients = frame.findViewById(R.id.viewmedicationIngredients);
                    TextView viewmedicationAppearance = frame.findViewById(R.id.viewmedicationAppearance);
                    TextView viewmedicationEffects = frame.findViewById(R.id.viewmedicationEffects);
                    TextView viewmedicationCaution = frame.findViewById(R.id.viewmedicationCaution);

                    String name = nameEditText.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(AddPrescriptionActivity.this, "빈 의약품 이름이 있습니다", Toast.LENGTH_SHORT).show();
                        return;  //의약품 이름이 비면 반복문 자체를 종료?
                    }

                    int dosage = CommonMethod.parseInteger(dosageEditText.getText().toString().trim());
                    int frequency = CommonMethod.parseInteger(frequencyEditText.getText().toString().trim());
                    String ingredients = viewmedicationIngredients.getText().toString();
                    String appearance = viewmedicationAppearance.getText().toString();
                    String effects = viewmedicationEffects.getText().toString();
                    String caution = viewmedicationCaution.getText().toString();

                    Medication medication = new Medication();
                    medication.setName(name);
                    medication.setDosage(dosage);
                    medication.setFrequency(frequency);
                    medication.setIngredients(ingredients);
                    medication.setAppearance(appearance);
                    medication.setEffects(effects);
                    medication.setCaution(caution);
                    medication.setMemo(null);
                    medication.setSideeffct(null);
                    medication.setSe_existence(false);

                    validMedications.add(medication); // 이름이 있는 약만 리스트에 추가
                }

                // 위 두 검사를 통과하면 처방전과 의약품 정보 저장.
                Date selectedDate = regDate.getTime();
                Prescription prescription = new Prescription(selectedDate);
                prescription.setDuration(duration);

                long prescriptionId = prescriptionRepository.insert(prescription);

                for (Medication med : validMedications) {
                    long medicationId = medicationRepository.insert(med);
                    Prescription_View relation = new Prescription_View((int) prescriptionId, (int) medicationId);
                    prescription_viewRepository.insert(relation);
                }
                // UI 스레드에서 실행해야 할 작업만 남김
                // runOnUiThread(() -> { ... }); 을 사용하지 않는 이유는 레파지터리를 통해 쿼리 작업만 백그라운드에서 동작하도록 했기 때문.
                // 레파치터리가 없었으면 위에 쿼리 관련 코드들은 전부 new Thread()를 통해 새로운 스레드에서 작업해야함.
                // 새로운 스레드는 백그라운드에서 동작해야 쿼리 작업중 앱이 멈춘는걸 방지할 수 있는데, 백그라운드 스레드에서 Toast를 사용하면 크래시 뜸.
                // Toast는 일시적인 메시지를 띄워주는 기능을 수행하는데 UI관련 메서드이기 때문에 반드시 메인스레드(포어그라운드)에서 작동해야함.
                Toast.makeText(AddPrescriptionActivity.this, "처방전이 등록되었습니다!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(AddPrescriptionActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void showDatePicker(Calendar target, Button button, String prefix) {
        int year = target.get(Calendar.YEAR);
        int month = target.get(Calendar.MONTH);
        int day = target.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog = new DatePickerDialog(this, (view, y, m, d) -> {
            target.set(y, m, d);
            button.setText(prefix + y + "-" + (m + 1) + "-" + d);
        }, year, month, day);

        dialog.show();
    }
    private String normalizeMedicineName(String name) {
        if (name == null) return "";

        // 1. 기본 정규화: 앞뒤 공백 제거
        name = name.trim();

        // 2. 괄호와 그 내용 제거
        name = name.replaceAll("\\(.*?\\)", "").trim();

        // 3. 언더스코어(_)와 그 이후 내용 제거
        name = name.replaceAll("_.*$", "").trim();

        // 4. 추가 공백 제거
        name = name.replaceAll("\\s+", " ").trim();

        // 5. 숫자 이후의 문자열 모두 제거
        name = name.replaceAll("\\d.*$", "").trim();

        // 6. 특수문자 제거 (한글, 영문만 남김)
        name = name.replaceAll("[^가-힣a-zA-Z]", "").trim();

        // 7. 마지막 언더스코어 제거
        name = name.replaceAll("_$", "").trim();

        Log.d(TAG, "정규화된 약품명: " + name);
        return name;
    }

}




