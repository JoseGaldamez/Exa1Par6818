package dev.josegaldamez.exa1par6818;

import android.app.Application;
import dev.josegaldamez.exa1par6818.views.MainActivity;
import dev.josegaldamez.exa1par6818.controllers.AudioRecordingController;
import dev.josegaldamez.exa1par6818.models.AudioRecordingState;
import android.media.MediaMetadataRetriever;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.view.View;

import androidx.lifecycle.ViewModelProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileInputStream;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class AudioRecordingTest {
    @Test
    public void recordPauseResumeFinishAndPlayOneAudioFile() throws Exception {
        String packageName = InstrumentationRegistry.getInstrumentation()
                .getTargetContext().getPackageName();
        try (ParcelFileDescriptor descriptor = InstrumentationRegistry.getInstrumentation()
                .getUiAutomation().executeShellCommand("pm grant " + packageName
                        + " android.permission.RECORD_AUDIO");
             FileInputStream output = new FileInputStream(descriptor.getFileDescriptor())) {
            byte[] buffer = new byte[256];
            while (output.read(buffer) != -1) { /* Wait for the permission command to finish. */ }
        }
        AtomicReference<AudioRecordingController> model = new AtomicReference<>();
        AtomicReference<File> audioFile = new AtomicReference<>();
        AtomicLong pausedAt = new AtomicLong();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                model.set(new ViewModelProvider(activity).get(AudioRecordingController.class));
                assertEquals(View.GONE, activity.findViewById(R.id.playControl).getVisibility());
                activity.findViewById(R.id.recordButton).performClick();
                assertEquals(AudioRecordingState.Mode.RECORDING, model.get().mode());
                assertEquals(View.VISIBLE, activity.findViewById(R.id.activeRecordingControls).getVisibility());
                assertEquals(View.VISIBLE, activity.findViewById(R.id.recordingWave).getVisibility());
                assertEquals(View.GONE, activity.findViewById(R.id.playControl).getVisibility());
            });
            SystemClock.sleep(1500);
            scenario.onActivity(activity -> {
                activity.findViewById(R.id.pauseRecordingButton).performClick();
                assertEquals(AudioRecordingState.Mode.PAUSED, model.get().mode());
                pausedAt.set(model.get().state().getValue().elapsedMs);
            });
            SystemClock.sleep(1500);
            scenario.recreate();
            scenario.onActivity(activity -> {
                AudioRecordingController retained = new ViewModelProvider(activity).get(AudioRecordingController.class);
                assertSame(model.get(), retained);
                assertEquals(AudioRecordingState.Mode.PAUSED, retained.mode());
                assertEquals(pausedAt.get(), retained.state().getValue().elapsedMs);
                activity.findViewById(R.id.pauseRecordingButton).performClick();
                assertEquals(AudioRecordingState.Mode.RECORDING, retained.mode());
            });
            SystemClock.sleep(1500);
            scenario.onActivity(activity -> {
                activity.findViewById(R.id.finishRecordingButton).performClick();
                assertEquals(AudioRecordingState.Mode.READY, model.get().mode());
                audioFile.set(model.get().getCompletedRecording().getFile());
                assertTrue(audioFile.get().isFile());
                assertTrue(audioFile.get().length() > 0);
                assertEquals(View.VISIBLE, activity.findViewById(R.id.playControl).getVisibility());
                assertEquals(View.GONE, activity.findViewById(R.id.activeRecordingControls).getVisibility());
            });
            long duration;
            try (MediaMetadataRetriever metadata = new MediaMetadataRetriever()) {
                metadata.setDataSource(audioFile.get().getAbsolutePath());
                assertEquals("audio/mp4", metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE));
                duration = Long.parseLong(metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                assertTrue("Recorded audio contains both segments", duration >= 2000);
                assertTrue("Paused time is excluded from the audio", duration < 4500);
            }
            scenario.recreate();
            scenario.onActivity(activity -> {
                assertEquals(audioFile.get(), model.get().getCompletedRecording().getFile());
                activity.findViewById(R.id.playButton).performClick();
                assertEquals(AudioRecordingState.Mode.PLAYING, model.get().mode());
            });
            SystemClock.sleep(duration + 1800);
            scenario.onActivity(activity -> {
                assertEquals(AudioRecordingState.Mode.READY, model.get().mode());
                // Verify a completed draft can also be restored after process recreation.
                androidx.lifecycle.SavedStateHandle saved = new androidx.lifecycle.SavedStateHandle();
                saved.set("audio_path", audioFile.get().getAbsolutePath());
                saved.set("audio_duration", duration);
                AudioRecordingController restored = new AudioRecordingController(
                        (Application) activity.getApplicationContext(), saved);
                assertEquals(AudioRecordingState.Mode.READY, restored.mode());
                assertEquals(audioFile.get(), restored.getCompletedRecording().getFile());
            });
        } finally {
            if (audioFile.get() != null) audioFile.get().delete();
        }
    }
}
