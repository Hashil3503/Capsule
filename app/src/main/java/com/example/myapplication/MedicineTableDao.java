package com.example.myapplication;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;
// SQL 쿼리를 위한 Dao 파일
@Dao
public interface MedicineTableDao {
    @Insert
    long insert(MedicineTable medicationtable);

    @Update
    void update(MedicineTable medicinetable);

    @Delete
    void delete(MedicineTable medicinetable);

    @Query("SELECT * FROM medicinetable")
    List<MedicineTable> getAllMedicineTables();

    @Query("SELECT * FROM medicinetable WHERE name = :medicinename")
    MedicineTable getMedicationByName(String medicinename);

    @Query("DELETE FROM medicinetable WHERE name = :medicinename")
    void deleteMedicationByName(String medicinename);

    @Query("DELETE FROM MedicineTable")
    void deleteAll();
}