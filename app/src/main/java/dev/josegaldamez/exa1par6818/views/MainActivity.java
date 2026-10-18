package dev.josegaldamez.exa1par6818.views;

import dev.josegaldamez.exa1par6818.R;
import dev.josegaldamez.exa1par6818.controllers.AudioRecordingController;
import dev.josegaldamez.exa1par6818.controllers.NoteFormController;
import dev.josegaldamez.exa1par6818.controllers.NotesController;
import dev.josegaldamez.exa1par6818.models.Note;
import dev.josegaldamez.exa1par6818.models.NoteOperation;
import dev.josegaldamez.exa1par6818.models.NoteDraft;
import dev.josegaldamez.exa1par6818.models.AudioRecordingState;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.TextUtils;
import android.view.View;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowCompat;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_NOTE_ID = "note_id";
    private NotesController notesController;
    private long editingId = -1;
    private boolean editorPopulated;
    private boolean loadingEditor;
    private AudioRecordingController audioController;
    private NoteFormController formController;
    private final ActivityResultLauncher<String> microphonePermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) audioController.startRecording();
                else showMicrophoneDenied();
            });
    private MaterialAutoCompleteTextView subjectInput;
    private TextInputEditText topicInput;
    private TextInputEditText dateInput;
    private TextInputEditText descriptionInput;
    private TextInputLayout subjectLayout;
    private TextInputLayout topicLayout;
    private TextInputLayout dateLayout;
    private TextInputLayout descriptionLayout;
    private final Calendar selectedDate = Calendar.getInstance();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        editingId = getIntent().getLongExtra(EXTRA_NOTE_ID, -1);
        editorPopulated = savedInstanceState != null && savedInstanceState.getBoolean("editor_populated");
        notesController = new ViewModelProvider(this).get(NotesController.class);

        // Decode the supplied high-resolution logo at a size appropriate for the header.
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 8;
        ((ImageView) findViewById(R.id.uthLogo)).setImageBitmap(
                BitmapFactory.decodeResource(getResources(), R.drawable.uth_logo, options));

        subjectLayout = findViewById(R.id.subjectLayout);
        topicLayout = findViewById(R.id.topicLayout);
        dateLayout = findViewById(R.id.dateLayout);
        descriptionLayout = findViewById(R.id.descriptionLayout);
        subjectInput = findViewById(R.id.subjectInput);
        topicInput = findViewById(R.id.topicInput);
        dateInput = findViewById(R.id.dateInput);
        descriptionInput = findViewById(R.id.descriptionInput);
        formController = new NoteFormController(getResources().getStringArray(R.array.subjects));
        new FormKeyboardHelper(findViewById(R.id.main), topicInput, descriptionInput);
        subjectInput.setSimpleItems(R.array.subjects);
        dateFormat.setLenient(false);
        // Selection is exclusively through the calendar; keep keyboard and accessibility focus.
        dateInput.setKeyListener(null);
        dateInput.setOnClickListener(v -> showDatePicker());
        dateLayout.setEndIconOnClickListener(v -> showDatePicker());
        if (savedInstanceState == null && editingId < 0) {
            dateInput.setText(dateFormat.format(selectedDate.getTime()));
        }

        clearErrorOnEdit(subjectInput, subjectLayout);
        clearErrorOnEdit(topicInput, topicLayout);
        clearErrorOnEdit(dateInput, dateLayout);
        clearErrorOnEdit(descriptionInput, descriptionLayout);

        audioController = new ViewModelProvider(this).get(AudioRecordingController.class);
        audioController.state().observe(this, this::renderAudioState);
        audioController.errors().observe(this, message -> {
            if (message != null) {
                showMessage(message);
                audioController.consumeError();
            }
        });
        findViewById(R.id.recordButton).setOnClickListener(v -> {
            if (audioController.getCompletedRecording() != null) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.record_again_title)
                        .setMessage(R.string.record_again_message)
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.record, (dialog, which) -> requestRecording())
                        .show();
            } else requestRecording();
        });
        findViewById(R.id.playButton).setOnClickListener(v -> audioController.togglePlayback());
        findViewById(R.id.pauseRecordingButton).setOnClickListener(v -> audioController.toggleRecordingPause());
        findViewById(R.id.finishRecordingButton).setOnClickListener(v -> audioController.finishRecording());
        findViewById(R.id.notesButton).setOnClickListener(v -> {
            if (editingId >= 0) finish();
            else startActivity(new Intent(this, NotesActivity.class));
        });
        findViewById(R.id.saveButton).setOnClickListener(v -> saveNote());
        findViewById(R.id.editorBackButton).setOnClickListener(v -> {
            if (!notesController.isBusy()) finish();
        });
        notesController.busy().observe(this, busy -> {
            boolean enabled = !busy && !loadingEditor;
            findViewById(R.id.saveButton).setEnabled(enabled);
            findViewById(R.id.notesButton).setEnabled(enabled);
            findViewById(R.id.editorBackButton).setEnabled(enabled);
            subjectInput.setEnabled(enabled);
            topicInput.setEnabled(enabled);
            dateInput.setEnabled(enabled);
            descriptionInput.setEnabled(enabled);
            findViewById(R.id.recordButton).setEnabled(enabled);
            findViewById(R.id.pauseRecordingButton).setEnabled(enabled);
            findViewById(R.id.finishRecordingButton).setEnabled(enabled);
            ((MaterialButton) findViewById(R.id.saveButton)).setText(busy ? R.string.saving_note
                    : editingId >= 0 ? R.string.update_note : R.string.save);
        });
        notesController.errors().observe(this, message -> {
            if (message == null) return;
            notesController.consumeError();
            showMessage(message);
            if (loadingEditor) {
                new MaterialAlertDialogBuilder(this).setMessage(message)
                        .setNegativeButton(R.string.back, (dialog, which) -> finish())
                        .setPositiveButton(R.string.retry, (dialog, which) -> notesController.loadEditor(editingId))
                        .setCancelable(false).show();
            }
        });
        notesController.operations().observe(this, result -> {
            if (result == null) return;
            notesController.consumeOperation();
            if (result.type == NoteOperation.Type.MISSING) {
                Toast.makeText(this, R.string.note_not_found, Toast.LENGTH_LONG).show();
                finish();
            } else if (result.type == NoteOperation.Type.SAVED) {
                Toast.makeText(this, editingId >= 0 ? R.string.note_updated : R.string.note_saved,
                        Toast.LENGTH_SHORT).show();
                if (editingId >= 0) finish();
                else {
                    audioController.clearDraft();
                    topicInput.setText("");
                    descriptionInput.setText("");
                    subjectInput.setText("", false);
                    dateInput.setText(dateFormat.format(Calendar.getInstance().getTime()));
                    startActivity(new Intent(this, NotesActivity.class)
                            .putExtra(NotesActivity.EXTRA_SUBJECT, result.note.subject)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                }
            }
        });
        if (editingId >= 0) {
            ((TextView) findViewById(R.id.screenTitle)).setText(R.string.edit_title);
            ((TextView) findViewById(R.id.screenSubtitle)).setText(R.string.edit_subtitle);
            ((MaterialButton) findViewById(R.id.saveButton)).setText(R.string.update_note);
            findViewById(R.id.editorBackButton).setVisibility(View.VISIBLE);
            loadingEditor = notesController.editingNote().getValue() == null;
            notesController.editingNote().observe(this, this::populateEditor);
            notesController.loadEditor(editingId);
        }
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (notesController.isBusy()) showMessage(R.string.saving_note);
                else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void populateEditor(Note note) {
        if (note == null) return;
        loadingEditor = false;
        if (!editorPopulated) {
            subjectInput.setText(note.subject, false);
            topicInput.setText(note.topic);
            dateInput.setText(note.date);
            descriptionInput.setText(note.description);
            editorPopulated = true;
        }
        if (audioController.getCompletedRecording() == null) {
            audioController.loadSavedRecording(note.audioPath, note.durationMs);
        }
        findViewById(R.id.saveButton).setEnabled(!notesController.isBusy());
    }

    private void saveNote() {
        if (notesController.isBusy() || loadingEditor || !validateForm()) return;
        if (audioController.mode() == AudioRecordingState.Mode.RECORDING
                || audioController.mode() == AudioRecordingState.Mode.PAUSED) {
            showMessage(R.string.finish_before_save);
            return;
        }
        if (editingId < 0 && audioController.getCompletedRecording() == null) {
            showMessage(R.string.audio_required);
            return;
        }
        audioController.onScreenHidden();
        WindowCompat.getInsetsController(getWindow(), findViewById(R.id.main))
                .hide(WindowInsetsCompat.Type.ime());
        notesController.save(editingId, new NoteDraft(subjectInput.getText().toString(),
                textOf(topicInput), textOf(dateInput), textOf(descriptionInput)),
                audioController.getCompletedRecording());
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putBoolean("editor_populated", editorPopulated);
        super.onSaveInstanceState(state);
    }

    private void requestRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            audioController.startRecording();
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
            new MaterialAlertDialogBuilder(this)
                    .setMessage(R.string.microphone_rationale)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.allow_microphone, (dialog, which) ->
                            microphonePermission.launch(Manifest.permission.RECORD_AUDIO))
                    .show();
        } else {
            microphonePermission.launch(Manifest.permission.RECORD_AUDIO);
        }
    }

    private void showMicrophoneDenied() {
        Snackbar.make(findViewById(R.id.main), R.string.microphone_denied, Snackbar.LENGTH_LONG)
                .setAction(R.string.open_settings, v -> startActivity(new Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + getPackageName()))))
                .show();
    }

    private void renderAudioState(AudioRecordingState state) {
        boolean capturing = state.mode == AudioRecordingState.Mode.RECORDING;
        boolean paused = state.mode == AudioRecordingState.Mode.PAUSED;
        boolean active = capturing || paused;
        boolean playing = state.mode == AudioRecordingState.Mode.PLAYING;
        boolean canPlay = state.mode == AudioRecordingState.Mode.READY || playing;
        findViewById(R.id.idleControls).setVisibility(active ? View.GONE : View.VISIBLE);
        findViewById(R.id.activeRecordingControls).setVisibility(active ? View.VISIBLE : View.GONE);
        findViewById(R.id.playControl).setVisibility(canPlay ? View.VISIBLE : View.GONE);
        findViewById(R.id.recordControl).setVisibility(editingId >= 0 ? View.GONE : View.VISIBLE);
        findViewById(R.id.recordButton).setEnabled(!playing && !notesController.isBusy());
        RecordingWaveView wave = findViewById(R.id.recordingWave);
        wave.setVisibility(active ? View.VISIBLE : View.GONE);
        wave.setRecording(capturing, state.amplitude);

        MaterialButton pause = findViewById(R.id.pauseRecordingButton);
        pause.setText(paused ? R.string.resume_recording : R.string.pause_recording);
        pause.setIconResource(paused ? R.drawable.ic_mic : R.drawable.ic_pause);
        MaterialButton play = findViewById(R.id.playButton);
        play.setIconResource(playing ? R.drawable.ic_stop : R.drawable.ic_play);
        play.setContentDescription(getString(playing ? R.string.stop_playback : R.string.play));
        ((TextView) findViewById(R.id.playLabel)).setText(playing ? R.string.stop_playback : R.string.play);
        int status = capturing ? R.string.recording : paused ? R.string.recording_paused
                : playing ? R.string.playing : canPlay ? R.string.recording_ready : R.string.ready;
        TextView statusView = findViewById(R.id.recordingStatus);
        if (!TextUtils.equals(statusView.getText(), getString(status))) statusView.setText(status);
        long seconds = state.elapsedMs / 1000;
        String time = String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60);
        TextView timer = findViewById(R.id.recordingTimer);
        if (!TextUtils.equals(timer.getText(), time)) timer.setText(time);
        if (capturing || playing) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onStop() {
        if (audioController != null) audioController.onScreenHidden();
        super.onStop();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            View focusedView = getCurrentFocus();
            if (focusedView instanceof EditText) {
                Rect bounds = new Rect();
                focusedView.getGlobalVisibleRect(bounds);
                if (!bounds.contains((int) event.getRawX(), (int) event.getRawY())) {
                    focusedView.clearFocus();
                    WindowCompat.getInsetsController(getWindow(), findViewById(R.id.main))
                            .hide(WindowInsetsCompat.Type.ime());
                }
            }
        }
        // Let the touched control receive its normal click, including other inputs.
        return super.dispatchTouchEvent(event);
    }

    private void showDatePicker() {
        Date current = formController.parseDate(dateInput.getText() == null ? "" : dateInput.getText().toString());
        if (current != null) selectedDate.setTime(current);
        DatePickerDialog picker = new DatePickerDialog(this, (view, year, month, day) -> {
            selectedDate.set(year, month, day);
            dateInput.setText(dateFormat.format(selectedDate.getTime()));
        }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH));
        picker.show();
    }

    private boolean validateForm() {
        NoteDraft draft = new NoteDraft(subjectInput.getText().toString(), textOf(topicInput),
                textOf(dateInput), textOf(descriptionInput));
        Map<NoteFormController.Field, Integer> errors = formController.validate(draft);
        TextInputLayout[] layouts = {subjectLayout, topicLayout, dateLayout, descriptionLayout};
        View[] inputs = {subjectInput, topicInput, dateInput, descriptionInput};
        NoteFormController.Field[] fields = NoteFormController.Field.values();
        View firstInvalid = null;
        for (int index = 0; index < fields.length; index++) {
            Integer error = errors.get(fields[index]);
            layouts[index].setError(error == null ? null : getString(error));
            if (error != null && firstInvalid == null) firstInvalid = inputs[index];
        }
        if (firstInvalid != null) firstInvalid.requestFocus();
        return errors.isEmpty();
    }

    private String textOf(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    private void clearErrorOnEdit(android.widget.TextView input, TextInputLayout layout) {
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                layout.setError(null);
            }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    private void showMessage(int message) {
        Snackbar.make(findViewById(R.id.main), message, Snackbar.LENGTH_LONG).show();
    }
}
