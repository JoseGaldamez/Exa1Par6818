package dev.josegaldamez.exa1par6818.models;

/** Form data, independent of Android views. */
public final class NoteDraft {
    public final String subject;
    public final String topic;
    public final String date;
    public final String description;

    public NoteDraft(String subject, String topic, String date, String description) {
        this.subject = subject.trim();
        this.topic = topic.trim();
        this.date = date.trim();
        this.description = description.trim();
    }
}
