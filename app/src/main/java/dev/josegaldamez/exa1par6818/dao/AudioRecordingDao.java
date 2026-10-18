package dev.josegaldamez.exa1par6818.dao;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import dev.josegaldamez.exa1par6818.models.AudioRecording;

/** All access to the private directory containing recording drafts. */
public final class AudioRecordingDao {
    private final File directory;

    public AudioRecordingDao(Context context) {
        directory = new File(context.getApplicationContext().getFilesDir(), "recordings");
    }

    public File createDraft() throws IOException {
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new IOException("Audio directory unavailable");
        }
        return File.createTempFile("voice_", ".m4a", directory);
    }

    /** Copy a draft before inserting it, so leaving the editor cannot delete saved audio. */
    public AudioRecording copyForNote(AudioRecording source) throws IOException {
        completeDraft(source.getFile(), source.getDurationMs());
        File destination = createDraft();
        try {
            try (FileInputStream input = new FileInputStream(source.getFile());
                 FileOutputStream output = new FileOutputStream(destination)) {
                byte[] buffer = new byte[16384];
                int length;
                while ((length = input.read(buffer)) != -1) output.write(buffer, 0, length);
                output.getFD().sync();
            }
            return completeDraft(destination, source.getDurationMs());
        } catch (IOException exception) {
            delete(destination);
            throw exception;
        }
    }

    public AudioRecording completeDraft(File draft, long durationMs) throws IOException {
        if (!isManagedFile(draft) || !draft.isFile() || draft.length() == 0) {
            throw new IOException("Empty or unavailable audio");
        }
        return new AudioRecording(draft, durationMs);
    }

    public AudioRecording findByPath(String path, long durationMs) {
        if (path == null) return null;
        try {
            return completeDraft(new File(path), durationMs);
        } catch (IOException exception) {
            return null;
        }
    }

    public void delete(File file) {
        try {
            if (isManagedFile(file)) file.delete();
        } catch (IOException ignored) {
            // A cleanup failure must not discard a successfully finalized recording.
        }
    }

    private boolean isManagedFile(File file) throws IOException {
        return file != null && directory.getCanonicalFile().equals(file.getCanonicalFile().getParentFile());
    }
}
