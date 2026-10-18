package dev.josegaldamez.exa1par6818.models;

/** Immutable state shared by the audio controller and the screen. */
public final class AudioRecordingState {
    public enum Mode { IDLE, RECORDING, PAUSED, READY, PLAYING }

    public final Mode mode;
    public final long elapsedMs;
    public final float amplitude;

    public AudioRecordingState(Mode mode, long elapsedMs, float amplitude) {
        this.mode = mode;
        this.elapsedMs = elapsedMs;
        this.amplitude = amplitude;
    }
}
