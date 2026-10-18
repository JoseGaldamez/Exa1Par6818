package dev.josegaldamez.exa1par6818;

import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.widget.EditText;
import android.widget.ListView;

import androidx.lifecycle.ViewModelProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import dev.josegaldamez.exa1par6818.controllers.AudioRecordingController;
import dev.josegaldamez.exa1par6818.controllers.NotesController;
import dev.josegaldamez.exa1par6818.dao.NoteDao;
import dev.josegaldamez.exa1par6818.dao.SubjectDao;
import dev.josegaldamez.exa1par6818.dao.NotesDatabaseHelper;
import dev.josegaldamez.exa1par6818.models.Note;
import dev.josegaldamez.exa1par6818.views.MainActivity;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class NotesFlowTest {
    @Test public void createListenFilterEditAndDeleteNoteWithItsAudio() throws Exception {
        String topic = "Prueba de apuntes " + System.currentTimeMillis();
        String otherSubject = "Filtro de prueba " + System.currentTimeMillis();
        try (SubjectDao subjects = new SubjectDao(InstrumentationRegistry.getInstrumentation().getTargetContext())) {
            subjects.add(otherSubject);
        }
        try (ParcelFileDescriptor command = InstrumentationRegistry.getInstrumentation().getUiAutomation()
                .executeShellCommand("pm grant dev.josegaldamez.exa1par6818 android.permission.RECORD_AUDIO");
             FileInputStream output = new FileInputStream(command.getFileDescriptor())) {
            byte[] buffer = new byte[256];
            while (output.read(buffer) != -1) { }
        }
        long id = -1;
        File persistedFile = null;
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                ((MaterialAutoCompleteTextView) activity.findViewById(R.id.subjectInput)).setText("Programación Móvil 1", false);
                ((EditText) activity.findViewById(R.id.topicInput)).setText(topic);
                ((EditText) activity.findViewById(R.id.descriptionInput)).setText("Descripción de prueba");
                activity.findViewById(R.id.recordButton).performClick();
            });
            SystemClock.sleep(1800);
            scenario.onActivity(activity -> {
                activity.findViewById(R.id.finishRecordingButton).performClick();
                activity.findViewById(R.id.saveButton).performClick();
            });
            Note saved = awaitNote(topic);
            id = saved.id;
            persistedFile = new File(saved.audioPath);
            assertTrue(persistedFile.isFile());
            waitForView(R.id.notesList);
            waitForText(topic);
            onView(withText(topic)).perform(click());
            onView(withId(R.id.listPlayButton)).perform(click());
            onView(withId(R.id.listPlayButton)).check(matches(withText(R.string.stop_playback)));
            onView(withId(R.id.listPlayButton)).perform(click());
            onView(withId(R.id.listEditButton)).perform(click());
            waitForView(R.id.topicInput);
            onView(withId(R.id.screenTitle)).check(matches(withText(R.string.edit_title)));
            onView(withId(R.id.topicInput)).check(matches(withText(topic)));
            onView(withId(R.id.topicInput)).perform(scrollTo(), replaceText(topic + " editado"), closeSoftKeyboard());
            onView(withId(R.id.descriptionInput)).perform(scrollTo(), replaceText("Descripción actualizada"), closeSoftKeyboard());
            onView(withId(R.id.saveButton)).perform(scrollTo(), click());
            Note updated = awaitNote(topic + " editado");
            assertEquals(saved.id, updated.id);
            assertEquals(saved.audioPath, updated.audioPath);
            assertEquals(saved.durationMs, updated.durationMs);
            assertEquals("Descripción actualizada", updated.description);
            waitForView(R.id.notesList);
            // Filter to another subject and verify the newly edited note is excluded.
            onView(withId(R.id.subjectFilter)).perform(click());
            onView(withText(otherSubject)).inRoot(androidx.test.espresso.matcher.RootMatchers.isPlatformPopup()).perform(click());
            SystemClock.sleep(500);
            onView(withText(topic + " editado")).check(doesNotExist());
            onView(withId(R.id.subjectFilter)).perform(click());
            onView(withText("Programación Móvil 1")).inRoot(androidx.test.espresso.matcher.RootMatchers.isPlatformPopup()).perform(click());
            SystemClock.sleep(500);
            waitForText(topic + " editado");
            onView(withText(topic + " editado")).perform(click());
            onView(withId(R.id.listDeleteButton)).perform(click());
            onView(withText(R.string.cancel)).perform(click());
            assertTrue(persistedFile.isFile());
            onView(withId(R.id.listDeleteButton)).perform(click());
            onView(withId(android.R.id.button1)).perform(click());
            long deadline = SystemClock.elapsedRealtime() + 8000;
            boolean deleted = false;
            try (NoteDao dao = new NoteDao(InstrumentationRegistry.getInstrumentation().getTargetContext())) {
                while (SystemClock.elapsedRealtime() < deadline) {
                    if (dao.findById(id) == null && !persistedFile.exists()) { deleted = true; break; }
                    SystemClock.sleep(100);
                }
            }
            assertTrue("Deleting a note removes both SQLite metadata and audio", deleted);
        } finally {
            try (NotesDatabaseHelper helper = new NotesDatabaseHelper(InstrumentationRegistry.getInstrumentation().getTargetContext())) {
                helper.getWritableDatabase().delete("subjects", "name = ?", new String[]{otherSubject});
            }
            try (NoteDao dao = new NoteDao(InstrumentationRegistry.getInstrumentation().getTargetContext())) {
                if (id >= 0) dao.delete(id);
                for (Note note : dao.findBySubject(null)) {
                    if (note.topic.startsWith(topic)) {
                        dao.delete(note.id);
                        new dev.josegaldamez.exa1par6818.dao.AudioRecordingDao(
                                InstrumentationRegistry.getInstrumentation().getTargetContext()).delete(new File(note.audioPath));
                    }
                }
            }
            if (persistedFile != null) new dev.josegaldamez.exa1par6818.dao.AudioRecordingDao(
                    InstrumentationRegistry.getInstrumentation().getTargetContext()).delete(persistedFile);
        }
    }

    private Note awaitNote(String topic) {
        long deadline = SystemClock.elapsedRealtime() + 10000;
        try (NoteDao dao = new NoteDao(InstrumentationRegistry.getInstrumentation().getTargetContext())) {
            while (SystemClock.elapsedRealtime() < deadline) {
                for (Note note : dao.findBySubject(null)) if (note.topic.equals(topic)) return note;
                SystemClock.sleep(100);
            }
        }
        throw new AssertionError("Note was not persisted: " + topic);
    }

    private void waitForText(String text) {
        long deadline = SystemClock.elapsedRealtime() + 8000;
        do {
            try { onView(withText(text)).check(matches(isDisplayed())); return; }
            catch (androidx.test.espresso.NoMatchingViewException | AssertionError ignored) { SystemClock.sleep(100); }
        } while (SystemClock.elapsedRealtime() < deadline);
        onView(withText(text)).check(matches(isDisplayed()));
    }

    private void waitForView(int id) {
        long deadline = SystemClock.elapsedRealtime() + 8000;
        do {
            try { onView(withId(id)).check(matches(isDisplayed())); return; }
            catch (androidx.test.espresso.NoMatchingViewException | AssertionError ignored) { SystemClock.sleep(100); }
        } while (SystemClock.elapsedRealtime() < deadline);
        onView(withId(id)).check(matches(isDisplayed()));
    }
}
