package com.example.myapplication;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.util.Date;

@Entity(tableName = "bloodsugar")
public class BloodSugar {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private Date date;
    private float value;
    @ColumnInfo(defaultValue = "0") // empty_stomach의 기본값을 0으로 설정.
    private boolean empty_stomach;

    public BloodSugar(Date date) { this.date = date; }

    // Getter & Setter
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }

    public float getValue() { return value; }
    public void setValue(float value) { this.value = value ;}

    public boolean getEmpty_stomach() { return empty_stomach; }
    public void setEmpty_stomach(boolean empty_stomach) { this.empty_stomach = empty_stomach;}
}