package com.example.myapplication;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;
// SQL 쿼리를 위한 Dao 파일
@Dao
public interface BloodPressureDao {
    @Insert
    long insert(BloodPressure bloodpressure);

    @Update
    void update(BloodPressure bloodpressure);

    @Delete
    void delete(BloodPressure bloodpressure);

    @Query("SELECT * FROM bloodpressure ORDER BY date ASC")
    List<BloodPressure> getAllBloodPressures();

    @Query("SELECT * FROM bloodpressure WHERE id = :id")
    BloodPressure getBloodPressureById(long id);

    //선택한 YYYY-MM의 레코드 목록 조회
    //strftime('%Y-%m', date / 1000, 'unixepoch') 메서드를 통해서 date를 문자열로 변환하여 yearMonth 문자열과 비교함.
    @Query("SELECT * FROM bloodpressure WHERE strftime('%Y-%m', date / 1000, 'unixepoch') = :yearMonth ORDER BY date ASC")
    List<BloodPressure> getByMonth(String yearMonth);
}
