package dev.josegaldamez.exa1par6818.controllers;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dev.josegaldamez.exa1par6818.R;
import dev.josegaldamez.exa1par6818.dao.SubjectDao;

/** Loads and creates subjects without blocking the UI on SQLite. */
public final class SubjectsController extends AndroidViewModel {
    private final SubjectDao dao;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final MutableLiveData<String[]> subjects = new MutableLiveData<>();
    private final MutableLiveData<Boolean> busy = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> errors = new MutableLiveData<>();
    private final MutableLiveData<String> created = new MutableLiveData<>();
    private boolean cleared;

    public SubjectsController(@NonNull Application app) {
        super(app);
        dao = new SubjectDao(app);
        reload();
    }

    public LiveData<String[]> subjects() { return subjects; }
    public LiveData<Boolean> busy() { return busy; }
    public LiveData<Integer> errors() { return errors; }
    public LiveData<String> created() { return created; }
    public void consumeError() { errors.setValue(null); }
    public void consumeCreated() { created.setValue(null); }

    public void reload() { run(null); }

    public void create(String name) {
        if (name.trim().isEmpty()) {
            errors.setValue(R.string.required_error);
            return;
        }
        run(name.trim());
    }

    private void run(String name) {
        if (Boolean.TRUE.equals(busy.getValue())) return;
        busy.setValue(true);
        worker.execute(() -> {
            try {
                boolean added = name == null || dao.add(name);
                String[] result = dao.findAll();
                deliver(() -> {
                    subjects.setValue(result);
                    busy.setValue(false);
                    if (!added) errors.setValue(R.string.subject_exists);
                    else if (name != null) created.setValue(name);
                });
            } catch (RuntimeException exception) {
                deliver(() -> {
                    busy.setValue(false);
                    errors.setValue(R.string.subject_storage_error);
                });
            }
        });
    }

    private void deliver(Runnable update) { main.post(() -> { if (!cleared) update.run(); }); }

    @Override protected void onCleared() {
        cleared = true;
        worker.execute(dao::close);
        worker.shutdown();
    }
}
