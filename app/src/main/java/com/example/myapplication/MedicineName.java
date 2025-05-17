package com.example.myapplication;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

//엔티티 선언 파일 : 의약품 엔티티
@Entity(tableName = "medicineName")
public class MedicineName {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String name;

    public MedicineName() {}

    public MedicineName(String name) {
        this.name = name;
    } //생성자

    // Getter & Setter
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }


}