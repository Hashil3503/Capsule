package com.example.myapplication;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.util.Date;
//엔티티 선언 파일 : 처방전 엔티티
@Entity(tableName = "prescription")
public class Prescription {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private Date date;
    private int duration;

    public Prescription(Date date) {
        this.date = date;
    }

    // Getter & Setter
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }
}