package com.example.myapplication;

import java.util.Calendar;

public class AlarmItem {
    private Calendar dateTime;
    private int requestCode;
    private boolean isTaken = false;  // 🔸 복용 완료 여부

    private int pid;

    public AlarmItem() {
        // Gson 역직렬화용 기본 생성자
    }

    public AlarmItem(Calendar dateTime, int requestCode) {
        this.dateTime = dateTime;
        this.requestCode = requestCode;
        this.isTaken = false;
    }

    public Calendar getDateTime() {
        return dateTime;
    }

    public int getRequestCode() {
        return requestCode;
    }

    public boolean isTaken() {
        return isTaken;
    }

    public void setTaken(boolean taken) {
        isTaken = taken;
    }

    public int getPid() { return pid; }

    public void setPid(int pid) {
        this.pid = pid;
    }
}
