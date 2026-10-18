package dev.josegaldamez.exa1par6818.models;

import java.io.File;

/** A finalized local audio recording, independent of the screen and recorder. */
public final class AudioRecording {
    private final File file;
    private final long durationMs;

    public AudioRecording(File file, long durationMs) {
        this.file = file;
        this.durationMs = durationMs;
    }

    public File getFile() { return file; }
    public long getDurationMs() { return durationMs; }
}
