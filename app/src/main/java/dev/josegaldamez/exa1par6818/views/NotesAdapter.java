package dev.josegaldamez.exa1par6818.views;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import dev.josegaldamez.exa1par6818.R;
import dev.josegaldamez.exa1par6818.models.Note;

final class NotesAdapter extends BaseAdapter {
    private final List<Note> notes = new ArrayList<>();
    private long selectedId = -1;
    private final SimpleDateFormat storedDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
    private final SimpleDateFormat displayDateFormat = new SimpleDateFormat(
            "d 'de' MMMM 'de' yyyy", new Locale("es", "HN"));

    void submit(List<Note> data, long selectedId) {
        notes.clear();
        notes.addAll(data);
        this.selectedId = selectedId;
        notifyDataSetChanged();
    }

    void select(long id) { selectedId = id; notifyDataSetChanged(); }
    Note find(long id) {
        for (Note note : notes) if (note.id == id) return note;
        return null;
    }
    @Override public int getCount() { return notes.size(); }
    @Override public Note getItem(int position) { return notes.get(position); }
    @Override public long getItemId(int position) { return getItem(position).id; }
    @Override public boolean hasStableIds() { return true; }

    @Override public View getView(int position, View recycled, ViewGroup parent) {
        View row = recycled == null ? LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_note, parent, false) : recycled;
        Note note = getItem(position);
        boolean selected = note.id == selectedId;
        MaterialCardView card = (MaterialCardView) row;
        card.setCardBackgroundColor(ContextCompat.getColor(parent.getContext(),
                selected ? R.color.soft_green : R.color.surface));
        card.setStrokeColor(ContextCompat.getColor(parent.getContext(),
                selected ? R.color.uth_green : R.color.outline));
        card.setSelected(selected);
        long seconds = note.durationMs / 1000;
        String duration = String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60);
        ((TextView) row.findViewById(R.id.noteMeta)).setText(parent.getContext()
                .getString(R.string.note_row_meta, readableDate(note.date), duration));
        ((TextView) row.findViewById(R.id.noteTopic)).setText(note.topic);
        ((TextView) row.findViewById(R.id.noteSubject)).setText(note.subject);
        ((TextView) row.findViewById(R.id.noteDescription)).setText(note.description);
        return row;
    }

    private String readableDate(String date) {
        try {
            storedDateFormat.setLenient(false);
            return displayDateFormat.format(storedDateFormat.parse(date));
        } catch (ParseException exception) {
            return date;
        }
    }
}
