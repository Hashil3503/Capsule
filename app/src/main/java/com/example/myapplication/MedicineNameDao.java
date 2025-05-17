package com.example.myapplication;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;
// SQL 쿼리를 위한 Dao 파일
@Dao
public interface MedicineNameDao {
    @Insert
    long insert(MedicineName medicineName);

    @Update
    void update(MedicineName medicineName);

    @Delete
    void delete(MedicineName medicineName);

    @Query("SELECT * FROM medicineName")
    List<MedicineName> getAllMedicineNames();

    @Insert
    void insertAll(List<MedicineName> medicineNameList);

}