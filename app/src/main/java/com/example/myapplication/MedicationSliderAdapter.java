package com.example.myapplication;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.view.ViewGroup;
import android.view.LayoutInflater;

import java.util.List;

public class MedicationSliderAdapter extends RecyclerView.Adapter<MedicationSliderAdapter.ViewHolder> {
    private List<Medication> medications;

    public MedicationSliderAdapter(List<Medication> medications) {
        this.medications = medications;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) { //의약품 개별 정보를 표시할 카드 레이아웃 생성
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_medication_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) { //position은 ViewPage2가 현재 화면에 표시하려는 의약품의 인덱스 번호.
        //onBindViewHolder는 화면에 표시하려는 데이터를 ViewHolder에 연결하는 메서드
        Medication medication = medications.get(position);
        holder.bind(medication);
    }

    @Override
    public int getItemCount() {
        return medications.size();
    } //의약품 개수 계산. getItemCount() 메서드는 총 몇개의 페이지를 생성할지 결정하는 RecyclerView.Adapter 추상 클래스의 추상 메서드

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, effectText, textEffect;

        ViewHolder(View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.medicationName);
            effectText = itemView.findViewById(R.id.medicationEffect);
            textEffect = itemView.findViewById(R.id.textEffect);
        }

        void bind(Medication medication) { //onBindViewHolder가 전달한 medication을 사용해 의약품 카드의 내용을 채움.
            if (medication.getEffects() == null || medication.getEffects().isEmpty()) {
                // 복용 중인 처방전이 없는 경우
                nameText.setText(medication.getName()); // "복용 중인 처방전이 없습니다" 문구
                effectText.setText("");
                textEffect.setText("");

            } else {
                // 의약품 정보가 있을 경우
                nameText.setText("< 약품명 >\n" + medication.getName());
                effectText.setText(medication.getEffects());
            }
        }
    }
}
