package dev.josegaldamez.exa1par6818.dao;

import android.content.Context;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

/** Small persistent catalog shared by the editor, validation and note filters. */
public final class SubjectDao implements AutoCloseable {
    private final NotesDatabaseHelper helper;

    public SubjectDao(Context context) {
        helper = new NotesDatabaseHelper(context);
    }

    public String[] findAll() {
        List<String> subjects = new ArrayList<>();
        try (Cursor cursor = helper.getReadableDatabase().query("subjects", new String[]{"name"},
                null, null, null, null, "rowid ASC")) {
            while (cursor.moveToNext()) subjects.add(cursor.getString(0));
        }
        return subjects.toArray(new String[0]);
    }

    public boolean add(String name) {
        String trimmed = name.trim();
        if (trimmed.isEmpty()) return false;
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            for (String subject : findAll()) {
                if (subject.equalsIgnoreCase(trimmed)) return false;
            }
            ContentValues values = new ContentValues();
            values.put("name", trimmed);
            boolean added = db.insertWithOnConflict("subjects", null, values,
                    SQLiteDatabase.CONFLICT_IGNORE) != -1;
            db.setTransactionSuccessful();
            return added;
        } finally {
            db.endTransaction();
        }
    }

    @Override public void close() { helper.close(); }
}
