package com.example.myapplication;

import android.app.Application;
import androidx.lifecycle.LiveData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Prescription_ViewRepository {
    private final Prescription_ViewDao prescription_viewDao;
    private final ExecutorService executorService;

    public Prescription_ViewRepository(Application application) {
        DB database = DB.getInstance(application);
        prescription_viewDao = database.prescription_viewDao();
        executorService = Executors.newSingleThreadExecutor();
    }

    public void insert(Prescription_View prescriptionView) {
        executorService.execute(() -> prescription_viewDao.insert(prescriptionView));
    }

    // 특정 처방전에 대한 약물 리스트 조회
    public List<Prescription_View> getMedicationsForPrescription(long prescriptionId) {
        try {
            Future<List<Prescription_View>> future = executorService.submit(() ->
                    prescription_viewDao.getMedicationsForPrescription(prescriptionId)
            );
            return future.get(); // 동기적으로 결과 받음
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }


    // 특정 약물을 포함한 처방전 조회
    public List<Prescription_View> getPrescriptionsForMedication(long medicationId) {
        return prescription_viewDao.getPrescriptionsForMedication(medicationId);
    }

    // 반환을 int형으로 하는 이유는 db에서 실제로 레코드가 삭제되어 삭제된 레코드 수를 반환할 때 까지 기다리기 위함.
    public int deleteByAllKey(long prescriptionId, long medicationId) {
        try {
        Future<Integer> future = executorService.submit(() ->
                prescription_viewDao.deleteByAllKey(prescriptionId, medicationId)
        );
        return future.get(); // 삭제된 행 수 반환 (blocking)
    } catch (Exception e) {
        e.printStackTrace();
        return 0;
    }
    }
}
