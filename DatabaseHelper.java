package edu.buffalo.cse.cse486586.groupmessenger2;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by shadkhan on 21/02/17.
 */

public class DatabaseHelper extends SQLiteOpenHelper {

    // code snippet from https://developer.android.com/guide/topics/data/data-storage.html

    private static final String KEY_WORD = "key";
    private static final String KEY_DEFINITION = "value";
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "dictionary";
    private static final String DICTIONARY_TABLE_CREATE =
            "CREATE TABLE " + DATABASE_NAME + " (" +
                    KEY_WORD + " TEXT, " +
                    KEY_DEFINITION + " TEXT);";

    DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS "+DATABASE_NAME);
        db.execSQL(DICTIONARY_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        db.execSQL("DROP TABLE IF EXISTS "+DATABASE_NAME);
        db.execSQL(DICTIONARY_TABLE_CREATE);
    }

}
