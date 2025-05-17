package com.example.myapplication;
import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Date;

//엔티티 선언 파일 : 의약품 엔티티
@Entity(tableName = "medicinetable")
public class MedicineTable {
    @PrimaryKey
    @NonNull
    private String name;
    private String ingredients;
    private String appearance;
    private String effects;
    private String caution;
    private String memo; //입력용
    private String sideeffct; //입력용
    @ColumnInfo(defaultValue = "0")
    private boolean se_existence; //입력용

    public MedicineTable(){} //기본 생성자
    @Ignore
    public MedicineTable(String name) {
        this.name = name;
    } //생성자

    // Getter & Setter
    @NonNull
    public String getName() { return name; }
    public void setName(@NonNull String name) { this.name = name; }

    public String getIngredients() { return ingredients; }
    public void setIngredients(String ingredients) { this.ingredients = ingredients; }

    public String getAppearance() { return appearance; }
    public void setAppearance(String appearance) { this.appearance = appearance; }

    public String getEffects() { return effects; }
    public void setEffects(String effects) { this.effects = effects; }

    public String getCaution() { return caution; }
    public void setCaution(String caution) { this.caution = caution; }

    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }

    public String getSideeffct() { return sideeffct; }
    public void setSideeffct(String sideeffct) { this.sideeffct = sideeffct; }

    public boolean getSe_existence() { return se_existence; }
    public void setSe_existence(boolean se_existence) { this.se_existence = se_existence; }

}