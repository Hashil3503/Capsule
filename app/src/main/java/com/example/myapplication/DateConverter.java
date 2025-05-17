package com.example.myapplication;

import androidx.room.TypeConverter;
import java.util.Date;

//Room이 Date타입을 직접 저장하지 못해서 변환 과정을 거쳐야함. date->long->date로 변환은 되는데 long->date->long 변환은 왜 안된다는건지 모르겠음.
public class DateConverter {
    @TypeConverter
    public static Date fromTimestamp(Long value) { //Long 타입의 값을 Date 객체로 변환
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public static Long dateToTimestamp(Date date) { //Date 객체를 Long 타입의 값으로 변환
        return date == null ? null : date.getTime();
    }
}

