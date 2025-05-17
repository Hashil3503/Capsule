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

}

