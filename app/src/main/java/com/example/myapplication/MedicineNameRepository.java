package com.example.myapplication;
import android.app.Application;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

//Dao와 UI를 분리하여 코드 유지보수를 원활하게 하기 위한 Repository (데이터베이스 구조 변경시에 Activity 코드 수정 최소화)
public class MedicineNameRepository {
    private final MedicineNameDao medicineNameDao;
    private final ExecutorService executorService;

    public MedicineNameRepository(Application application) {
        DB db = DB.getInstance(application); //DB 싱글톤 인스턴스 가져오기
        medicineNameDao = db.medicineNameListDao(); //dao 객체 가져오기
        executorService = Executors.newSingleThreadExecutor(); //데이터베이스 작업을 백그라운드 스레드에서 실행
    }

    public long insert(MedicineName medicineName) {
        Future<Long> future = executorService.submit(() -> medicineNameDao.insert(medicineName)); //Future<타입>
        try {
            return future.get(); // 백그라운드 작업 완료 후 ID 반환
        } catch (Exception e) { //오류 발생 감지
            e.printStackTrace(); //오류 발생시 오류 원인을 출력
            return -1; // 오류 발생 시 기본값 반환
        }
    }

    public void insertAll(List<String> productNames) {
        List<MedicineName> medicineNameList = new ArrayList<>();
        for (String name : productNames) {
            medicineNameList.add(new MedicineName(name));
        }

        Future<?> future = executorService.submit(() -> {
            try {
                medicineNameDao.insertAll(medicineNameList);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        try {
            future.get(); // insertAll이 끝날 때까지 기다림
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void delete(MedicineName medicineName) {
        executorService.execute(() -> medicineNameDao.delete(medicineName));
    }

    public void update(MedicineName medicineName) {
        executorService.execute(() -> medicineNameDao.update(medicineName));
    }

    public List<MedicineName> getAllMedicineNames() {
        return medicineNameDao.getAllMedicineNames();
    }



}
