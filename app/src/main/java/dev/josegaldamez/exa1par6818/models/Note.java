package dev.josegaldamez.exa1par6818.models;

/** A persisted note. SQLite stores metadata and the path to its private audio file. */
public final class Note {
    public final long id;
    public final String subject;
    public final String topic;
    public final String date;
    public final String description;
    public final String audioPath;
    public final long durationMs;
    public final long createdAt;
    public final long updatedAt;

    public Note(long id, String subject, String topic, String date, String description,
                String audioPath, long durationMs, long createdAt, long updatedAt) {
        this.id = id;
        this.subject = subject;
        this.topic = topic;
        this.date = date;
        this.description = description;
        this.audioPath = audioPath;
        this.durationMs = durationMs;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
