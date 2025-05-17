package com.example.myapplication;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;
// SQL 쿼리를 위한 Dao 파일
@Dao
public interface MedicationDao {
    @Insert
    long insert(Medication medication);

    @Update
    void update(Medication medication);

    @Delete
    void delete(Medication medication);

    @Query("SELECT * FROM medication")
    List<Medication> getAllMedications();

    @Query("SELECT * FROM medication WHERE id = :medicationId")
    Medication getMedicationById(long medicationId);

    @Query("DELETE FROM medication WHERE id = :medicationId")
    void deleteMedicationById(long medicationId);

}