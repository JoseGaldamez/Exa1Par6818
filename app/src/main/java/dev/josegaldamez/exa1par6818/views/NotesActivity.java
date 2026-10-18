package dev.josegaldamez.exa1par6818.views;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.util.List;

import dev.josegaldamez.exa1par6818.R;
import dev.josegaldamez.exa1par6818.controllers.AudioRecordingController;
import dev.josegaldamez.exa1par6818.controllers.NotesController;
import dev.josegaldamez.exa1par6818.models.AudioRecordingState;
import dev.josegaldamez.exa1par6818.models.Note;
import dev.josegaldamez.exa1par6818.models.NoteOperation;

public class NotesActivity extends AppCompatActivity {
    public static final String EXTRA_SUBJECT = "subject_filter";
    private NotesController controller;
    private AudioRecordingController audioController;
    private NotesAdapter adapter;
    private MaterialAutoCompleteTextView filter;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notes);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.notesRoot), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        controller = new ViewModelProvider(this).get(NotesController.class);
        audioController = new ViewModelProvider(this).get(AudioRecordingController.class);
        MaterialToolbar toolbar = findViewById(R.id.notesToolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        Drawable addIcon = AppCompatResources.getDrawable(this, R.drawable.ic_add);
        if (addIcon != null) addIcon.mutate().setTint(getColor(R.color.uth_green));
        toolbar.getMenu().add(R.string.new_note).setIcon(addIcon)
                .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                .setOnMenuItemClickListener(item -> {
                    startActivity(new Intent(this, MainActivity.class));
                    return true;
                });
        filter = findViewById(R.id.subjectFilter);
        String[] subjects = getResources().getStringArray(R.array.subjects);
        String[] filters = new String[subjects.length + 1];
        filters[0] = getString(R.string.all_subjects);
        System.arraycopy(subjects, 0, filters, 1, subjects.length);
        filter.setSimpleItems(filters);
        String initial = savedInstanceState == null && getIntent().hasExtra(EXTRA_SUBJECT)
                ? getIntent().getStringExtra(EXTRA_SUBJECT) : controller.getFilter();
        filter.setText(initial == null ? filters[0] : initial, false);
        filter.setOnItemClickListener((parent, view, position, id) -> {
            controller.select(-1);
            audioController.clearDraft();
            controller.loadNotes(position == 0 ? null : filters[position]);
        });
        adapter = new NotesAdapter();
        ListView list = findViewById(R.id.notesList);
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, position, id) -> {
            audioController.onScreenHidden();
            controller.select(id);
            adapter.select(id);
            renderSelection();
        });
        controller.notes().observe(this, this::renderNotes);
        controller.busy().observe(this, busy -> {
            findViewById(R.id.notesLoading).setVisibility(busy ? View.VISIBLE : View.GONE);
            filter.setEnabled(!busy);
            findViewById(R.id.emptyNotes).setVisibility(!busy && adapter.getCount() == 0 ? View.VISIBLE : View.GONE);
            renderSelection();
        });
        controller.errors().observe(this, message -> {
            if (message == null) return;
            controller.consumeError();
            Snackbar.make(findViewById(R.id.notesRoot), message, Snackbar.LENGTH_LONG)
                    .setAction(R.string.retry, view -> refresh()).show();
        });
        controller.operations().observe(this, result -> {
            if (result == null) return;
            controller.consumeOperation();
            if (result.type == NoteOperation.Type.DELETED) {
                audioController.clearDraft();
                Toast.makeText(this, R.string.note_deleted, Toast.LENGTH_SHORT).show();
                refresh();
            }
        });
        audioController.state().observe(this, state -> {
            boolean playing = state.mode == AudioRecordingState.Mode.PLAYING;
            MaterialButton play = findViewById(R.id.listPlayButton);
            play.setText(playing ? R.string.stop_playback : R.string.play);
            play.setIconResource(playing ? R.drawable.ic_stop : R.drawable.ic_play);
            if (playing) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        });
        audioController.errors().observe(this, message -> {
            if (message == null) return;
            audioController.consumeError();
            Snackbar.make(findViewById(R.id.notesRoot), message, Snackbar.LENGTH_LONG).show();
        });
        findViewById(R.id.listPlayButton).setOnClickListener(v -> {
            Note note = selectedNote();
            if (note == null) return;
            if (audioController.mode() == AudioRecordingState.Mode.PLAYING) audioController.togglePlayback();
            else if (audioController.loadSavedRecording(note.audioPath, note.durationMs)) audioController.togglePlayback();
        });
        findViewById(R.id.listEditButton).setOnClickListener(v -> {
            Note note = selectedNote();
            if (note != null) startActivity(new Intent(this, MainActivity.class)
                    .putExtra(MainActivity.EXTRA_NOTE_ID, note.id));
        });
        findViewById(R.id.listDeleteButton).setOnClickListener(v -> {
            Note note = selectedNote();
            if (note == null) return;
            audioController.onScreenHidden();
            new MaterialAlertDialogBuilder(this).setTitle(R.string.delete_title)
                    .setMessage(getString(R.string.delete_message, note.topic))
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.delete_note, (dialog, which) -> controller.delete(note))
                    .show();
        });
    }

    private void renderNotes(List<Note> notes) {
        adapter.submit(notes, controller.getSelectedId());
        if (selectedNote() == null) {
            controller.select(-1);
            audioController.clearDraft();
        }
        ((TextView) findViewById(R.id.notesCount)).setText(getResources()
                .getQuantityString(R.plurals.notes_count, notes.size(), notes.size()));
        findViewById(R.id.emptyNotes).setVisibility(!controller.isBusy() && notes.isEmpty() ? View.VISIBLE : View.GONE);
        renderSelection();
    }

    private Note selectedNote() { return adapter.find(controller.getSelectedId()); }

    private void renderSelection() {
        Note selected = selectedNote();
        boolean enabled = selected != null && !controller.isBusy();
        findViewById(R.id.listPlayButton).setEnabled(enabled);
        findViewById(R.id.listEditButton).setEnabled(enabled);
        findViewById(R.id.listDeleteButton).setEnabled(enabled);
        ((TextView) findViewById(R.id.selectedNoteLabel)).setText(selected == null
                ? getString(R.string.select_note_hint) : getString(R.string.selected_note, selected.topic));
    }

    private void refresh() {
        String text = filter.getText().toString();
        controller.loadNotes(text.equals(getString(R.string.all_subjects)) ? null : text);
    }

    @Override protected void onResume() {
        super.onResume();
        if (!controller.isBusy()) refresh();
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent.hasExtra(EXTRA_SUBJECT)) {
            filter.setText(intent.getStringExtra(EXTRA_SUBJECT), false);
            controller.select(-1);
        }
        refresh();
    }

    @Override protected void onStop() {
        audioController.onScreenHidden();
        super.onStop();
    }
}
