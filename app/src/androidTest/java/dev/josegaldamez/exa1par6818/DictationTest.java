package dev.josegaldamez.exa1par6818;

import android.Manifest;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.ParcelFileDescriptor;
import android.speech.RecognizerIntent;
import android.widget.EditText;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;

import dev.josegaldamez.exa1par6818.views.MainActivity;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class DictationTest {
    @Test public void recognizedTextCanBeEditedAndReturnedWithoutChangingOtherFields() throws Exception {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        // The recognizer returns a controlled result; no microphone audio is captured by this test.
        try (ParcelFileDescriptor descriptor = instrumentation.getUiAutomation().executeShellCommand(
                "pm grant " + instrumentation.getTargetContext().getPackageName() + " " + Manifest.permission.RECORD_AUDIO);
             FileInputStream stream = new FileInputStream(descriptor.getFileDescriptor())) {
            byte[] buffer = new byte[256];
            while (stream.read(buffer) != -1) { }
        }
        Intent result = new Intent().putStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS,
                new ArrayList<>(Collections.singletonList("La Activity pasa a onPause")));
        Instrumentation.ActivityMonitor monitor = new Instrumentation.ActivityMonitor() {
            @Override public Instrumentation.ActivityResult onStartActivity(Intent intent) {
                return RecognizerIntent.ACTION_RECOGNIZE_SPEECH.equals(intent.getAction())
                        ? new Instrumentation.ActivityResult(Activity.RESULT_OK, result) : null;
            }
        };
        instrumentation.addMonitor(monitor);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                ((EditText) activity.findViewById(R.id.topicInput)).setText("Ciclo de vida de una Activity");
                ((EditText) activity.findViewById(R.id.descriptionInput)).setText("Descripción inicial.");
            });
            onView(withId(R.id.dictateDescriptionButton)).perform(scrollTo(), click());
            onView(withId(R.id.dictationTopic)).check(matches(withText("Ciclo de vida de una Activity")));
            onView(withId(R.id.startDictationButton)).perform(scrollTo(), click());
            onView(withId(R.id.dictationInput)).check(matches(
                    withText("Descripción inicial. La Activity pasa a onPause")));
            assertEquals(1, monitor.getHits());
            onView(withId(R.id.dictationInput)).perform(scrollTo(), replaceText("Descripción revisada."), closeSoftKeyboard());
            onView(withId(R.id.saveDescriptionButton)).perform(scrollTo(), click());
            onView(withId(R.id.descriptionInput)).check(matches(withText("Descripción revisada.")));
            onView(withId(R.id.topicInput)).check(matches(withText("Ciclo de vida de una Activity")));
        } finally { instrumentation.removeMonitor(monitor); }
    }

    @Test public void leavingWithoutSavingKeepsTheOriginalDescription() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity ->
                    ((EditText) activity.findViewById(R.id.descriptionInput)).setText("Descripción original"));
            onView(withId(R.id.dictateDescriptionButton)).perform(scrollTo(), click());
            onView(withId(R.id.dictationInput)).perform(scrollTo(), replaceText("Cambio sin guardar"), closeSoftKeyboard());
            onView(withContentDescription(R.string.back)).perform(click());
            onView(withId(R.id.descriptionInput)).check(matches(withText("Descripción original")));
        }
    }
}
