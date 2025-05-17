package com.example.myapplication;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;
// SQL 쿼리를 위한 Dao 파일
@Dao
public interface PrescriptionDao {
    @Insert
    long insert(Prescription prescription);

    @Update
    void update(Prescription prescription);

    @Delete
    void delete(Prescription prescription);

    @Query("SELECT * FROM prescription")
    List<Prescription> getAllPrescriptions();

    @Query("SELECT * FROM prescription WHERE id = :prescriptionId")
    Prescription getPrescriptionById(long prescriptionId);

    @Query("DELETE FROM prescription WHERE id = :prescriptionId")
    void deletePrescriptionById(long prescriptionId);

    @Query("SELECT * FROM prescription WHERE date(datetime(date/1000, 'unixepoch', '+' || duration || ' days')) >= datetime('now') ORDER BY duration DESC LIMIT 1")
    Prescription getOldestActivePrescription();
    //처방전 등록일 + 복용일수 = 종료일. 종료일이 아직 지나지 않은 처방전을 복용일수 내림차순으로 정렬하고 맨 위에 뜨는 레코드 하나만 조회.
}