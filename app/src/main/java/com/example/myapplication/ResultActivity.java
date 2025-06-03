package com.example.myapplication;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.AsyncTask;
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.FileInputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ResultActivity extends AppCompatActivity {
    private static final String TAG = "ResultActivity";
    private ImageView imageView;
    private TextView resultTextView;
    private Button btnConfirm;
    private ProgressBar progressBar;
    private Bitmap bitmap;
    private Map<String, String> structuredData;
    private Set<String> extractedMedicines = new HashSet<>();
    private Uri imageUri;
    private Set<String> medicineDatabase = new HashSet<>();
    private static final float SIMILARITY_THRESHOLD = 0.2f;
    private static final float HIGH_SIMILARITY = 0.5f;
    private static final float MEDIUM_SIMILARITY = 0.3f;
    private DatabaseHelper dbHelper;
    private AlertDialog currentDialog;
    private TextView currentListView;
    private int currentPage = 0;
    private Set<String> recognizedMedicines = new HashSet<>();
    private Set<String> modifiedMedicines = new HashSet<>();

    private ArrayAdapter<String> adapter;

    private MedicineNameRepository medicineNameRepository;

    private Set<String> medicineNames = new HashSet<>(); // 자동완성을 위한 Set
    private List<MedicineName> nameList = new ArrayList<>(); // DB 조회 결과는 List 유지

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 윈도우 플래그 설정
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        setContentView(R.layout.activity_result);

        medicineNameRepository = new MedicineNameRepository(getApplication());

        new Thread(() -> {
            nameList = medicineNameRepository.getAllMedicineNames();

            runOnUiThread(() -> {
                for (MedicineName name : nameList) {
                    medicineNames.add(name.getName());
                }
                // 자동완성을 위한 어댑터 선언
                adapter = new ArrayAdapter<>(
                        ResultActivity.this,
                        android.R.layout.simple_dropdown_item_1line,
                        new ArrayList<>(medicineNames) // Set을 List로 변환하여 어댑터에 전달
                );
            });
        }).start();

        // OCR 결과와 수정된 약품명 Set 초기화
        recognizedMedicines = new HashSet<>();
        modifiedMedicines = new HashSet<>();

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
                // AddPrescriptionActivity로 이동, 추출한 의약품 목록 전달
                Intent intent = new Intent(this, AddPrescriptionActivity.class);
                intent.putStringArrayListExtra("medicine_names", new ArrayList<>(extractedMedicines)); // Set을 List로 변환하여 전달
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

                    // OCR 처리를 위한 최적화된 크기로 조정 (800x800으로 제한)
                    int maxSize = 800; // OCR 처리에 충분한 크기로 조정
                    options.inSampleSize = calculateInSampleSize(options, maxSize, maxSize);
                    options.inJustDecodeBounds = false;
                    options.inPreferredConfig = Bitmap.Config.RGB_565; // 메모리 사용량 감소
                    options.inDither = true;

                    // 이미지 다시 로드
                    inputStream = resolver.openInputStream(imageUri);
                    bitmap = BitmapFactory.decodeStream(inputStream, null, options);

                    if (bitmap == null) {
                        Log.e(TAG, "이미지를 디코딩할 수 없습니다.");
                        return null;
                    }

                    // 이미지 크기 로깅
                    Log.d(TAG, String.format("최적화된 이미지 크기: %dx%d", bitmap.getWidth(), bitmap.getHeight()));

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
        private static final int MAX_IMAGE_SIZE = 1024;

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            resultTextView.setText("텍스트 인식 중...");
        }

        @Override
        protected Text doInBackground(Void... voids) {
            try {
                if (bitmap == null || bitmap.isRecycled()) {
                    return null;
                }

                // 1. 최적화된 이미지 크기 조정
                Bitmap resizedBitmap = null;
                try {
                    int width = bitmap.getWidth();
                    int height = bitmap.getHeight();
                    
                    // 이미지가 너무 크면 최적화된 크기로 조정
                    if (width > MAX_IMAGE_SIZE || height > MAX_IMAGE_SIZE) {
                        float scale = Math.min(
                            (float) MAX_IMAGE_SIZE / width,
                            (float) MAX_IMAGE_SIZE / height
                        );
                        int newWidth = Math.round(width * scale);
                        int newHeight = Math.round(height * scale);
                        
                        // 품질과 속도의 균형을 맞춘 스케일링
                        resizedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
                    } else {
                        resizedBitmap = bitmap;
                    }

                    // 2. OCR 처리 (한국어 설정)
                    InputImage image = InputImage.fromBitmap(resizedBitmap, 0);
                    TextRecognizer recognizer = TextRecognition.getClient(
                            new KoreanTextRecognizerOptions.Builder().build());

                    // 3. OCR 실행
                    Task<Text> result = recognizer.process(image);
                    return Tasks.await(result);

                } finally {
                    // 4. 메모리 정리
                    if (resizedBitmap != null && resizedBitmap != bitmap) {
                        resizedBitmap.recycle();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "OCR 처리 중 오류 발생", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Text text) {
            if (text == null) {
                Toast.makeText(ResultActivity.this, "텍스트 인식에 실패했습니다.", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // 5. 텍스트 처리 시작
            new OptimizedTextProcessingTask().execute(text);
        }
    }

    private class OptimizedTextProcessingTask extends AsyncTask<Text, Void, Set<String>> {
        private final Set<String> MEDICINE_KEYWORDS = new HashSet<>(Arrays.asList(
                "정", "캡슐", "시럽", "주사", "액", "연고", "크림", "패치",
                "tab", "cap", "inj", "cream", "patch", "gel", "정제", "주사제",
                "캡슐제", "시럽제", "연고제", "크림제", "패치제", "가루", "산", "환",
                "물", "좌", "점안", "점이", "주", "알", "개", "통", "병", "셀"
        ));

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            resultTextView.setText("약품명 분석 중...");
        }

        @Override
        protected Set<String> doInBackground(Text... texts) {
            Text text = texts[0];
            Set<String> recognizedMedicines = new HashSet<>();
            if (text == null) return recognizedMedicines;

            // 1. DB 데이터 미리 로드 및 캐싱
            List<MedicineName> medicines = medicineNameRepository.getAllMedicineNames();
            Map<String, String> medicineMap = new HashMap<>();
            Map<String, String> normalizedMap = new HashMap<>();
            
            for (MedicineName med : medicines) {
                String name = med.getName();
                String normalized = CommonMethod.normalizeWord(name);
                medicineMap.put(normalized, name);
                normalizedMap.put(name, normalized);
            }

            // 2. 텍스트 블록 병렬 처리
            ExecutorService executor = Executors.newFixedThreadPool(4);
            List<Future<Set<String>>> futures = new ArrayList<>();

            for (Text.TextBlock block : text.getTextBlocks()) {
                futures.add(executor.submit(() -> processTextBlock(block, medicineMap, normalizedMap)));
            }

            // 3. 결과 수집
            try {
                for (Future<Set<String>> future : futures) {
                    recognizedMedicines.addAll(future.get());
                }
            } catch (Exception e) {
                Log.e(TAG, "텍스트 처리 중 오류 발생", e);
            } finally {
                executor.shutdown();
            }

            return recognizedMedicines;
        }

        private Set<String> processTextBlock(Text.TextBlock block, 
                                          Map<String, String> medicineMap,
                                          Map<String, String> normalizedMap) {
            Set<String> blockResults = new HashSet<>();
            String blockText = block.getText().trim();
            if (blockText.isEmpty()) return blockResults;

            // 4. 약품명이 포함된 라인 필터링
            String[] lines = blockText.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // 5. 빠른 키워드 체크
                if (containsMedicineKeyword(line)) {
                    String normalized = CommonMethod.normalizeWord(line);
                    Log.d(TAG, String.format("@@ OCR 추출 라인: %s/ 정규화: %s ",line, normalized));

                    if (normalized != null && !normalized.isEmpty()) {
                        // 6. 정확한 매칭 시도
                        String matched = medicineMap.get(normalized);
                        if (matched != null) {
                            Log.d(TAG, String.format("@@ 정확 매칭: OCR='%s' 정규화='%s' 매칭='%s'", line, normalized, matched));
                            blockResults.add(matched);
                            continue;
                        }

                        // 7. 유사도 기반 매칭
                        for (Map.Entry<String, String> entry : medicineMap.entrySet()) {
                            String dbName = entry.getKey();
                            if (Math.abs(normalized.length() - dbName.length()) <= 2) {
                                float similarity = calculateSimilarity(normalized, dbName);
                                if (similarity >= 0.7f) {
                                    Log.d(TAG, String.format("@@ 유사 매칭: OCR='%s' 정규화='%s' 매칭='%s' | 유사도=%.2f", line, normalized, dbName, similarity));

                                    blockResults.add(entry.getValue());
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            return blockResults;
        }

        private boolean containsMedicineKeyword(String text) {
            String lowerText = text.toLowerCase();
            for (String keyword : MEDICINE_KEYWORDS) {
                if (lowerText.contains(keyword.toLowerCase())) {
                    return true;
                }
            }
            return false;
        }

        private float calculateSimilarity(String s1, String s2) {
            if (s1 == null || s2 == null) return 0;
            if (s1.equals(s2)) return 1.0f;

            int maxLength = Math.max(s1.length(), s2.length());
            if (maxLength == 0) return 1.0f;

            int distance = levenshteinDistance(s1, s2);
            return 1.0f - (float) distance / maxLength;
        }


        @Override
        protected void onPostExecute(Set<String> medicines) {
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
        if (s1 == null || s2 == null) {
            Log.d(TAG, "🟠 calculateSimilarity: 입력 중 null 존재 -> s1=" + s1 + ", s2=" + s2);
            return 0f;
        }

        if (s1.equals(s2)) {
            Log.d(TAG, "🟢 calculateSimilarity: 완전일치 -> s1=" + s1 + ", s2=" + s2);
            return 1.0f;
        }

        int maxLength = Math.max(s1.length(), s2.length());
        if (maxLength == 0) {
            Log.d(TAG, "🟠 calculateSimilarity: 두 문자열 모두 빈 문자열");
            return 1.0f;
        }
        // Levenshtein 거리 계산
        int distance = levenshteinDistance(s1, s2);
        float similarity = 1.0f - (float) distance / maxLength;

        Log.d(TAG, String.format("🔍 calculateSimilarity: \"%s\" ↔ \"%s\" | 거리: %d | 유사도: %.3f", s1, s2, distance, similarity));

        return similarity;
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

    private void displayResults() {
        LinearLayout resultLayout = findViewById(R.id.resultLayout);
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

        // 약품명 목록을 정렬된 List로 변환하여 표시
        List<String> sortedMedicines = new ArrayList<>(extractedMedicines);
        Collections.sort(sortedMedicines);

        for (String medicine : sortedMedicines) {
            LinearLayout itemLayout = new LinearLayout(this);
            itemLayout.setOrientation(LinearLayout.HORIZONTAL);
            itemLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            itemLayout.setPadding(0, 10, 0, 10);

            TextView medicineText = new TextView(this);
            medicineText.setText(medicine);
            medicineText.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            medicineText.setTextSize(16);

            Button editButton = new Button(this);
            editButton.setText("수정");
            editButton.setOnClickListener(v -> showEditDialog(medicine));

            Button deleteButton = new Button(this);
            deleteButton.setText("삭제");
            deleteButton.setOnClickListener(v -> {
                extractedMedicines.remove(medicine);
                displayResults();
            });

            itemLayout.addView(medicineText);
            itemLayout.addView(editButton);
            itemLayout.addView(deleteButton);
            mainLayout.addView(itemLayout);
        }

        // 약품 추가 버튼
        Button addButton = new Button(this);
        addButton.setText("약품 추가");
        addButton.setOnClickListener(v -> showAddMedicineDialog());
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

    private void showEditDialog(final String medicine) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("약품명 수정");

        final AutoCompleteTextView input = new AutoCompleteTextView(this);
        input.setText(medicine);
        input.setSelection(input.getText().length());
        input.setHint("약품명을 입력하세요");

        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);

        builder.setView(input);

        // 데이터베이스의 약품명을 어댑터로 생성
        input.setAdapter(adapter);
        input.setThreshold(1);

        builder.setPositiveButton("저장", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                extractedMedicines.remove(medicine);
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

    private void showAddMedicineDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("약품 추가");

        // 입력 필드 생성
        final AutoCompleteTextView input = new AutoCompleteTextView(this);
        input.setAdapter(adapter);
        input.setThreshold(1);
        input.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        input.setPadding(50, 30, 50, 30);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(input);
        builder.setView(layout);

        builder.setPositiveButton("추가", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                extractedMedicines.add(newName); // Set은 자동으로 중복 제거
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


