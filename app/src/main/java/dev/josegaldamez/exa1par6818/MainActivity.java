package dev.josegaldamez.exa1par6818;

import android.app.DatePickerDialog;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
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
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets keyboard = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, keyboard.bottom));
            return insets;
        });

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
        subjectInput.setSimpleItems(R.array.subjects);
        dateFormat.setLenient(false);
        // Selection is exclusively through the calendar; keep keyboard and accessibility focus.
        dateInput.setKeyListener(null);
        dateInput.setOnClickListener(v -> showDatePicker());
        dateLayout.setEndIconOnClickListener(v -> showDatePicker());
        if (savedInstanceState == null) {
            dateInput.setText(dateFormat.format(selectedDate.getTime()));
        }

        clearErrorOnEdit(subjectInput, subjectLayout);
        clearErrorOnEdit(topicInput, topicLayout);
        clearErrorOnEdit(dateInput, dateLayout);
        clearErrorOnEdit(descriptionInput, descriptionLayout);

        findViewById(R.id.recordButton).setOnClickListener(v -> showMessage(R.string.record_pending));
        findViewById(R.id.playButton).setOnClickListener(v -> showMessage(R.string.play_pending));
        findViewById(R.id.notesButton).setOnClickListener(v -> showMessage(R.string.notes_pending));
        findViewById(R.id.saveButton).setOnClickListener(v -> {
            if (validateForm()) {
                showMessage(R.string.save_pending);
            }
        });
    }

    private void showDatePicker() {
        Date current = parseDate(dateInput.getText() == null ? "" : dateInput.getText().toString());
        if (current != null) selectedDate.setTime(current);
        DatePickerDialog picker = new DatePickerDialog(this, (view, year, month, day) -> {
            selectedDate.set(year, month, day);
            dateInput.setText(dateFormat.format(selectedDate.getTime()));
        }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH));
        picker.show();
    }

    private boolean validateForm() {
        View firstInvalid = null;
        String subject = subjectInput.getText().toString().trim();
        boolean validSubject = false;
        for (String option : getResources().getStringArray(R.array.subjects)) {
            if (option.equals(subject)) validSubject = true;
        }
        subjectLayout.setError(validSubject ? null : getString(R.string.subject_error));
        if (!validSubject) firstInvalid = subjectInput;

        boolean validTopic = !textOf(topicInput).isEmpty();
        topicLayout.setError(validTopic ? null : getString(R.string.required_error));
        if (!validTopic && firstInvalid == null) firstInvalid = topicInput;

        boolean validDate = parseDate(textOf(dateInput)) != null;
        dateLayout.setError(validDate ? null : getString(R.string.date_error));
        if (!validDate && firstInvalid == null) firstInvalid = dateInput;

        boolean validDescription = !textOf(descriptionInput).isEmpty();
        descriptionLayout.setError(validDescription ? null : getString(R.string.required_error));
        if (!validDescription && firstInvalid == null) firstInvalid = descriptionInput;

        if (firstInvalid != null) firstInvalid.requestFocus();
        return firstInvalid == null;
    }

    private Date parseDate(String value) {
        if (!value.matches("\\d{4}-\\d{2}-\\d{2}")) return null;
        ParsePosition position = new ParsePosition(0);
        Date parsed = dateFormat.parse(value, position);
        return position.getIndex() == value.length() ? parsed : null;
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
