package com.example.myapplication;
import android.app.Application;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

//Dao와 UI를 분리하여 코드 유지보수를 원활하게 하기 위한 Repository (데이터베이스 구조 변경시에 Activity 코드 수정 최소화)
public class MedicationRepository {
    private final MedicationDao medicationDao;
    private final ExecutorService executorService;

    public MedicationRepository(Application application) {
        DB db = DB.getInstance(application); //DB 싱글톤 인스턴스 가져오기
        medicationDao = db.medicationDao(); //prescriptiondao 객체 가져오기
        executorService = Executors.newSingleThreadExecutor(); //데이터베이스 작업을 백그라운드 스레드에서 실행
    }

    public long insert(Medication medication) { //long으로 선언한 이유는 관계성 테이블 생성이 용이하도록 테이블에 레코드를 추가할 시 해당 id를 반환하기 위함.
        Future<Long> future = executorService.submit(() -> medicationDao.insert(medication)); //Future<타입>
        try {
            return future.get(); // 백그라운드 작업 완료 후 ID 반환
        } catch (Exception e) { //오류 발생 감지
            e.printStackTrace(); //오류 발생시 오류 원인을 출력
            return -1; // 오류 발생 시 기본값 반환
        }
    }


    public void delete(Medication medication) {
        executorService.execute(() -> medicationDao.delete(medication));
    }

    public void update(Medication medication) {
        executorService.execute(() -> medicationDao.update(medication));
    }

    public List<Medication> getAllMedications() {
        return medicationDao.getAllMedications();
    }

    public Medication getMedicationById(long medicationId) {
        return medicationDao.getMedicationById(medicationId);
    }

    public void deleteMedicationById(long medicationId) {
        executorService.execute(() -> medicationDao.deleteMedicationById(medicationId));
    }

}
