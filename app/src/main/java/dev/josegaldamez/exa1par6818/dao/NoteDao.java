package dev.josegaldamez.exa1par6818.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import dev.josegaldamez.exa1par6818.models.AudioRecording;
import dev.josegaldamez.exa1par6818.models.Note;
import dev.josegaldamez.exa1par6818.models.NoteDraft;

/** Parameterized SQLite CRUD. Call from the controller's worker thread. */
public final class NoteDao implements AutoCloseable {
    private final NotesDatabaseHelper helper;

    public NoteDao(Context context) { helper = new NotesDatabaseHelper(context); }

    public Note insert(NoteDraft draft, AudioRecording audio) {
        long now = System.currentTimeMillis();
        ContentValues values = metadata(draft);
        values.put("audio_path", audio.getFile().getAbsolutePath());
        values.put("duration_ms", audio.getDurationMs());
        values.put("created_at", now);
        values.put("updated_at", now);
        long id = helper.getWritableDatabase().insertOrThrow("notes", null, values);
        return new Note(id, draft.subject, draft.topic, draft.date, draft.description,
                audio.getFile().getAbsolutePath(), audio.getDurationMs(), now, now);
    }

    public Note findById(long id) {
        try (Cursor cursor = helper.getReadableDatabase().query("notes", null, "_id = ?",
                new String[]{Long.toString(id)}, null, null, null)) {
            return cursor.moveToFirst() ? read(cursor) : null;
        }
    }

    public List<Note> findBySubject(String subject) {
        List<Note> notes = new ArrayList<>();
        String selection = subject == null ? null : "subject = ?";
        String[] arguments = subject == null ? null : new String[]{subject};
        try (Cursor cursor = helper.getReadableDatabase().query("notes", null,
                selection, arguments, null, null, "note_date DESC, _id DESC")) {
            while (cursor.moveToNext()) notes.add(read(cursor));
        }
        return notes;
    }

    /** Editing only changes text/date fields; the audio and creation time stay intact. */
    public Note update(long id, NoteDraft draft) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            if (db.update("notes", metadata(draft), "_id = ?", new String[]{Long.toString(id)}) != 1) {
                throw new IllegalStateException("Note no longer exists");
            }
            Note updated = findById(id);
            db.setTransactionSuccessful();
            return updated;
        } finally {
            db.endTransaction();
        }
    }

    public boolean delete(long id) {
        return helper.getWritableDatabase().delete("notes", "_id = ?",
                new String[]{Long.toString(id)}) == 1;
    }

    private ContentValues metadata(NoteDraft draft) {
        ContentValues values = new ContentValues();
        values.put("subject", draft.subject);
        values.put("topic", draft.topic);
        values.put("note_date", draft.date);
        values.put("description", draft.description);
        values.put("updated_at", System.currentTimeMillis());
        return values;
    }

    private Note read(Cursor cursor) {
        return new Note(cursor.getLong(cursor.getColumnIndexOrThrow("_id")),
                cursor.getString(cursor.getColumnIndexOrThrow("subject")),
                cursor.getString(cursor.getColumnIndexOrThrow("topic")),
                cursor.getString(cursor.getColumnIndexOrThrow("note_date")),
                cursor.getString(cursor.getColumnIndexOrThrow("description")),
                cursor.getString(cursor.getColumnIndexOrThrow("audio_path")),
                cursor.getLong(cursor.getColumnIndexOrThrow("duration_ms")),
                cursor.getLong(cursor.getColumnIndexOrThrow("created_at")),
                cursor.getLong(cursor.getColumnIndexOrThrow("updated_at")));
    }

    @Override public void close() { helper.close(); }
}
