package com.example.myapplication;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Date;

//엔티티 선언 파일 : 의약품 엔티티
@Entity(tableName = "medication")
public class Medication {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String name;
    private String ingredients;
    private String appearance;
    private String effects;
    private String caution;
    private int dosage; // 입렬용
    private int frequency; //입력용
    //private int duration; 원래 의약품에서 관리하려고 했는데 처방전에서 관리하는 걸로 수정함.
    private String memo; //입력용
    private String sideeffct; //입력용
    @ColumnInfo(defaultValue = "0")
    private boolean se_existence; //입력용

    public Medication(){} //기본 생성자
    @Ignore
    public Medication(String name) {
        this.name = name;
    } //생성자

    // Getter & Setter
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIngredients() { return ingredients; }
    public void setIngredients(String ingredients) { this.ingredients = ingredients; }

    public String getAppearance() { return appearance; }
    public void setAppearance(String appearance) { this.appearance = appearance; }

    public String getEffects() { return effects; }
    public void setEffects(String effects) { this.effects = effects; }

    public String getCaution() { return caution; }
    public void setCaution(String caution) { this.caution = caution; }

    public int getDosage() { return dosage; }
    public void setDosage(int dosage) { this.dosage = dosage; }

    public int getFrequency() { return frequency; }
    public void setFrequency(int frequency) { this.frequency = frequency; }

    //public int getDuration() { return duration; } 원래 의약품에서 관리하려고 했는데 처방전에서 관리하는 걸로 수정함.
    //public void setDuration(int duration) { this.duration = duration; } 원래 의약품에서 관리하려고 했는데 처방전에서 관리하는 걸로 수정함.

    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }

    public String getSideeffct() { return sideeffct; }
    public void setSideeffct(String sideeffct) { this.sideeffct = sideeffct; }

    public boolean getSe_existence() { return se_existence; }
    public void setSe_existence(boolean se_existence) { this.se_existence = se_existence; }


}