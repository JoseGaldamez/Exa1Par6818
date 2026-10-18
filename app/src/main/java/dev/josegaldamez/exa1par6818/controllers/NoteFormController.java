package dev.josegaldamez.exa1par6818.controllers;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import dev.josegaldamez.exa1par6818.R;
import dev.josegaldamez.exa1par6818.models.NoteDraft;

/** Validates form data without reading or changing any views. */
public final class NoteFormController {
    public enum Field { SUBJECT, TOPIC, DATE, DESCRIPTION }
    private final Set<String> subjects;

    public NoteFormController(String[] subjects) {
        this.subjects = new HashSet<>(Arrays.asList(subjects));
    }

    public Map<Field, Integer> validate(NoteDraft draft) {
        Map<Field, Integer> errors = new EnumMap<>(Field.class);
        if (!subjects.contains(draft.subject)) errors.put(Field.SUBJECT, R.string.subject_error);
        if (draft.topic.isEmpty()) errors.put(Field.TOPIC, R.string.required_error);
        else if (!draft.topic.matches("(?:\\p{L}\\p{M}*|[0-9 ])+")) {
            errors.put(Field.TOPIC, R.string.topic_symbols_error);
        }
        if (parseDate(draft.date) == null) errors.put(Field.DATE, R.string.date_error);
        if (draft.description.isEmpty()) errors.put(Field.DESCRIPTION, R.string.required_error);
        return errors;
    }

    public Date parseDate(String value) {
        if (!value.matches("\\d{4}-\\d{2}-\\d{2}")) return null;
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
        format.setLenient(false);
        ParsePosition position = new ParsePosition(0);
        Date parsed = format.parse(value, position);
        return position.getIndex() == value.length() ? parsed : null;
    }
}
