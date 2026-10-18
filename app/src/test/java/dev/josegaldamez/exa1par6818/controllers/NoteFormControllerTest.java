package dev.josegaldamez.exa1par6818.controllers;

import dev.josegaldamez.exa1par6818.models.NoteDraft;
import org.junit.Test;
import static org.junit.Assert.*;

public class NoteFormControllerTest {
    private final NoteFormController controller = new NoteFormController(new String[]{"Programación Móvil I"});

    @Test public void acceptsValidFieldsAndLeapDate() {
        assertTrue(controller.validate(new NoteDraft("Programación Móvil I", " Activity ",
                "2028-02-29", " Resumen ")).isEmpty());
    }

    @Test public void rejectsWhitespaceAndUnknownSubject() {
        assertEquals(4, controller.validate(new NoteDraft("Otra asignatura", "  ", "", "\n\t")).size());
    }

    @Test public void rejectsImpossibleAndIncorrectlyFormattedDates() {
        assertNull(controller.parseDate("2026-02-29"));
        assertNull(controller.parseDate("2026-13-01"));
        assertNull(controller.parseDate("2026-01-01extra"));
        assertNull(controller.parseDate("17/10/2026"));
    }
}
