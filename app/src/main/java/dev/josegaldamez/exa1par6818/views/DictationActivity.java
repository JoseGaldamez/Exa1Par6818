package dev.josegaldamez.exa1par6818.views;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;

import dev.josegaldamez.exa1par6818.R;

public class DictationActivity extends AppCompatActivity {
    public static final String EXTRA_DESCRIPTION = "dictated_description";
    public static final String EXTRA_TOPIC = "dictation_topic";
    private TextInputEditText input;
    private final ActivityResultLauncher<String> microphonePermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) startDictation();
                else explainMicrophonePermission();
            });
    private final ActivityResultLauncher<Intent> recognition = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != RESULT_OK) return;
                Intent data = result.getData();
                ArrayList<String> matches = data == null ? null
                        : data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (matches == null || matches.isEmpty() || matches.get(0) == null
                        || matches.get(0).trim().isEmpty()) {
                    Snackbar.make(input, R.string.dictation_no_text, Snackbar.LENGTH_LONG).show();
                    return;
                }
                String current = input.getText() == null ? "" : input.getText().toString();
                String separator = current.isEmpty()
                        || Character.isWhitespace(current.charAt(current.length() - 1)) ? "" : " ";
                input.append(separator + matches.get(0).trim());
                input.setSelection(input.length());
            });

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dictation);
        input = findViewById(R.id.dictationInput);
        if (savedInstanceState == null) input.setText(getIntent().getStringExtra(EXTRA_DESCRIPTION));
        String topic = getIntent().getStringExtra(EXTRA_TOPIC);
        ((TextView) findViewById(R.id.dictationTopic)).setText(topic == null || topic.trim().isEmpty()
                ? getString(R.string.dictate_description) : topic);
        new FormKeyboardHelper(findViewById(R.id.dictationRoot), findViewById(R.id.dictationScroll), input);
        MaterialToolbar toolbar = findViewById(R.id.dictationToolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        findViewById(R.id.startDictationButton).setOnClickListener(v -> requestDictation());
        findViewById(R.id.saveDescriptionButton).setOnClickListener(v -> {
            setResult(RESULT_OK, new Intent().putExtra(EXTRA_DESCRIPTION,
                    input.getText() == null ? "" : input.getText().toString()));
            finish();
        });
    }

    private void requestDictation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            startDictation();
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
            explainMicrophonePermission();
        } else microphonePermission.launch(Manifest.permission.RECORD_AUDIO);
    }

    private void explainMicrophonePermission() {
        boolean canRequestAgain = shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO);
        new AlertDialog.Builder(this)
                .setTitle(R.string.allow_microphone)
                .setMessage(R.string.dictation_permission)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(canRequestAgain ? R.string.allow_microphone : R.string.open_settings,
                        (dialog, which) -> {
                            if (canRequestAgain) microphonePermission.launch(Manifest.permission.RECORD_AUDIO);
                            else startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.parse("package:" + getPackageName())));
                        })
                .show();
    }

    private void startDictation() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-HN")
                .putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.dictation_title));
        try {
            recognition.launch(intent);
        } catch (ActivityNotFoundException exception) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.dictation_title)
                    .setMessage(R.string.dictation_unavailable)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        }
    }
}
