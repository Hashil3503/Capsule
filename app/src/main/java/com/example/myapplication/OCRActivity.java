package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.Preview;
import androidx.camera.core.SurfaceOrientedMeteringPointFactory;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.ListenableFuture;

public class OCRActivity extends AppCompatActivity {

    private static final String TAG = "OCRActivity";
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final int REQUEST_CODE_GALLERY = 20;
    private final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    private PreviewView previewView;
    private ImageView previewImageView;
    private Button btnCapture;
    private Button btnGallery;

    private Button button_add_prescription;
    private ProgressBar progressBar;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private File photoFile;
    private Uri photoUri;
    private ProcessCameraProvider cameraProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocr);

        previewView = findViewById(R.id.textureView);
        previewImageView = findViewById(R.id.imageView);
        btnCapture = findViewById(R.id.btnCapture);
        btnGallery = findViewById(R.id.btnGallery);
        progressBar = findViewById(R.id.progressBar);
        button_add_prescription = findViewById((R.id.button));

        cameraExecutor = Executors.newSingleThreadExecutor();

        // 초기에는 이미지뷰를 숨김
        previewImageView.setVisibility(View.GONE);
        previewView.setVisibility(View.VISIBLE);

        // 권한 확인 및 요청
        if (!checkPermissions()) {
            requestPermissions();
        } else {
            initializeCamera();
        }

        btnCapture.setOnClickListener(v -> takePhoto());
        btnGallery.setOnClickListener(v -> openGallery());

        button_add_prescription.setOnClickListener(new View.OnClickListener() { //새탭 버튼 클릭시 이벤트 동작처리
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(OCRActivity.this, AddPrescriptionActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkPermissions()) {
            initializeCamera();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        closeCamera();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeCamera();
    }

    private void closeCamera() {
        if (cameraProvider != null) {
            try {
                // 모든 바인딩 해제
                cameraProvider.unbindAll();
                cameraProvider = null;
            } catch (Exception e) {
                Log.e(TAG, "카메라 리소스 해제 중 오류 발생", e);
            }
        }
    }

    private void initializeCamera() {
        // 이전 카메라 인스턴스 정리
        closeCamera();

        // 새로운 카메라 초기화
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (Exception e) {
                Log.e(TAG, "카메라 초기화 실패", e);
                Toast.makeText(this, "카메라를 초기화할 수 없습니다. 앱을 다시 시작해주세요.", Toast.LENGTH_LONG).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        try {
            // 이전 바인딩 해제
            cameraProvider.unbindAll();

            // 프리뷰 설정
            Preview preview = new Preview.Builder().build();
            preview.setSurfaceProvider(previewView.getSurfaceProvider());

            // 이미지 캡처 설정
            imageCapture = new ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build();

            // 카메라 선택
            CameraSelector cameraSelector = new CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build();

            // 카메라에 유즈케이스 바인딩
            Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

            // 카메라 설정 (자동 초점 등)
            setupCamera(camera);

        } catch (Exception e) {
            Log.e(TAG, "카메라 바인딩 실패", e);
            Toast.makeText(this, "카메라를 시작할 수 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupCamera(Camera camera) {
        try {
            // 자동 초점 모드 설정
            camera.getCameraControl().enableTorch(false);
            camera.getCameraInfo().getTorchState().observe(this, state -> {
                // 토치 상태 변경 시 처리
            });

            // 자동 초점 설정
            MeteringPoint point = new SurfaceOrientedMeteringPointFactory(1.0f, 1.0f)
                    .createPoint(0.5f, 0.5f);
            FocusMeteringAction action = new FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                    .setAutoCancelDuration(3, TimeUnit.SECONDS)
                    .build();
            camera.getCameraControl().startFocusAndMetering(action);

        } catch (Exception e) {
            Log.e(TAG, "카메라 설정 실패", e);
        }
    }

    private void requestPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();

        // 카메라 권한
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CAMERA);
        }

        // Android 13 이상에서는 READ_MEDIA_IMAGES 권한 사용
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            // Android 13 미만에서는 기존 저장소 권한 사용
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_CODE_PERMISSIONS);
        }
    }

    private boolean checkPermissions() {
        // 카메라 권한 확인
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        // Android 13 이상에서는 READ_MEDIA_IMAGES 권한 확인
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED;
        } else {
            // Android 13 미만에서는 기존 저장소 권한 확인
            boolean readStorage = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
            boolean writeStorage = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
            return readStorage && writeStorage;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                initializeCamera();
            } else {
                Toast.makeText(this, "필요한 권한이 없으면 앱을 사용할 수 없습니다.", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void takePhoto() {
        if (imageCapture == null) {
            Toast.makeText(this, "카메라가 초기화되지 않았습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnCapture.setEnabled(false);

        // 임시 파일 생성
        File photoFile = new File(getExternalFilesDir(null),
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                        .format(System.currentTimeMillis()) + ".jpg");

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        try {
                            // 이미지 파일이 존재하는지 확인
                            if (!photoFile.exists()) {
                                throw new IOException("이미지 파일이 존재하지 않습니다.");
                            }

                            // 이미지 크기 확인
                            BitmapFactory.Options options = new BitmapFactory.Options();
                            options.inJustDecodeBounds = true;
                            BitmapFactory.decodeFile(photoFile.getAbsolutePath(), options);

                            if (options.outWidth <= 0 || options.outHeight <= 0) {
                                throw new IOException("이미지 파일이 손상되었습니다.");
                            }

                            // 카메라 리소스 해제
                            if (cameraProvider != null) {
                                cameraProvider.unbindAll();
                                cameraProvider = null;
                            }
                            if (imageCapture != null) {
                                imageCapture = null;
                            }
                            if (cameraExecutor != null) {
                                cameraExecutor.shutdown();
                                cameraExecutor = null;
                            }

                            // ResultActivity로 이동
                            Intent intent = new Intent(OCRActivity.this, ResultActivity.class);
                            intent.putExtra("imageUri", Uri.fromFile(photoFile).toString());
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);

                        } catch (Exception e) {
                            Log.e(TAG, "이미지 처리 중 오류 발생", e);
                            runOnUiThread(() -> {
                                Toast.makeText(OCRActivity.this,
                                        "이미지 처리 중 오류가 발생했습니다. 다시 시도해주세요.",
                                        Toast.LENGTH_SHORT).show();
                                progressBar.setVisibility(View.GONE);
                                btnCapture.setEnabled(true);
                            });
                        }
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exc) {
                        Log.e(TAG, "사진 촬영 실패", exc);
                        Toast.makeText(OCRActivity.this, "사진 촬영에 실패했습니다.", Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                        btnCapture.setEnabled(true);
                    }
                });
    }

    private void processImageAndStartOCR(Uri imageUri) {
        try {
            // ResultActivity로 직접 이동
            Intent intent = new Intent(OCRActivity.this, ResultActivity.class);
            intent.putExtra("imageUri", imageUri.toString());
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "이미지 처리 중 오류 발생", e);
            Toast.makeText(this, "이미지 처리 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_CODE_GALLERY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_GALLERY && resultCode == RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                processImageAndStartOCR(selectedImageUri);
            }
        }
    }

    private Bitmap optimizeImage(String imagePath) {
        try {
            // 이미지 크기 계산
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imagePath, options);

            // 최대 크기 제한 (메모리 사용량 감소)
            int maxDimension = 1024;
            int scale = 1;
            while (options.outWidth / scale > maxDimension || options.outHeight / scale > maxDimension) {
                scale *= 2;
            }

            // 실제 이미지 로드
            options.inJustDecodeBounds = false;
            options.inSampleSize = scale;
            options.inPreferredConfig = Bitmap.Config.RGB_565; // 메모리 사용량 감소
            options.inDither = true;

            Bitmap bitmap = BitmapFactory.decodeFile(imagePath, options);

            // 이미지 크기 조정
            if (bitmap != null && (bitmap.getWidth() > maxDimension || bitmap.getHeight() > maxDimension)) {
                float ratio = Math.min(
                        (float) maxDimension / bitmap.getWidth(),
                        (float) maxDimension / bitmap.getHeight()
                );

                int newWidth = Math.round(bitmap.getWidth() * ratio);
                int newHeight = Math.round(bitmap.getHeight() * ratio);

                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
                bitmap.recycle();
                bitmap = scaledBitmap;
            }

            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "이미지 최적화 중 오류 발생", e);
            return null;
        }
    }
}
