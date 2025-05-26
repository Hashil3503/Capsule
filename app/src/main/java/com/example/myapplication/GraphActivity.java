package com.example.myapplication;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.*;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class GraphActivity extends AppCompatActivity {
    private final int Max_Empty_Sugar = 100;
    private final int Min_Empty_Sugar = 70;

    private final int Max_noEmpty_Sugar = 140;
    private final int Min_noEmpty_Sugar = 70;
    private final int Max_Systolic_Pressure = 120;
    private final int Min_Systolic_Pressure = 90;
    private final int Max_Diastolic_Pressure = 80;
    private final int Min_Diastolic_Pressure = 60;

    private BloodSugarRepository bloodSugarRepository;
    private BloodPressureRepository bloodPressureRepository;
    private LinearLayout graphContainer;
    private TextView dateText;
    private TextView resultStatusText;
    private TextView abnormalText; //비정상 좌표들 x값 나열할 텍스트뷰
    private String selectedDate = "";
    private String currentGraphType = "bloodSugar";
    private List<String> abnormalList = new ArrayList<>(); // 비정상 좌표 x값을 저장할 리스트

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM", Locale.KOREA);
        Date today = new Date();
        selectedDate = sdf.format(today);

        ScrollView scrollView = new ScrollView(this);
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(32, 32, 32, 32);

        int bgColor = isDarkMode() ? Color.BLACK : Color.WHITE;
        int textColor = isDarkMode() ? Color.LTGRAY : Color.DKGRAY;
        mainLayout.setBackgroundColor(bgColor);

        dateText = new TextView(this);
        dateText.setText("날짜: " + selectedDate);
        dateText.setTextSize(20);
        dateText.setTextColor(textColor);
        mainLayout.addView(dateText);

        graphContainer = new LinearLayout(this);
        graphContainer.setOrientation(LinearLayout.VERTICAL);
        mainLayout.addView(graphContainer);

        mainLayout.addView(getButtonLayout());

        resultStatusText = new TextView(this);
        resultStatusText.setTextSize(20);
        resultStatusText.setPadding(0, 20, 0, 20);
        resultStatusText.setTextColor(textColor);
        mainLayout.addView(resultStatusText);

        TextView descriptionText = new TextView(this);
        descriptionText.setText("※ 좌표쌍 점은 수치 기준에 따라 색으로 구분됩니다. (정상: 검정색, 비정상: 마젠타)");
        descriptionText.setTextSize(16);
        descriptionText.setTextColor(textColor);
        mainLayout.addView(descriptionText);

        scrollView.addView(mainLayout);
        setContentView(scrollView);

        bloodSugarRepository = new BloodSugarRepository(getApplication());
        bloodPressureRepository = new BloodPressureRepository(getApplication());

        showGraph(currentGraphType);
    }

    @NonNull
    private LinearLayout getButtonLayout() {
        LinearLayout buttonRow = new LinearLayout(this);
        buttonRow.setOrientation(LinearLayout.HORIZONTAL);
        buttonRow.setPadding(0, 16, 0, 16);
        buttonRow.setWeightSum(3);

        Button bloodSugarButton = new Button(this);
        bloodSugarButton.setText("혈당 그래프");
        bloodSugarButton.setOnClickListener(v -> {
            currentGraphType = "bloodSugar";
            showGraph(currentGraphType);
        });
        bloodSugarButton.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        Button bloodPressureButton = new Button(this);
        bloodPressureButton.setText("혈압 그래프");
        bloodPressureButton.setOnClickListener(v -> {
            currentGraphType = "bloodPressure";
            showGraph(currentGraphType);
        });
        bloodPressureButton.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        Button dateButton = new Button(this);
        dateButton.setText("날짜 변경");
        dateButton.setOnClickListener(v -> showMonthPicker());
        dateButton.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        buttonRow.addView(bloodSugarButton);
        buttonRow.addView(bloodPressureButton);
        buttonRow.addView(dateButton);

        return buttonRow;
    }

    private boolean isDarkMode() {
        int nightModeFlags = AppCompatDelegate.getDefaultNightMode();
        return nightModeFlags == AppCompatDelegate.MODE_NIGHT_YES;
    }

    @SuppressLint("SetTextI18n")
    private void showGraph(String type) {
        graphContainer.removeAllViews();
        resultStatusText.setText("");

        HorizontalScrollView horizontalScrollView = new HorizontalScrollView(this);
        LinearLayout graphLayout = new LinearLayout(this);
        graphLayout.setOrientation(LinearLayout.VERTICAL);

        new Thread(() -> {
            List<BloodSugar> bloodSugars = bloodSugarRepository.getByMonth(selectedDate);
            List<BloodPressure> bloodPressures = bloodPressureRepository.getByMonth(selectedDate);

            runOnUiThread(() -> {
                GraphView graphView = null;
                boolean hasAbnormal = false;

                if (type.equals("bloodSugar")) {
                    hasAbnormal = checkBloodSugarAbnormal(bloodSugars);
                    graphView = new GraphView(this, type, selectedDate, bloodSugars, null, abnormalList);
                } else if (type.equals("bloodPressure")) {
                    hasAbnormal = checkBloodPressureAbnormal(bloodPressures);
                    graphView = new GraphView(this, type, selectedDate, null, bloodPressures, abnormalList);
                }

                if (graphView != null) {
                    graphView.setLayoutParams(new LinearLayout.LayoutParams(2000, 1300));
                    graphLayout.addView(graphView);
                    horizontalScrollView.addView(graphLayout);
                    graphContainer.addView(horizontalScrollView);

                    if (hasAbnormal) {
                        StringBuilder resultList = new StringBuilder("비정상 일자 : ");
                        for (int i = 0; i < abnormalList.size(); i++) {
                            resultList.append(abnormalList.get(i));
                            if (i < abnormalList.size() - 1) {
                                resultList.append(", ");
                            }
                        }
                        resultStatusText.setText(resultList.toString());
                        resultStatusText.setTextColor(Color.RED);
                        resultStatusText.setVisibility(View.VISIBLE);
                    } else {
                        resultStatusText.setVisibility(View.GONE);
                    }
                }
            });
        }).start();
    }

//    private boolean checkBloodSugarAbnormal(List<BloodSugar> sugars) {
//        if (sugars == null) return false;
//        for (BloodSugar s : sugars) {
//            if (s.getEmpty_stomach() && s.getValue() > 100) return true;
//            if (!s.getEmpty_stomach() && s.getValue() > 140) return true;
//        }
//        return false;
//    }

    private boolean checkBloodSugarAbnormal(List<BloodSugar> sugars) {
        if (sugars == null) return false;
        boolean abnormalFound = false;
        abnormalList.clear(); // 리스트 비우기

        for (BloodSugar s : sugars) {
            boolean abnormal = s.getEmpty_stomach() && s.getValue() >= Max_Empty_Sugar || s.getEmpty_stomach() && s.getValue() < Min_Empty_Sugar
                    || !s.getEmpty_stomach() && s.getValue() >= Max_noEmpty_Sugar || !s.getEmpty_stomach() && s.getValue() < Min_noEmpty_Sugar;
            if (abnormal) {
                abnormalFound = true;
                String date = new SimpleDateFormat("d", Locale.KOREA).format(s.getDate());
                abnormalList.add(date); // 리스트에 추가
            }
        }
        return abnormalFound;
    }

//    private boolean checkBloodPressureAbnormal(List<BloodPressure> pressures) {
//        if (pressures == null) return false;
//        for (BloodPressure p : pressures) {
//            if (p.getSystolic() > 140 || p.getDiastolic() > 90) return true;
//        }
//        return false;
//    }

    private boolean checkBloodPressureAbnormal(List<BloodPressure> pressures) {
        if (pressures == null) return false;
        boolean abnormalFound = false;
        abnormalList.clear(); // 리스트 비우기

        for (BloodPressure p : pressures) {
            boolean abnormal = p.getSystolic() >= Max_Systolic_Pressure || p.getSystolic() < Min_Systolic_Pressure || p.getDiastolic() >= Max_Diastolic_Pressure || p.getDiastolic() < Min_Diastolic_Pressure;
            if (abnormal) {
                abnormalFound = true;
                String date = new SimpleDateFormat("d", Locale.KOREA).format(p.getDate());
                abnormalList.add(date); // 리스트에 추가
            }
        }
        return abnormalFound;
    }

    @SuppressLint("SetTextI18n")
    private void showMonthPicker() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("날짜 선택 (YYYY-MM)");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setPadding(50, 40, 50, 10);

        final NumberPicker yearPicker = new NumberPicker(this);
        final NumberPicker monthPicker = new NumberPicker(this);

        int year = Calendar.getInstance().get(Calendar.YEAR);
        yearPicker.setMinValue(2000);
        yearPicker.setMaxValue(2100);
        yearPicker.setValue(year);

        monthPicker.setMinValue(1);
        monthPicker.setMaxValue(12);
        monthPicker.setValue(Calendar.getInstance().get(Calendar.MONTH) + 1);

        layout.addView(yearPicker);
        layout.addView(monthPicker);
        builder.setView(layout);

        builder.setPositiveButton("확인", (dialog, which) -> {
            selectedDate = String.format(Locale.KOREA, "%d-%02d", yearPicker.getValue(), monthPicker.getValue());
            dateText.setText("날짜: " + selectedDate);
            if (currentGraphType == null) currentGraphType = "bloodSugar";
            showGraph(currentGraphType);
        });

        builder.setNegativeButton("취소", null);
        builder.show();
    }

    @SuppressLint("ViewConstructor")
    static class GraphView extends View {
        private final Paint axisPaint, textPaint, linePaint, dotPaint, labelPaint;
        private final String baseMonth;
        private final Map<String, Float> data1 = new LinkedHashMap<>();
        private final Map<String, Float> data2 = new LinkedHashMap<>();
        private final String label1, label2;
        private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
        private final List<String> abnormalList;

        public GraphView(Context context, String type, String baseMonth, List<BloodSugar> sugars, List<BloodPressure> pressures, List<String> abnormalList) {
            super(context);
            this.baseMonth = baseMonth;
            this.abnormalList = abnormalList;



            axisPaint = new Paint();
            axisPaint.setColor(Color.BLACK);
            axisPaint.setStrokeWidth(4);

            textPaint = new Paint();
            textPaint.setColor(Color.GRAY);
            textPaint.setTextSize(24);

            linePaint = new Paint();
            linePaint.setStrokeWidth(5);

            dotPaint = new Paint();
            dotPaint.setStrokeWidth(10);

            labelPaint = new Paint();
            labelPaint.setTextSize(30);
            labelPaint.setFakeBoldText(true);

            if ("bloodPressure".equals(type)) {
                label1 = "수축";
                label2 = "이완";
                if (pressures != null) {
                    for (BloodPressure p : pressures) {
                        String day = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(p.getDate());
                        data1.put(day, (float) p.getSystolic());
                        data2.put(day, (float) p.getDiastolic());
                    }
                }
            } else {
                label1 = "공복";
                label2 = "식후";
                if (sugars != null) {
                    for (BloodSugar s : sugars) {
                        String day = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(s.getDate());
                        if (s.getEmpty_stomach()) data1.put(day, s.getValue());
                        else data2.put(day, s.getValue());
                    }
                }
            }
        }

        @Override
        protected void onDraw(@NonNull Canvas canvas) {
            int width = getWidth();
            int height = getHeight();

            float startX = 100f;
            float startY = height - 100f;
            float endX = width - 100f;
            float maxY = 200f;
            float availableWidth = endX - startX;
            float availableHeight = startY - 150f;

            float scaleX = availableWidth / 34f;
            float scaleY = availableHeight / maxY;

            canvas.drawLine(startX, 50, startX, startY, axisPaint);
            canvas.drawLine(startX, startY, endX, startY, axisPaint);

            for (int i = 1; i <= 31; i++) {
                float x = startX + i * scaleX;
                canvas.drawText(String.valueOf(i), x - 10, startY + 30, textPaint);
            }

            for (float i = 20; i <= maxY; i += 20f) {
                float y = startY - i * scaleY;
                canvas.drawText(String.format("%.0f", i), 40, y + 10, textPaint);
            }

            labelPaint.setColor(Color.BLUE);
            canvas.drawText(label1, 20, 40, labelPaint);
            labelPaint.setColor(Color.RED);
            canvas.drawText(label2, 20, 80, labelPaint);

            drawGraph(canvas, data1, label1, startX, startY, scaleX, scaleY, Color.BLUE);
            drawGraph(canvas, data2, label2, startX, startY, scaleX, scaleY, Color.RED);
        }

        private void drawGraph(Canvas canvas, Map<String, Float> data, String label,
                               float startX, float startY, float scaleX, float scaleY, int color) {
            linePaint.setColor(color);

            Float lastX = null, lastY = null;

            for (Map.Entry<String, Float> entry : data.entrySet()) {
                float xIndex;
                try {
                    xIndex = getDateDiff(baseMonth + "-01", entry.getKey()) + 1f;
                } catch (ParseException e) {
                    continue;
                }

                float x = startX + xIndex * scaleX;
                float y = startY - entry.getValue() * scaleY;

                if (lastX != null) {
                    canvas.drawLine(lastX, lastY, x, y, linePaint);
                }

                boolean abnormal = isAbnormal(label, entry.getValue());
                dotPaint.setColor(abnormal ? Color.MAGENTA : Color.BLACK);
                canvas.drawCircle(x, y, 10, dotPaint);

                lastX = x;
                lastY = y;
            }
        }

        private int getDateDiff(String startDate, String endDate) throws ParseException {
            Date date1 = sdf.parse(startDate);
            Date date2 = sdf.parse(endDate);
            long diff = date2.getTime() - date1.getTime();
            return (int) (diff / (1000 * 60 * 60 * 24));
        }

        private boolean isAbnormal(String label, float value) { // 비정상이면 true, 정상이면 false 반환
            switch (label) {
                case "공복":
                    return value < 70 || value >= 100;
                case "식후":
                    return value < 70 || value >= 140;
                case "수축":
                    return value < 90 || value >= 120;
                case "이완":
                    return value < 60 || value >= 80;
                default:
                    return false;
            }
        }
    }
}










