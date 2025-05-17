package com.example.myapplication;
import android.app.Application;
import android.util.Log;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

//Dao와 UI를 분리하여 코드 유지보수를 원활하게 하기 위한 Repository (데이터베이스 구조 변경시에 Activity 코드 수정 최소화)
public class PrescriptionRepository {
    private final PrescriptionDao prescriptionDao;
    private final ExecutorService executorService;

    public PrescriptionRepository(Application application) {
        DB db = DB.getInstance(application); //DB 싱글톤 인스턴스 가져오기
        prescriptionDao = db.prescriptionDao(); //prescriptiondao 객체 가져오기
        executorService = Executors.newSingleThreadExecutor(); //데이터베이스 작업을 백그라운드 스레드에서 실행
    }

    public long insert(Prescription prescription) { //long으로 선언한 이유는 관계성 테이블 생성이 용이하도록 prescription 테이블에 레코드를 추가할 시 해당 id를 반환하기 위함. 기본적으로 insert 어노테이션을 long타입으로 반환할 경우 PK를 반환한다.
        Future<Long> future = executorService.submit(() -> prescriptionDao.insert(prescription));  //executorService.submit()과 executorService.execute()의 차이점 : 전자는 실행 후 결과를 반환, 후자는 실행 후 결과를 반환하지 않음.
        //  Future<타입> : 지금 당장은 결과를 받을 수 없지만, 작업이 완료되면 결과를 받을 수 있는 객체.
        try {
            return future.get(); // 백그라운드 작업 완료 후 ID 반환. 작업이 끝난 뒤 결과 값을 get()으로 가져와서 반환함.
        } catch (Exception e) {
            e.printStackTrace();
            return -1; // 오류 발생 시 기본값 반환
        }
    }

    public boolean delete(Prescription prescription) {
        executorService.execute(() -> prescriptionDao.delete(prescription));
        Log.d("처방전레파지터리", "delete 호출됨");
        return true;
    }

    public void update(Prescription prescription) {
        executorService.execute(() -> prescriptionDao.update(prescription));
    }

    public List<Prescription> getAllPrescriptions() {
        List<Prescription> result = prescriptionDao.getAllPrescriptions();

        Log.d("PrescriptionRepo", "getAllPrescriptions() called. Count: " + result.size());

        for (Prescription p : result) {
            Log.d("PrescriptionRepo", "Prescription ID: " + p.getId() + ", Date: " + p.getDate());
        }

        return result;
    }

    public Prescription getPrescriptionById(long prescriptionId) {
        return prescriptionDao.getPrescriptionById(prescriptionId);
    }

    public void deletePrescriptionById(long prescriptionId) {
        executorService.execute(() -> prescriptionDao.deletePrescriptionById(prescriptionId));
    }

    public Prescription getOldestActivePrescription(){
        return prescriptionDao.getOldestActivePrescription();
    }
}
