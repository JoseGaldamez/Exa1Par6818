package dev.josegaldamez.exa1par6818;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;

import dev.josegaldamez.exa1par6818.dao.AudioRecordingDao;
import dev.josegaldamez.exa1par6818.dao.NoteDao;
import dev.josegaldamez.exa1par6818.models.AudioRecording;
import dev.josegaldamez.exa1par6818.models.Note;
import dev.josegaldamez.exa1par6818.models.NoteDraft;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class NotesPersistenceTest {
    @Test public void sqlitePersistsFiltersAndUpdatesMetadataWithoutChangingAudio() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AudioRecordingDao audioDao = new AudioRecordingDao(context);
        File draft = audioDao.createDraft();
        AudioRecording savedAudio = null;
        long id = -1;
        try {
            try (FileOutputStream output = new FileOutputStream(draft)) { output.write(new byte[]{1, 2, 3, 4}); }
            savedAudio = audioDao.copyForNote(new AudioRecording(draft, 3200));
            String topic = "SQLite '); DROP TABLE notes; -- áéíóú";
            try (NoteDao dao = new NoteDao(context)) {
                Note inserted = dao.insert(new NoteDraft("Programación Móvil I", topic, "2026-10-17", "Descripción original"), savedAudio);
                id = inserted.id;
            }
            // Reopening the helper verifies persistence beyond the original connection.
            try (NoteDao dao = new NoteDao(context)) {
                Note restored = dao.findById(id);
                assertNotNull(restored);
                assertEquals(topic, restored.topic);
                assertEquals(3200, restored.durationMs);
                assertTrue(new File(restored.audioPath).isFile());
                assertNotEquals(draft.getAbsolutePath(), restored.audioPath);
                assertTrue(dao.findBySubject("Programación Móvil I").stream().anyMatch(n -> n.id == restored.id));
                Note updated = dao.update(id, new NoteDraft("Base de Datos I", "Tema editado", "2026-10-18", "Descripción editada"));
                assertEquals(restored.audioPath, updated.audioPath);
                assertEquals(restored.durationMs, updated.durationMs);
                assertEquals(restored.createdAt, updated.createdAt);
                assertFalse(dao.findBySubject("Programación Móvil I").stream().anyMatch(n -> n.id == restored.id));
                assertTrue(dao.findBySubject("Base de Datos I").stream().anyMatch(n -> n.id == restored.id));
                assertTrue(dao.findBySubject("Asignatura inexistente").isEmpty());
                assertTrue(dao.delete(id));
                assertNull(dao.findById(id));
                assertFalse(dao.delete(id));
            }
        } finally {
            try (NoteDao dao = new NoteDao(context)) { if (id >= 0) dao.delete(id); }
            audioDao.delete(draft);
            if (savedAudio != null) audioDao.delete(savedAudio.getFile());
        }
    }
}
