package com.example.myapplication;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;
// SQL 쿼리를 위한 Dao 파일
@Dao
public interface BloodSugarDao {
    @Insert
    long insert(BloodSugar bloodsugar);

    @Update
    void update(BloodSugar bloodsugar);

    @Delete
    void delete(BloodSugar bloodsugar);

    @Query("SELECT * FROM bloodsugar ORDER BY date ASC")
    List<BloodSugar> getAllBloodSugars();
    @Query("SELECT * FROM bloodsugar WHERE id = :id")
    BloodSugar getBloodSugarById(long id);

    //선택한 YYYY-MM의 레코드 목록 조회
    @Query("SELECT * FROM bloodsugar WHERE strftime('%Y-%m', date / 1000, 'unixepoch') = :yearMonth ORDER BY date ASC")
    List<BloodSugar> getByMonth(String yearMonth);
}