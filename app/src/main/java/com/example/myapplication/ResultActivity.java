package com.example.myapplication;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.AsyncTask;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.app.AlertDialog;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.content.ContentResolver;
import android.os.Build;
import androidx.exifinterface.media.ExifInterface;
import android.webkit.MimeTypeMap;

import androidx.appcompat.app.AppCompatActivity;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.tasks.Task;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class ResultActivity extends AppCompatActivity {
    private static final String TAG = "ResultActivity";
    private ImageView imageView;
    private TextView resultTextView;
    private Button btnConfirm;
    private ProgressBar progressBar;
    private Bitmap bitmap;
    private Map<String, String> structuredData;
    private ArrayList<String> extractedMedicines = new ArrayList<>();
    private Uri imageUri;
    private Set<String> medicineDatabase = new HashSet<>();
    private static final float SIMILARITY_THRESHOLD = 0.2f;
    private static final float HIGH_SIMILARITY = 0.5f;
    private static final float MEDIUM_SIMILARITY = 0.3f;
    private DatabaseHelper dbHelper;
    private AlertDialog currentDialog;
    private TextView currentListView;
    private int currentPage = 0;
    private List<String> recognizedMedicines;
    private List<String> modifiedMedicines;

    private ArrayAdapter<String> adapter;

    private MedicineNameRepository medicineNameRepository;

    private List<String> medicineNames = new ArrayList<>(); // 자동완성을 위한 리스트
    private List<MedicineName> nameList = new ArrayList<>(); // 자동완성을 위한 리스트 2

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 윈도우 플래그 설정
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        setContentView(R.layout.activity_result);

        medicineNameRepository = new MedicineNameRepository(getApplication());

        new Thread(() -> {
            nameList = medicineNameRepository.getAllMedicineNames(); //의약품 이름 목록을 조회해서 medicineNames 문자열 리스트에 추가.

            runOnUiThread(() -> {
                for (MedicineName name : nameList) {
                    medicineNames.add(name.getName());
                }
                // 자동완성을 위한 어댑터 선언
                adapter = new ArrayAdapter<>(
                        ResultActivity.this,
                        android.R.layout.simple_dropdown_item_1line,
                        medicineNames
                );
            });
        }).start();

        // OCR 결과와 수정된 약품명 리스트 초기화
        recognizedMedicines = new ArrayList<>();
        modifiedMedicines = new ArrayList<>();

        initializeViews();
        setupButtons();


        // 이미지 처리
        String imageUriString = getIntent().getStringExtra("imageUri");
        if (imageUriString != null && !imageUriString.isEmpty()) {
            imageUri = Uri.parse(imageUriString);
            Log.d(TAG, "이미지 URI: " + imageUri.toString());
            processImage(imageUri);
        } else {
            Log.e(TAG, "이미지 URI가 없습니다.");
            Toast.makeText(this, "이미지 URI가 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        // 다이얼로그가 표시되어 있다면 닫기
        if (currentDialog != null && currentDialog.isShowing()) {
            currentDialog.dismiss();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // 비트맵 리소스 해제
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
            bitmap = null;
        }
        if (imageView != null) {
            imageView.setImageBitmap(null);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 실행 중인 AsyncTask 취소
        if (getWindow().getCallback() instanceof AsyncTask) {
            ((AsyncTask) getWindow().getCallback()).cancel(true);
        }

        // 다이얼로그 닫기
        if (currentDialog != null && currentDialog.isShowing()) {
            currentDialog.dismiss();
            currentDialog = null;
        }

        // DatabaseHelper 정리
        if (dbHelper != null) {
            dbHelper.close();
            dbHelper = null;
        }

        // 이미지 리소스 정리
        cleanupImageResources();

        // 메모리 정리
        System.gc();
    }

    @Override
    public void finish() {
        // Surface 정리
        if (getWindow() != null && getWindow().getDecorView() != null) {
            getWindow().getDecorView().setVisibility(View.GONE);
        }

        // 이미지 리소스 정리
        cleanupImageResources();

        super.finish();
    }

    private void initializeViews() {
        resultTextView = findViewById(R.id.resultTextView);
        btnConfirm = findViewById(R.id.btnConfirm);
        imageView = findViewById(R.id.resultImageView);
        progressBar = findViewById(R.id.progressBar);
        structuredData = new HashMap<>();
    }

    private void setupButtons() {
        btnConfirm.setOnClickListener(v -> {
            if (!extractedMedicines.isEmpty()) {
                // AddPrescriptionActivity로 이동, 추출한 의약품 목록 전달.
                Intent intent = new Intent(this, AddPrescriptionActivity.class);
                intent.putStringArrayListExtra("medicine_names", extractedMedicines);
                startActivity(intent);
            } else {
                Toast.makeText(this, "인식된 약품이 없습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isKoreanValid(String text) {
        if (text == null || text.isEmpty()) return false;

        // 한글이 포함되어 있고 깨진 문자가 없는지 확인
        boolean hasKorean = text.matches(".*[가-힣]+.*");
        boolean hasInvalidChar = text.contains("") || text.contains("?") || text.contains("□") || text.contains("▯");

        if (hasKorean && !hasInvalidChar) {
            Log.d(TAG, "유효한 한글 텍스트: " + text);
            return true;
        } else {
            Log.w(TAG, String.format("유효하지 않은 텍스트: %s (한글포함: %b, 깨진문자: %b)",
                    text, hasKorean, hasInvalidChar));
            return false;
        }
    }

    private void processImage(Uri uri) {
        try {
            // URI 유효성 검사
            if (uri == null) {
                Log.e(TAG, "이미지 URI가 null입니다.");
                Toast.makeText(this, "이미지를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            Log.d(TAG, "이미지 URI 처리 시작: " + uri.toString());

            // 파일 접근 권한 및 유효성 확인
            try {
                InputStream inputStream = null;
                String mimeType = null;

                if (uri.getScheme() != null && uri.getScheme().equals("content")) {
                    // Content URI인 경우
                    mimeType = getContentResolver().getType(uri);
                    inputStream = getContentResolver().openInputStream(uri);
                } else if (uri.getScheme() != null && uri.getScheme().equals("file")) {
                    // File URI인 경우
                    String path = uri.getPath();
                    if (path != null) {
                        File file = new File(path);
                        if (!file.exists()) {
                            Log.e(TAG, "파일이 존재하지 않습니다: " + path);
                            Toast.makeText(this, "이미지 파일을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                        // 파일 확장자로 MIME 타입 추정
                        String extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
                        if (extension != null) {
                            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
                        }
                        inputStream = new FileInputStream(file);
                    }
                }

                // 스트림과 MIME 타입 확인
                if (inputStream == null) {
                    Log.e(TAG, "이미지 스트림을 열 수 없습니다: " + uri);
                    Toast.makeText(this, "이미지 파일을 열 수 없습니다.", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                if (mimeType == null || !mimeType.startsWith("image/")) {
                    Log.e(TAG, "유효하지 않은 이미지 형식입니다: " + uri);
                    Toast.makeText(this, "지원하지 않는 이미지 형식입니다.", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                // 스트림 닫기
                inputStream.close();

                // 이미지 로드 및 OCR 처리
                new LoadImageTask().execute(uri);

            } catch (SecurityException e) {
                Log.e(TAG, "이미지 파일 접근 권한이 없습니다: " + uri, e);
                Toast.makeText(this, "이미지 파일에 접근할 수 없습니다.", Toast.LENGTH_SHORT).show();
                finish();
            } catch (IOException e) {
                Log.e(TAG, "이미지 파일 읽기 오류: " + uri, e);
                Toast.makeText(this, "이미지 파일을 읽을 수 없습니다.", Toast.LENGTH_SHORT).show();
                finish();
            }
        } catch (Exception e) {
            Log.e(TAG, "이미지 처리 중 오류 발생: " + uri, e);
            Toast.makeText(this, "이미지 처리 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private class LoadImageTask extends AsyncTask<Uri, Void, Bitmap> {
        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            resultTextView.setText("이미지 처리 중...");
            btnConfirm.setEnabled(false);
        }

        @Override
        protected Bitmap doInBackground(Uri... uris) {
            try {
                Uri imageUri = uris[0];
                if (imageUri == null) {
                    Log.e(TAG, "이미지 URI가 null입니다.");
                    return null;
                }

                Log.d(TAG, "이미지 로드 시작: " + imageUri.toString());

                // ContentResolver를 통해 이미지 스트림 열기
                InputStream inputStream = null;
                Bitmap bitmap = null;

                try {
                    ContentResolver resolver = getContentResolver();
                    inputStream = resolver.openInputStream(imageUri);

                    if (inputStream == null) {
                        Log.e(TAG, "이미지 스트림을 열 수 없습니다.");
                        return null;
                    }

                    // 이미지 크기 확인
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeStream(inputStream, null, options);
                    inputStream.close();

                    // 메모리 제한을 고려한 샘플링 크기 계산
                    int maxSize = 2048;
                    options.inSampleSize = calculateInSampleSize(options, maxSize, maxSize);
                    options.inJustDecodeBounds = false;
                    options.inPreferredConfig = Bitmap.Config.ARGB_8888;

                    // 이미지 다시 로드
                    inputStream = resolver.openInputStream(imageUri);
                    bitmap = BitmapFactory.decodeStream(inputStream, null, options);

                    if (bitmap == null) {
                        Log.e(TAG, "이미지를 디코딩할 수 없습니다.");
                        return null;
                    }

                    Log.d(TAG, String.format("이미지 로드 완료: %dx%d", bitmap.getWidth(), bitmap.getHeight()));

                    // EXIF 정보 읽기 및 회전 처리
                    try {
                        inputStream.close();
                        inputStream = resolver.openInputStream(imageUri);

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            ExifInterface exif = new ExifInterface(inputStream);
                            int orientation = exif.getAttributeInt(
                                    ExifInterface.TAG_ORIENTATION,
                                    ExifInterface.ORIENTATION_NORMAL);

                            Matrix matrix = new Matrix();
                            switch (orientation) {
                                case ExifInterface.ORIENTATION_ROTATE_90:
                                    matrix.postRotate(90);
                                    break;
                                case ExifInterface.ORIENTATION_ROTATE_180:
                                    matrix.postRotate(180);
                                    break;
                                case ExifInterface.ORIENTATION_ROTATE_270:
                                    matrix.postRotate(270);
                                    break;
                            }

                            if (!matrix.isIdentity()) {
                                Bitmap rotatedBitmap = Bitmap.createBitmap(
                                        bitmap, 0, 0,
                                        bitmap.getWidth(), bitmap.getHeight(),
                                        matrix, true);
                                if (rotatedBitmap != bitmap) {
                                    bitmap.recycle();
                                    bitmap = rotatedBitmap;
                                }
                                Log.d(TAG, "이미지 회전 처리 완료");
                            }
                        }
                    } catch (IOException e) {
                        Log.w(TAG, "EXIF 정보를 읽을 수 없습니다.", e);
                    }

                    return bitmap;
                } finally {
                    try {
                        if (inputStream != null) {
                            inputStream.close();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "스트림 닫기 실패", e);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "이미지 로드 중 오류 발생", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (result == null) {
                Toast.makeText(ResultActivity.this, "이미지를 로드할 수 없습니다.", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            try {
                // 이전 비트맵 해제
                if (bitmap != null && !bitmap.isRecycled()) {
                    bitmap.recycle();
                }

                bitmap = result;
                imageView.setImageBitmap(bitmap);
                Log.d(TAG, "이미지 표시 완료");

                // OCR 작업 시작
                new OCRTask().execute();
            } catch (Exception e) {
                Log.e(TAG, "이미지 설정 중 오류 발생", e);
                Toast.makeText(ResultActivity.this, "이미지 처리 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    private class OCRTask extends AsyncTask<Void, Void, Text> {
        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            resultTextView.setText("텍스트 인식 중...");
        }

        @Override
        protected Text doInBackground(Void... voids) {
            try {
                if (bitmap == null || bitmap.isRecycled()) {
                    Log.e(TAG, "비트맵이 null이거나 recycled 상태입니다.");
                    return null;
                }

                // 이미지 전처리
                Bitmap processedBitmap = preprocessImage(bitmap);
                if (processedBitmap == null) {
                    Log.e(TAG, "이미지 전처리 실패");
                    return null;
                }

                // 한국어 OCR 옵션 사용
                InputImage image = InputImage.fromBitmap(processedBitmap, 0);
                TextRecognizer recognizer = TextRecognition.getClient(
                        new KoreanTextRecognizerOptions.Builder().build());

                // OCR 결과 대기
                Task<Text> result = recognizer.process(image);
                Text text = Tasks.await(result);

                // OCR 결과 로깅
                if (text != null) {
                    Log.d(TAG, "OCR 인식 결과:");
                    for (Text.TextBlock block : text.getTextBlocks()) {
                        Log.d(TAG, "블록: " + block.getText());
                    }
                } else {
                    Log.e(TAG, "OCR 결과가 null입니다.");
                }

                // 메모리 정리
                if (processedBitmap != bitmap && !processedBitmap.isRecycled()) {
                    processedBitmap.recycle();
                }

                return text;
            } catch (Exception e) {
                Log.e(TAG, "OCR 처리 중 오류 발생", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Text text) {
            progressBar.setVisibility(View.GONE);

            if (text == null) {
                Toast.makeText(ResultActivity.this, "텍스트 인식에 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // OCR 결과 처리
            new TextProcessingTask().execute(text);
        }
    }

    private Bitmap preprocessImage(Bitmap original) {
        try {
            if (original == null) {
                Log.e(TAG, "원본 이미지가 null입니다.");
                return null;
            }

            // 간단한 전처리만 수행
            Bitmap processedBitmap = Bitmap.createBitmap(original.getWidth(),
                    original.getHeight(), Bitmap.Config.RGB_565);
            Canvas canvas = new Canvas(processedBitmap);
            Paint paint = new Paint();

            // 명암 대비 조정
            float contrast = 1.2f;
            float brightness = -10f;
            ColorMatrix colorMatrix = new ColorMatrix(new float[] {
                    contrast, 0, 0, 0, brightness,
                    0, contrast, 0, 0, brightness,
                    0, 0, contrast, 0, brightness,
                    0, 0, 0, 1, 0
            });

            paint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
            canvas.drawBitmap(original, 0, 0, paint);

            return processedBitmap;
        } catch (Exception e) {
            Log.e(TAG, "이미지 전처리 중 오류 발생", e);
            return original;
        }
    }

    private class TextProcessingTask extends AsyncTask<Text, Void, List<String>> {
        private final Set<String> SECTION_KEYWORDS = new HashSet<>(Arrays.asList(
                "약품명", "약명", "처방약", "조제약", "처방내역", "조제내역"
        ));

        private final Set<String> MEDICINE_FORM_KEYWORDS = new HashSet<>(Arrays.asList(
                "정", "캡슐", "시럽", "주사", "액", "연고", "크림", "패치",
                "tab", "cap", "inj", "cream", "patch", "gel", "정제", "주사제",
                "캡슐제", "시럽제", "연고제", "크림제", "패치제", "가루", "산", "환",
                "물", "좌", "점안", "점이", "주", "알", "개", "통", "병"
        ));

        private final Set<String> UNIT_KEYWORDS = new HashSet<>(Arrays.asList(
                "mg", "mcg", "g", "ml", "cc", "정", "캡슐", "알", "개", "통", "병"
        ));

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            resultTextView.setText("약품명 분석 중...");
        }

        @Override
        protected List<String> doInBackground(Text... texts) {
            Text text = texts[0];
            List<String> recognizedMedicines = new ArrayList<>();

            if (text == null) {
                Log.e(TAG, "OCR 텍스트가 null입니다.");
                return recognizedMedicines;
            }

            // 모든 텍스트 블록 처리
            for (Text.TextBlock block : text.getTextBlocks()) {
                String blockText = block.getText().trim();
                Log.d(TAG, "처리 중인 블록: " + blockText);

                // 각 줄에서 약품명 추출 시도
                String[] lines = blockText.split("\n");
                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty()) continue;

                    // 약품 형태 키워드가 있는 경우에만 처리
                    for (String keyword : MEDICINE_FORM_KEYWORDS) {
                        if (line.toLowerCase().contains(keyword.toLowerCase())) {
                            // 약품명 추출 (형태 키워드 포함)
//                            String medicineName = extractMedicineName(line);
                            String medicineName = line;
                            if (!medicineName.isEmpty()) {
                                // 데이터베이스에서 매칭 검색
                                String matchedName = findBestMatch(medicineName);
                                if (matchedName != null && !recognizedMedicines.contains(matchedName)) {
                                    recognizedMedicines.add(matchedName);
                                    Log.d(TAG, "매칭된 약품명: " + matchedName);
                                }
                            }
                            break; // 한 줄에서 하나의 약품명만 추출
                        }
                    }
                }
            }

            return recognizedMedicines;
        }

        private String extractMedicineName(String line) { //필요 없는거 같아서 일단은 비활성화
            // 숫자와 단위 제거 (형태 키워드는 유지)
            String medicineName = line.replaceAll("\\d+\\s*(" + String.join("|", UNIT_KEYWORDS) + ")", "");
            medicineName = medicineName.replaceAll("\\d+", "");

            // 특수문자 제거 (형태 키워드는 유지)
            medicineName = medicineName.replaceAll("[^가-힣a-zA-Z0-9\\s]", "");

            // 공백 정리
            medicineName = medicineName.trim();

            // 너무 짧은 텍스트 제외
            if (medicineName.length() < 2) {
                return "";
            }

            Log.d(TAG, "추출된 약품명: " + medicineName);
            return medicineName;
        }

        @Override
        protected void onPostExecute(List<String> medicines) {
            extractedMedicines.clear();
            extractedMedicines.addAll(medicines);
            displayResults();
            progressBar.setVisibility(View.GONE);
            btnConfirm.setEnabled(true);
        }
    }

    private void performOCR(Uri uri) {
        new LoadImageTask().execute(uri);
    }

//    private String findBestMatch(String medicineName) {
//        if (medicineName == null || medicineName.isEmpty()) {
//            return null;
//        }
//
//        try {
//            String normalizedInput = normalizeString(medicineName);
//            String bestMatch = null;
//            float maxSimilarity = SIMILARITY_THRESHOLD;
//            List<String> similarMedicines = new ArrayList<>();
//
//            // 데이터베이스에서 직접 검색
//            List<String> matches = dbHelper.searchMedicines(normalizedInput);
//            if (!matches.isEmpty()) {
//                for (String dbMedicine : matches) {
//                    String normalizedDb = normalizeString(dbMedicine);
//                    float similarity = calculateSimilarity(normalizedInput, normalizedDb);
//
//                    if (similarity > maxSimilarity) {
//                        maxSimilarity = similarity;
//                        bestMatch = dbMedicine;
//                        Log.d(TAG, String.format("매칭 발견: %s (유사도: %.2f)", dbMedicine, similarity));
//                    }
//
//                    // 유사도가 0.1 이상인 모든 약품명 저장
//                    if (similarity >= 0.1f) {
//                        similarMedicines.add(dbMedicine);
//                    }
//                }
//            }
//
//            // 최종 매칭이 없고 유사한 약품명이 있는 경우
//            if (bestMatch == null && !similarMedicines.isEmpty()) {
//                // 유사한 약품명 목록을 보여주는 다이얼로그 표시
//                showSimilarMedicinesDialog(medicineName, similarMedicines);
//            }
//
//            if (bestMatch != null) {
//                Log.d(TAG, String.format("최종 매칭: %s (유사도: %.2f)", bestMatch, maxSimilarity));
//            } else {
//                Log.d(TAG, String.format("매칭 실패: %s (최대 유사도: %.2f)", medicineName, maxSimilarity));
//            }
//
//            return bestMatch;
//        } catch (Exception e) {
//            Log.e(TAG, "약품명 매칭 중 오류 발생", e);
//            return null;
//        }
//    }

    private String findBestMatch(String medicineName) {
        if (medicineName == null || medicineName.isEmpty()) {
            return null;
        }

        try {
            String normalizedInput = normalizeMedicineName(medicineName);
            String bestMatch = null;
            float maxSimilarity = SIMILARITY_THRESHOLD;
            List<String> similarMedicines = new ArrayList<>();

            // Room DB에서 약품 이름 전체 조회 (백그라운드 스레드에서 실행되므로 안전)
            List<MedicineName> allMedicines = medicineNameRepository.getAllMedicineNames();

            for (MedicineName med : allMedicines) {
                String dbName = med.getName();
                String normalizedDb = normalizeString(dbName);

                float similarity = calculateSimilarity(normalizedInput, normalizedDb);

                if (similarity > maxSimilarity && similarity >= 0.7f) { //유사도 0.7 이상인것만 화면에 표시하기
                    maxSimilarity = similarity;
                    bestMatch = dbName;
                    Log.d(TAG, String.format("매칭 발견: %s (유사도: %.2f)", dbName, similarity));
                }
                // 유사도가 0.7 이상인 모든 약품명 저장 (유사한 이름 목록 보여줄 때 사용)
                if (similarity >= 0.7f) {
                    similarMedicines.add(dbName);
                }
            }

            if (bestMatch == null && !similarMedicines.isEmpty()) {
                showSimilarMedicinesDialog(medicineName, similarMedicines);
            }

            return (maxSimilarity >= 0.7f) ? bestMatch : null;

        } catch (Exception e) {
            Log.e(TAG, "약품명 매칭 중 오류 발생", e);
            return null;
        }
    }



    private void showSimilarMedicinesDialog(String originalName, List<String> similarMedicines) {
        runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("유사한 약품명 목록");
            builder.setMessage("정확한 매칭이 없습니다. 아래 목록에서 선택해주세요.");

            // 약품명 목록을 문자열로 변환
            StringBuilder message = new StringBuilder();
            message.append("원본: ").append(originalName).append("\n\n");
            message.append("유사한 약품명 목록:\n");

            for (int i = 0; i < similarMedicines.size(); i++) {
                message.append(i + 1).append(". ").append(similarMedicines.get(i)).append("\n");
            }

            // 스크롤 가능한 TextView 생성
            TextView textView = new TextView(this);
            textView.setText(message.toString());
            textView.setPadding(50, 30, 50, 30);
            textView.setTextSize(16);

            ScrollView scrollView = new ScrollView(this);
            scrollView.addView(textView);

            builder.setView(scrollView);

            // 약품명 선택 버튼 추가
            builder.setPositiveButton("선택", (dialog, which) -> {
                // 선택한 약품명을 extractedMedicines에 추가
                String selectedMedicine = similarMedicines.get(0); // 첫 번째 약품명 선택
                if (!extractedMedicines.contains(selectedMedicine)) {
                    extractedMedicines.add(selectedMedicine);
                    displayResults();
                }
            });

            builder.setNegativeButton("취소", null);
            builder.show();
        });
    }

    private float calculateSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0;
        if (s1.equals(s2)) return 1.0f;

        int maxLength = Math.max(s1.length(), s2.length());
        if (maxLength == 0) return 1.0f;

        // Levenshtein 거리 계산
        int distance = levenshteinDistance(s1, s2);
        return 1.0f - (float) distance / maxLength;
    }

    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(dp[i - 1][j - 1],
                            Math.min(dp[i - 1][j], dp[i][j - 1]));
                }
            }
        }

        return dp[s1.length()][s2.length()];
    }

    private String normalizeString(String text) {

        if (text == null) return "";

        // 1. 기본 정규화: 앞뒤 공백 제거
        text = text.trim();

        // 2. 괄호와 그 내용 제거
        text = text.replaceAll("\\(.*?\\)", "").trim();

        // 3. 언더스코어(_)와 그 이후 내용 제거
        text = text.replaceAll("_.*$", "").trim();

        // 4. 추가 공백 제거
        text = text.replaceAll("\\s+", " ").trim();

        // 5. 숫자 이후의 문자열 모두 제거
        text = text.replaceAll("\\d.*$", "").trim();

        // 6. 특수문자 제거 (한글, 영문만 남김)
        text = text.replaceAll("[^가-힣a-zA-Z]", "").trim();

        // 7. 마지막 언더스코어 제거
        text = text.replaceAll("_$", "").trim();

        return text;

    }

    private String normalizeMedicineName(String name) {
        if (name == null) return "";

        // 1. 앞뒤 공백 제거
        name = name.trim();

        // 2. 괄호, 언더스코어, 특수문자, 숫자 이후 모든 문자 제거
        //    - 괄호 포함: (부터 이후 제거
        //    - 언더스코어 포함: _ 이후 제거
        //    - 숫자 포함: 숫자 이후 제거
        name = name.replaceAll("[\\(\\_\\d].*$", "").trim();

        // 3. 특수문자 제거 (한글, 영문만 남김)
        name = name.replaceAll("[^가-힣a-zA-Z]", "").trim();

        Log.d(TAG, "정규화된 약품명: " + name);
        return name;
    }


    private void displayResults() {
        LinearLayout resultLayout = findViewById(R.id.resultLayout);

        // 기존 결과 화면이 있다면 모두 제거
        resultLayout.removeAllViews();

        // 메인 레이아웃 생성
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(50, 30, 50, 30);

        // 제목 TextView
        TextView titleView = new TextView(this);
        titleView.setText("=== 인식된 약품명 ===");
        titleView.setTextSize(18);
        titleView.setPadding(0, 0, 0, 20);
        mainLayout.addView(titleView);

        if (extractedMedicines.isEmpty()) {
            TextView emptyView = new TextView(this);
            emptyView.setText("데이터베이스와 일치하는 약품명을 찾을 수 없습니다.\n\n" +
                    "처방전의 약품명이 잘 보이도록 다시 촬영해주세요.\n" +
                    "또는 제품명 목록 보기를 통해 직접 찾아보세요.");
            mainLayout.addView(emptyView);
        } else {
            // 약품명 개수 표시
            TextView countView = new TextView(this);
            countView.setText(String.format("총 %d개의 약품명이 인식되었습니다.", extractedMedicines.size()));
            countView.setPadding(0, 0, 0, 20);
            mainLayout.addView(countView);

            // 각 약품명에 대한 수정/삭제 버튼 추가
            for (int i = 0; i < extractedMedicines.size(); i++) {
                final int index = i;
                LinearLayout itemLayout = new LinearLayout(this);
                itemLayout.setOrientation(LinearLayout.HORIZONTAL);
                itemLayout.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
                itemLayout.setPadding(0, 10, 0, 10);

                TextView medicineText = new TextView(this);
                medicineText.setText(String.format("%d. %s", i + 1, extractedMedicines.get(i)));
                medicineText.setLayoutParams(new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                medicineText.setTextSize(16);

                Button editButton = new Button(this);
                editButton.setText("수정");
                editButton.setOnClickListener(v -> showEditDialog(index));

                Button deleteButton = new Button(this);
                deleteButton.setText("삭제");
                deleteButton.setOnClickListener(v -> {
                    extractedMedicines.remove(index);
                    displayResults();
                });

                itemLayout.addView(medicineText);
                itemLayout.addView(editButton);
                itemLayout.addView(deleteButton);
                mainLayout.addView(itemLayout);
            }
        }

        // 약품 추가 버튼
        Button addButton = new Button(this);
        addButton.setText("약품 추가");
        addButton.setOnClickListener(v -> showAddDialog());
        addButton.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        addButton.setPadding(0, 20, 0, 0);
        mainLayout.addView(addButton);

        // ScrollView로 감싸기
        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(mainLayout);

        // 결과 레이아웃에 ScrollView 추가
        resultLayout.addView(scrollView);

        progressBar.setVisibility(View.GONE);
        btnConfirm.setEnabled(!extractedMedicines.isEmpty());
    }

    private void showEditDialog(final int index) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("약품명 수정");

        final AutoCompleteTextView input = new AutoCompleteTextView(this);
        input.setText(extractedMedicines.get(index));
        input.setSelection(input.getText().length());
        input.setHint("약품명을 입력하세요");

        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);

        builder.setView(input);

        // 데이터베이스의 약품명을 어댑터로 생성
        input.setAdapter(this.adapter);
        input.setThreshold(1);

        // 텍스트 변경 리스너 추가
//        input.addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
//
//            @Override
//            public void onTextChanged(CharSequence s, int start, int before, int count) {
//                String query = s.toString().toLowerCase();
//                List<String> matches = new ArrayList<>();
//                for (String name : medicineNames) {
//                    if (name.toLowerCase().contains(query)) {
//                        matches.add(name);
//                    }
//                }
//                adapter.clear();
//                adapter.addAll(matches);
//                adapter.notifyDataSetChanged();
//            }
//
//            @Override
//            public void afterTextChanged(Editable s) {}
//        });


        builder.setPositiveButton("저장", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                extractedMedicines.set(index, newName);
                displayResults();
            }
        });

        builder.setNegativeButton("취소", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            input.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
        });

        dialog.show();
    }

    private void showAddDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("약품 추가");

        final AutoCompleteTextView input = new AutoCompleteTextView(this);
        input.setHint("약품명을 입력하세요");
        builder.setView(input);

        // 어댑터 부착
        input.setAdapter(this.adapter);
        input.setThreshold(1);

        // 텍스트 변경 리스너 추가
//        input.addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
//
//            @Override
//            public void onTextChanged(CharSequence s, int start, int before, int count) {
//                String query = s.toString().toLowerCase();
//                List<String> matches = new ArrayList<>();
//                for (String name : medicineNames) {
//                    if (name.toLowerCase().contains(query)) {
//                        matches.add(name);
//                    }
//                }
//                adapter.clear();
//                adapter.addAll(matches);
//                adapter.notifyDataSetChanged();
//            }
//
//            @Override
//            public void afterTextChanged(Editable s) {}
//        });

        builder.setPositiveButton("추가", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty() && !extractedMedicines.contains(newName)) {
                extractedMedicines.add(newName);
                displayResults();
            }
        });

        builder.setNegativeButton("취소", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            input.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
        });

        dialog.show();
    }
    @Override
    public void onBackPressed() {
        // 메인 액티비티로 돌아가기. OCR로 돌아가면 카메라 초기화가 제대로 이루어지지 않고 데이터베이스 접근도 실패하는 오류 발생함.
        super.onBackPressed();
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void cleanupImageResources() {
        // 비트맵 리소스 해제
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
            bitmap = null;
        }

        // ImageView 정리
        if (imageView != null) {
            imageView.setImageBitmap(null);
            imageView = null;
        }

        // URI 정리
        imageUri = null;
    }
}


