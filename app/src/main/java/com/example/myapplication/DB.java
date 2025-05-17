package com.example.myapplication;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;


//앱에서 사용할 데이터베이스 (일단은 한개로 구성했는데, 추후 로그인 기능 등을 추가한다면 추가적인 파일 필요)
@Database(entities = {Prescription.class, Medication.class, Prescription_View.class, BloodPressure.class, BloodSugar.class, MedicineTable.class, MedicineName.class}, version = 1)

@TypeConverters({DateConverter.class}) //Date 타입 저장을 위한 컨버터를 DB에 적용시키는 구문

public abstract class DB extends RoomDatabase {
    public abstract PrescriptionDao prescriptionDao();
    public abstract MedicationDao medicationDao();
    public abstract Prescription_ViewDao prescription_viewDao();
    public abstract BloodSugarDao bloodsugarDao();
    public abstract BloodPressureDao bloodpressureDao();
    public abstract MedicineTableDao medicineTableDao();
    public abstract MedicineNameDao medicineNameListDao();

    private static volatile DB INSTANCE;
    //static으로 선언하여 db 인스턴스를 하나만 유지함
    //volatile을 사용해 멀티스레드 환경에서도 인스턴스가 안전하게 공유됨
    public static DB getInstance(Context context) { //getInstance는 인스턴스가 없는 경우 생성, 있다면 기존의 인스턴스를 반환해 중복 생성을 방지 (=싱글톤 패턴)
        if (INSTANCE == null) { //동시에 2개 이상의 인스턴스 생성을 방지
            synchronized (DB.class) { //멀티스레드 환경에서도 동기화를 통해 싱글톤 유지를 위해 사용?
                if (INSTANCE == null) { //여러 스레드가 동시에 접근할 경유 이 조건식을 통해서 동시에 2개 이상의 인스턴스 생성을 방지 (이중으로 검사함)
                    INSTANCE = Room.databaseBuilder( //데이터베이스 인스턴스 생성
                            context.getApplicationContext(),
                            DB.class,
                            "app_database"
                            ).addCallback(new RoomDatabase.Callback() {
                                @Override
                                public void onOpen(@NonNull SupportSQLiteDatabase db) {
                                    super.onOpen(db);
                                    db.execSQL("PRAGMA foreign_keys=ON;"); // foreign key 제약조건 활성화
                                }
                                @Override
                                public void onCreate(@NonNull SupportSQLiteDatabase db) { // foreign key 제약조건 활성화2
                                   db.execSQL("PRAGMA foreign_keys=ON;");
                                }
                            })
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}