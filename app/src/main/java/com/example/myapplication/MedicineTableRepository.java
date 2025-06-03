package com.example.myapplication;
import android.app.Application;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

//Dao와 UI를 분리하여 코드 유지보수를 원활하게 하기 위한 Repository (데이터베이스 구조 변경시에 Activity 코드 수정 최소화)
public class MedicineTableRepository {
    private final MedicineTableDao medicineTableDao;
    private final ExecutorService executorService;

    public MedicineTableRepository(Application application) {
        DB db = DB.getInstance(application); //DB 싱글톤 인스턴스 가져오기
        medicineTableDao = db.medicineTableDao(); //dao 객체 가져오기
        executorService = Executors.newSingleThreadExecutor(); //데이터베이스 작업을 백그라운드 스레드에서 실행
    }

    public void insert(MedicineTable medicineTable) {
        executorService.execute(() -> medicineTableDao.insert(medicineTable));
    }


    public void delete(MedicineTable medicineTable) {
        executorService.execute(() -> medicineTableDao.delete(medicineTable));
    }

    public void update(MedicineTable medicineTable) {
        executorService.execute(() -> medicineTableDao.update(medicineTable));
    }

    public List<MedicineTable> getAllMedicineTables() {
        return medicineTableDao.getAllMedicineTables();
    }

    public MedicineTable getMedicationByName(String medicinename) {
        return medicineTableDao.getMedicationByName(medicinename);
    }

    public void deleteMedicationByName(String medicinename) {
        executorService.execute(() -> medicineTableDao.deleteMedicationByName(medicinename));
    }

    public void deleteAllMedicineTables(){
        medicineTableDao.deleteAll();
    }

}
