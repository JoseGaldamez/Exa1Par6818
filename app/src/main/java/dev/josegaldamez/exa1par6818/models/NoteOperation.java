package dev.josegaldamez.exa1par6818.models;

public final class NoteOperation {
    public enum Type { SAVED, DELETED, MISSING }
    public final Type type;
    public final Note note;

    public NoteOperation(Type type, Note note) {
        this.type = type;
        this.note = note;
    }
}
