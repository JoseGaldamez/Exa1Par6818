package dev.josegaldamez.exa1par6818.controllers;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dev.josegaldamez.exa1par6818.R;
import dev.josegaldamez.exa1par6818.dao.AudioRecordingDao;
import dev.josegaldamez.exa1par6818.dao.NoteDao;
import dev.josegaldamez.exa1par6818.models.AudioRecording;
import dev.josegaldamez.exa1par6818.models.Note;
import dev.josegaldamez.exa1par6818.models.NoteDraft;
import dev.josegaldamez.exa1par6818.models.NoteOperation;

/** Coordinates SQLite and audio files. Database/file work never runs on the UI thread. */
public final class NotesController extends AndroidViewModel {
    private final NoteDao noteDao;
    private final AudioRecordingDao audioDao;
    private final SavedStateHandle savedState;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final MutableLiveData<List<Note>> notes = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<Note> editingNote = new MutableLiveData<>();
    private final MutableLiveData<Boolean> busy = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> errors = new MutableLiveData<>();
    private final MutableLiveData<NoteOperation> operation = new MutableLiveData<>();
    private int queryVersion;
    private boolean cleared;

    public NotesController(@NonNull Application app, SavedStateHandle savedState) {
        super(app);
        this.savedState = savedState;
        noteDao = new NoteDao(app);
        audioDao = new AudioRecordingDao(app);
    }

    public LiveData<List<Note>> notes() { return notes; }
    public LiveData<Note> editingNote() { return editingNote; }
    public LiveData<Boolean> busy() { return busy; }
    public LiveData<Integer> errors() { return errors; }
    public LiveData<NoteOperation> operations() { return operation; }
    public boolean isBusy() { return Boolean.TRUE.equals(busy.getValue()); }
    public void consumeError() { errors.setValue(null); }
    public void consumeOperation() { operation.setValue(null); }
    public String getFilter() { return savedState.get("subject_filter"); }
    public long getSelectedId() {
        Long id = savedState.get("selected_id");
        return id == null ? -1 : id;
    }
    public void select(long id) { savedState.set("selected_id", id); }

    public void loadNotes(String subject) {
        savedState.set("subject_filter", subject);
        int version = ++queryVersion;
        busy.setValue(true);
        worker.execute(() -> {
            try {
                List<Note> result = noteDao.findBySubject(subject);
                deliver(() -> {
                    if (version != queryVersion) return;
                    notes.setValue(result);
                    busy.setValue(false);
                });
            } catch (RuntimeException exception) {
                deliver(() -> {
                    if (version != queryVersion) return;
                    busy.setValue(false);
                    errors.setValue(R.string.notes_load_error);
                });
            }
        });
    }

    public void loadEditor(long id) {
        if (editingNote.getValue() != null || isBusy()) return;
        busy.setValue(true);
        worker.execute(() -> {
            try {
                Note note = noteDao.findById(id);
                deliver(() -> {
                    if (note != null) editingNote.setValue(note);
                    busy.setValue(false);
                    if (note == null) operation.setValue(new NoteOperation(NoteOperation.Type.MISSING, null));
                });
            } catch (RuntimeException exception) {
                fail(R.string.notes_load_error);
            }
        });
    }

    public void save(long id, NoteDraft draft, AudioRecording audio) {
        if (isBusy()) return;
        NoteFormController validation = new NoteFormController(getApplication().getResources()
                .getStringArray(R.array.subjects));
        if (!validation.validate(draft).isEmpty()) {
            errors.setValue(R.string.invalid_note);
            return;
        }
        if (id < 0 && audio == null) {
            errors.setValue(R.string.audio_required);
            return;
        }
        busy.setValue(true);
        worker.execute(() -> {
            AudioRecording copy = null;
            try {
                Note saved;
                if (id < 0) {
                    copy = audioDao.copyForNote(audio);
                    saved = noteDao.insert(draft, copy);
                } else saved = noteDao.update(id, draft);
                deliver(() -> {
                    busy.setValue(false);
                    operation.setValue(new NoteOperation(NoteOperation.Type.SAVED, saved));
                });
            } catch (Exception exception) {
                if (copy != null) audioDao.delete(copy.getFile());
                fail(R.string.note_save_error);
            }
        });
    }

    public void delete(Note selected) {
        if (isBusy()) return;
        busy.setValue(true);
        worker.execute(() -> {
            try {
                // Read the current path from SQLite instead of trusting a stale screen selection.
                Note current = noteDao.findById(selected.id);
                if (current == null || !noteDao.delete(selected.id)) throw new IllegalStateException("Missing note");
                audioDao.delete(new java.io.File(current.audioPath));
                deliver(() -> {
                    busy.setValue(false);
                    select(-1);
                    operation.setValue(new NoteOperation(NoteOperation.Type.DELETED, current));
                });
            } catch (RuntimeException exception) {
                fail(R.string.note_delete_error);
            }
        });
    }

    private void fail(int message) {
        deliver(() -> {
            busy.setValue(false);
            errors.setValue(message);
        });
    }

    private void deliver(Runnable update) { main.post(() -> { if (!cleared) update.run(); }); }

    @Override protected void onCleared() {
        cleared = true;
        worker.execute(noteDao::close);
        worker.shutdown();
    }
}
