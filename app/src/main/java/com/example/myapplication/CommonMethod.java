package com.example.myapplication;

public class CommonMethod { //자주 쓰일 것 같은 메서드 모아둠
    public static int parseInteger(String value) { //문자열을 int형으로 전환
        try {
            return Integer.parseInt(value.trim()); // 공백 제거 후 숫자로 변환
        } catch (NumberFormatException e) {
            return 0; // 변환할 수 없는 경우 기본값 0 반환
        }
    }

    public static float parseFloat(String value) { //문자열을 int형으로 전환
        try {
            return Float.parseFloat(value.trim()); // 공백 제거 후 숫자로 변환
        } catch (NumberFormatException e) {
            return 0; // 변환할 수 없는 경우 기본값 0 반환
        }
    }

    public static String normalizeWord(String text) {
        if (text == null) return "";

        // 1. 기본 정리: 앞뒤 공백 제거
        text = text.trim();

        // 2. 숫자+밀리그람/mg 단위 제거
        text = text.replaceAll("\\d+\\s*(밀리그람|밀리그램|mg|MG)", "").trim();

        // 3. 괄호로 시작해 괄호로 끝나는 문자열 제거 후, 괄호가 열린채로 끊긴 문자열 제거.
        text = text.replaceAll("\\([^\\)]*\\)", "").trim();
        text = text.replaceAll("[\\(].*$", "").trim();

        // 4. 제형 키워드 중 가장 먼저 등장하는 위치 찾기
        String[] forms = {"정제", "정", "캡슐", "셀", "시럽", "연고", "크림", "가루", "연고", "액", "환", "과립", "파스", "패치", "필름", "주사제", "주사", "젤"};
        int earliestIdx = text.length();
        String selectedForm = null;

        for (String form : forms) {
            int idx = text.indexOf(form);
            if (idx != -1 && idx < earliestIdx) {
                earliestIdx = idx;
                selectedForm = form;
            }
        }

        // 4. 제형 기준으로 자르기
        if (selectedForm != null) {
            int cutIdx = earliestIdx + selectedForm.length();
            text = text.substring(0, Math.min(cutIdx, text.length())).trim();
        } else {
            text = text.trim();
        }

        if (text.length() < 2) return "";

        return text;
    }

}

