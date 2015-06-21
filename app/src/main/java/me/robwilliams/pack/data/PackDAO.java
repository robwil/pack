package me.robwilliams.pack.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

public class PackDAO {
    private SQLiteDatabase db;
    private DatabaseHelper dbHelper;

    public PackDAO(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    public void open() {
        db = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public List<String> getAllListNames() {
        List<String> listNames = new ArrayList<>();
        String[] columns = {"name"};
        Cursor cursor = db.query("list", columns, null, null, null, null, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            listNames.add(cursor.getString(0));
            cursor.moveToNext();
        }
        cursor.close();
        return listNames;
    }

    public void createList(String name, int weight) {
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("weight", weight);
        db.insert("list", null, values);
    }

}
