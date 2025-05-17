package com.example.myapplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "DatabaseHelper";
    private static final String DATABASE_NAME = "medicines.db";
    private static final int DATABASE_VERSION = 4;
    private static final String TABLE_MEDICINES = "medicines";
    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_PRODUCT_NAME = "product_name";
    private static final String COLUMN_NAME = "name";

    private static final String CREATE_TABLE_MEDICINES = "CREATE TABLE IF NOT EXISTS " + TABLE_MEDICINES + " (" +
            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_PRODUCT_NAME + " TEXT, " +
            COLUMN_NAME + " TEXT)";

    private static DatabaseHelper instance;
    private SQLiteDatabase database;

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL(CREATE_TABLE_MEDICINES);
            Log.i(TAG, "데이터베이스 테이블 생성 완료");
        } catch (Exception e) {
            Log.e(TAG, "테이블 생성 중 오류", e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        try {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_MEDICINES);
            onCreate(db);
            Log.i(TAG, "데이터베이스 업그레이드 완료");
        } catch (Exception e) {
            Log.e(TAG, "데이터베이스 업그레이드 중 오류", e);
        }
    }

    public synchronized void insertMedicines(List<String> medicines) {
        SQLiteDatabase db = null;
        try {
            db = getWritableDatabase();
            db.beginTransaction();

            // 테이블 초기화
            db.delete(TABLE_MEDICINES, null, null);

            // 데이터 삽입
            for (String medicine : medicines) {
                ContentValues values = new ContentValues();
                values.put(COLUMN_PRODUCT_NAME, medicine);
                values.put(COLUMN_NAME, medicine);
                db.insert(TABLE_MEDICINES, null, values);
            }

            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e("DatabaseHelper", "데이터 삽입 중 오류 발생", e);
        } finally {
            if (db != null) {
                try {
                    db.endTransaction();
                } catch (Exception e) {
                    Log.e("DatabaseHelper", "트랜잭션 종료 중 오류 발생", e);
                }
            }
        }
    }

    public synchronized List<String> searchMedicines(String query) {
        List<String> results = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = getReadableDatabase();
            String normalizedQuery = query.replaceAll("[^가-힣a-zA-Z0-9]", "").toLowerCase();
            String selection = COLUMN_PRODUCT_NAME + " LIKE ?";
            String[] selectionArgs = new String[]{"%" + normalizedQuery + "%"};

            cursor = db.query(TABLE_MEDICINES,
                    new String[]{COLUMN_PRODUCT_NAME},
                    selection,
                    selectionArgs,
                    null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    results.add(cursor.getString(0));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "검색 중 오류 발생", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return results;
    }

    public synchronized List<String> getAllMedicines() {
        List<String> medicines = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = getReadableDatabase();
            cursor = db.query(TABLE_MEDICINES,
                    new String[]{COLUMN_PRODUCT_NAME},
                    null, null, null, null, COLUMN_PRODUCT_NAME);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    medicines.add(cursor.getString(0));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "전체 약품 목록 조회 중 오류 발생", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return medicines;
    }

    @Override
    public synchronized void close() {
        super.close();
        if (database != null && database.isOpen()) {
            database.close();
            database = null;
        }
    }
} 