package com.example.myapplication;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;


import java.util.List;

@Dao
public interface Prescription_ViewDao {
    // 새로운 Prescription_View 추가
    @Insert
    void insert(Prescription_View prescription_view);

    // 반환을 int형으로 하는 이유는 db에서 실제로 레코드가 삭제되어 삭제된 레코드 수를 반환할 때 까지 기다리기 위함.
    @Query("DELETE FROM prescription_view WHERE prescription_id = :prescriptionId AND medication_id = :medicationId")
    int deleteByAllKey(long prescriptionId, long medicationId);

    // 특정 처방전과 관련된 약물 리스트 조회
    @Query("SELECT * FROM prescription_view WHERE prescription_id = :prescriptionId")
    List<Prescription_View> getMedicationsForPrescription(long prescriptionId);

    // 특정 약물을 포함한 처방전 조회
    @Query("SELECT * FROM prescription_view WHERE medication_id = :medicationId")
    List<Prescription_View> getPrescriptionsForMedication(long medicationId);
}
