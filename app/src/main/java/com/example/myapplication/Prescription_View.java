package com.example.myapplication;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ForeignKey;
import androidx.room.Index;

//엔티티 선언 파일 : 처방전&의약품 관계성을 나타내는 엔티티

//외래키 사용법
//@ForeignKey(
//    entity = 참조할_엔티티.class,  // 참조할 테이블 (부모 테이블)
//    parentColumns = "부모테이블의_PK", // 참조할 부모 테이블의 기본 키 컬럼
//    childColumns = "현재테이블의_FK", // 현재 테이블의 외래 키 컬럼
//    onDelete = ForeignKey.CASCADE // 부모가 삭제될 때, 같이 삭제됨
//)
@Entity(
        tableName = "prescription_view",
        foreignKeys = {
                @ForeignKey(entity = Prescription.class,
                        parentColumns = "id",
                        childColumns = "prescription_id",
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(entity = Medication.class,
                        parentColumns = "id",
                        childColumns = "medication_id",
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = { @Index(value = "prescription_id"), @Index(value = "medication_id") }
)
public class Prescription_View {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private long prescription_id;
    private long medication_id;

    public Prescription_View() {}

    public Prescription_View(long prescription_id, long medication_id) {
        this.prescription_id = prescription_id;
        this.medication_id = medication_id;
    }

    // Getter & Setter
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getPrescription_id() { return prescription_id; }
    public void setPrescription_id(long prescription_id) { this.prescription_id = prescription_id; }

    public long getMedication_id() { return medication_id; }
    public void setMedication_id(long medication_id) { this.medication_id = medication_id; }

}