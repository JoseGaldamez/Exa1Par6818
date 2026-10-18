package dev.josegaldamez.exa1par6818.dao;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/** Versioned local database. Future schema migrations belong in onUpgrade. */
public final class NotesDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "uth_notes.db";
    private static final int DATABASE_VERSION = 1;

    public NotesDatabaseHelper(Context context) {
        super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE notes ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "subject TEXT NOT NULL CHECK(length(trim(subject)) > 0),"
                + "topic TEXT NOT NULL CHECK(length(trim(topic)) > 0),"
                + "note_date TEXT NOT NULL,"
                + "description TEXT NOT NULL CHECK(length(trim(description)) > 0),"
                + "audio_path TEXT NOT NULL UNIQUE,"
                + "duration_ms INTEGER NOT NULL CHECK(duration_ms >= 0),"
                + "created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX notes_subject_date ON notes(subject, note_date DESC, _id DESC)");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Version 1 is the initial schema. Add non-destructive migrations when incrementing it.
        throw new IllegalStateException("Missing migration from " + oldVersion + " to " + newVersion);
    }
}
