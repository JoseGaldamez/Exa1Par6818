package dev.josegaldamez.exa1par6818.controllers;

import dev.josegaldamez.exa1par6818.models.NoteDraft;
import dev.josegaldamez.exa1par6818.R;
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

    @Test public void acceptsTopicsWithAccentsNumbersAndSpaces() {
        for (String topic : new String[]{"Programación Móvil 1", "Diseño y análisis 2026",
                "Lección de español", "Programacio\u0301n", "  Tema 2  "}) {
            assertTrue(topic, controller.validate(new NoteDraft("Programación Móvil I", topic,
                    "2026-10-18", "Resumen")).isEmpty());
        }
    }

    @Test public void rejectsSymbolsAndPreservesRequiredTopicError() {
        for (String topic : new String[]{"Actividad @#$", "Tema!", "Tema_1", "Tema-1",
                "Tema.", "Tema 😊", "Tema\nNuevo", "Tema\tNuevo"}) {
            assertEquals(topic, Integer.valueOf(R.string.topic_symbols_error),
                    controller.validate(new NoteDraft("Programación Móvil I", topic,
                            "2026-10-18", "Resumen")).get(NoteFormController.Field.TOPIC));
        }
        assertEquals(Integer.valueOf(R.string.required_error),
                controller.validate(new NoteDraft("Programación Móvil I", "   ",
                        "2026-10-18", "Resumen")).get(NoteFormController.Field.TOPIC));
    }
}
