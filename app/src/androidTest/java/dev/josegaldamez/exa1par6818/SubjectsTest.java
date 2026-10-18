package dev.josegaldamez.exa1par6818;

import android.content.Context;
import android.content.ContextWrapper;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.widget.EditText;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

import dev.josegaldamez.exa1par6818.controllers.NoteFormController;
import dev.josegaldamez.exa1par6818.dao.NotesDatabaseHelper;
import dev.josegaldamez.exa1par6818.dao.SubjectDao;
import dev.josegaldamez.exa1par6818.models.NoteDraft;
import dev.josegaldamez.exa1par6818.views.MainActivity;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class SubjectsTest {
    private final Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();

    private Context isolatedDatabase(String databaseName) {
        return new ContextWrapper(target) {
            @Override public Context getApplicationContext() { return this; }
            @Override public SQLiteDatabase openOrCreateDatabase(String name, int mode,
                    SQLiteDatabase.CursorFactory factory, DatabaseErrorHandler handler) {
                return target.openOrCreateDatabase(databaseName, mode, factory, handler);
            }
        };
    }

    @Test public void startsWithOneSubjectAndPersistsNewNamesWithoutDuplicates() {
        String database = "subjects_catalog_test.db";
        target.deleteDatabase(database);
        Context context = isolatedDatabase(database);
        try {
            try (SubjectDao dao = new SubjectDao(context)) {
                assertArrayEquals(new String[]{"Programación Móvil 1"}, dao.findAll());
                assertFalse(dao.add("   "));
                assertTrue(dao.add("  Matemáticas  "));
                assertFalse(dao.add("matemáticas"));
            }
            try (SubjectDao dao = new SubjectDao(context)) {
                assertArrayEquals(new String[]{"Programación Móvil 1", "Matemáticas"}, dao.findAll());
                assertTrue(new NoteFormController(dao.findAll()).validate(
                        new NoteDraft("Matemáticas", "Tema", "2026-10-18", "Descripción")).isEmpty());
            }
        } finally { target.deleteDatabase(database); }
    }

    @Test public void migrationPreservesExistingNotesAndTheirSubjects() {
        String database = "subjects_migration_test.db";
        target.deleteDatabase(database);
        Context context = isolatedDatabase(database);
        try {
            try (SQLiteDatabase db = target.openOrCreateDatabase(database, Context.MODE_PRIVATE, null)) {
                db.execSQL("CREATE TABLE notes (_id INTEGER PRIMARY KEY, subject TEXT, topic TEXT)");
                db.execSQL("INSERT INTO notes VALUES (7, 'Base de Datos I', 'Apunte anterior')");
                db.setVersion(1);
            }
            try (SubjectDao dao = new SubjectDao(context);
                 NotesDatabaseHelper helper = new NotesDatabaseHelper(context)) {
                assertArrayEquals(new String[]{"Programación Móvil 1", "Base de Datos I"}, dao.findAll());
                try (android.database.Cursor cursor = helper.getReadableDatabase()
                        .rawQuery("SELECT topic FROM notes WHERE _id = 7", null)) {
                    assertTrue(cursor.moveToFirst());
                    assertEquals("Apunte anterior", cursor.getString(0));
                }
            }
        } finally { target.deleteDatabase(database); }
    }

    @Test public void modalCreatesSelectsAndRestoresSubjectFromSQLite() {
        String name = "Asignatura de prueba " + System.currentTimeMillis();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.addSubjectButton)).perform(scrollTo(), click());
            onView(withId(android.R.id.button1)).perform(click());
            onView(withText(R.string.required_error)).check(matches(isDisplayed()));
            onView(withId(R.id.newSubjectInput)).perform(replaceText(name), closeSoftKeyboard());
            onView(withId(android.R.id.button1)).perform(click());
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            awaitSubject(name);
            onView(withId(R.id.subjectInput)).check(matches(withText(name)));
            scenario.recreate();
            awaitSubject(name);
            scenario.onActivity(activity -> assertEquals(name,
                    ((MaterialAutoCompleteTextView) activity.findViewById(R.id.subjectInput)).getText().toString()));
            onView(withId(R.id.addSubjectButton)).perform(scrollTo(), click());
            onView(withId(R.id.newSubjectInput)).perform(replaceText(name), closeSoftKeyboard());
            onView(withId(android.R.id.button1)).perform(click());
            onView(withText(R.string.subject_exists)).check(matches(isDisplayed()));
            onView(withText(R.string.cancel)).perform(click());
        } finally {
            try (NotesDatabaseHelper helper = new NotesDatabaseHelper(target)) {
                helper.getWritableDatabase().delete("subjects", "name = ?", new String[]{name});
            }
        }
    }

    private void awaitSubject(String name) {
        long deadline = android.os.SystemClock.elapsedRealtime() + 5000;
        try (SubjectDao dao = new SubjectDao(target)) {
            while (android.os.SystemClock.elapsedRealtime() < deadline) {
                if (Arrays.asList(dao.findAll()).contains(name)) {
                    InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                    return;
                }
                android.os.SystemClock.sleep(50);
            }
        }
        fail("The new subject was not persisted in SQLite");
    }
}
