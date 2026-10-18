package dev.josegaldamez.exa1par6818.dao;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import dev.josegaldamez.exa1par6818.R;

/** Versioned local database. Future schema migrations belong in onUpgrade. */
public final class NotesDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "uth_notes.db";
    private static final int DATABASE_VERSION = 2;
    private final String initialSubject;

    public NotesDatabaseHelper(Context context) {
        super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
        initialSubject = context.getResources().getStringArray(R.array.subjects)[0];
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
        createSubjects(db);
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            createSubjects(db);
            db.execSQL("INSERT OR IGNORE INTO subjects(name) SELECT DISTINCT subject FROM notes");
        }
    }

    private void createSubjects(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE subjects (name TEXT NOT NULL PRIMARY KEY COLLATE NOCASE "
                + "CHECK(length(trim(name)) > 0))");
        db.execSQL("INSERT INTO subjects(name) VALUES (?)", new Object[]{initialSubject});
    }
}
