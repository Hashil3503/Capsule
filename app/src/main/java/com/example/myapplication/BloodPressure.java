package com.example.myapplication;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.util.Date;

@Entity(tableName = "bloodpressure")
public class BloodPressure {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private Date date;
    private int systolic; //수축기
    private int diastolic; //이완기
    public BloodPressure(Date date) { this.date = date; }

    // Getter & Setter
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }

    public int getSystolic() { return systolic; }
    public void setSystolic(int systolic) { this.systolic = systolic; }

    public int getDiastolic() { return diastolic; }
    public void setDiastolic(int diastolic) { this.diastolic = diastolic; }

}