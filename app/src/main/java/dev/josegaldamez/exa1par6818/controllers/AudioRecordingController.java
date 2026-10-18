package dev.josegaldamez.exa1par6818.controllers;

import dev.josegaldamez.exa1par6818.R;
import dev.josegaldamez.exa1par6818.dao.AudioRecordingDao;
import dev.josegaldamez.exa1par6818.models.AudioRecording;
import dev.josegaldamez.exa1par6818.models.AudioRecordingState;
import dev.josegaldamez.exa1par6818.models.AudioRecordingState.Mode;

import android.app.Application;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;

import java.io.File;
import java.io.IOException;

/** MVC audio controller. AndroidViewModel retains its session across screen rotation. */
public class AudioRecordingController extends AndroidViewModel {
    private final MutableLiveData<AudioRecordingState> state = new MutableLiveData<>();
    private final MutableLiveData<Integer> error = new MutableLiveData<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SavedStateHandle savedState;
    private final AudioManager audioManager;
    private final AudioAttributes audioAttributes = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build();
    private final AudioManager.OnAudioFocusChangeListener focusListener = change -> {
        if (change != AudioManager.AUDIOFOCUS_GAIN) stopPlayback();
    };
    private AudioFocusRequest focusRequest;
    private MediaRecorder recorder;
    private MediaPlayer player;
    private File pendingFile;
    private AudioRecording completedRecording;
    private final AudioRecordingDao recordingDao;
    private Mode mode = Mode.IDLE;
    private long recordedMs;
    private long segmentStartedAt;
    private boolean playerPrepared;
    private boolean completedPersisted;

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            float amplitude = 0;
            if (mode == Mode.RECORDING && recorder != null) {
                try {
                    amplitude = Math.min(1f, recorder.getMaxAmplitude() / 32767f);
                } catch (RuntimeException exception) {
                    failRecording(R.string.recording_error);
                    return;
                }
            }
            publish(amplitude);
            if (mode == Mode.RECORDING || mode == Mode.PLAYING) handler.postDelayed(this, 100);
        }
    };

    public AudioRecordingController(@NonNull Application application, SavedStateHandle savedState) {
        super(application);
        this.savedState = savedState;
        audioManager = (AudioManager) application.getSystemService(Application.AUDIO_SERVICE);
        recordingDao = new AudioRecordingDao(application);
        String path = savedState.get("audio_path");
        Long duration = savedState.get("audio_duration");
        completedRecording = recordingDao.findByPath(path, duration == null ? 0 : duration);
        completedPersisted = Boolean.TRUE.equals(savedState.get("audio_persisted"));
        if (completedRecording != null) mode = Mode.READY;
        publish(0);
    }

    public LiveData<AudioRecordingState> state() { return state; }
    public LiveData<Integer> errors() { return error; }
    public void consumeError() { error.setValue(null); }
    public Mode mode() { return mode; }
    public AudioRecording getCompletedRecording() { return completedRecording; }

    /** Borrow an existing saved recording for playback without taking ownership of its file. */
    public boolean loadSavedRecording(String path, long durationMs) {
        if (recorder != null) return false;
        stopPlayback();
        AudioRecording recording = recordingDao.findByPath(path, durationMs);
        if (recording == null) {
            error.setValue(R.string.audio_missing);
            return false;
        }
        clearDraft();
        completedRecording = recording;
        completedPersisted = true;
        savedState.set("audio_path", path);
        savedState.set("audio_duration", durationMs);
        savedState.set("audio_persisted", true);
        mode = Mode.READY;
        publish(0);
        return true;
    }

    /** Clear the editor after saving, deleting only its temporary recording. */
    public void clearDraft() {
        if (recorder != null) return;
        stopPlayback();
        if (completedRecording != null && !completedPersisted) recordingDao.delete(completedRecording.getFile());
        completedRecording = null;
        completedPersisted = false;
        savedState.set("audio_path", null);
        savedState.set("audio_duration", null);
        savedState.set("audio_persisted", false);
        mode = Mode.IDLE;
        publish(0);
    }

    @SuppressWarnings("deprecation")
    public void startRecording() {
        if (mode == Mode.RECORDING || mode == Mode.PAUSED) return;
        stopPlayback();
        try {
            pendingFile = recordingDao.createDraft();
            recorder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    ? new MediaRecorder(getApplication()) : new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setAudioEncodingBitRate(128000);
            recorder.setAudioSamplingRate(44100);
            recorder.setOutputFile(pendingFile.getAbsolutePath());
            recorder.setOnErrorListener((source, what, extra) -> failRecording(R.string.recording_error));
            recorder.prepare();
            recorder.start();
            recordedMs = 0;
            segmentStartedAt = SystemClock.elapsedRealtime();
            mode = Mode.RECORDING;
            restartTicker();
        } catch (IOException | RuntimeException exception) {
            failRecording(R.string.recording_error);
        }
    }

    public void toggleRecordingPause() {
        if (recorder == null) return;
        try {
            if (mode == Mode.RECORDING) {
                recorder.pause();
                recordedMs += SystemClock.elapsedRealtime() - segmentStartedAt;
                mode = Mode.PAUSED;
                handler.removeCallbacks(ticker);
                publish(0);
            } else if (mode == Mode.PAUSED) {
                recorder.resume();
                segmentStartedAt = SystemClock.elapsedRealtime();
                mode = Mode.RECORDING;
                restartTicker();
            }
        } catch (RuntimeException exception) {
            failRecording(R.string.recording_error);
        }
    }

    public void finishRecording() {
        if (recorder == null || (mode != Mode.RECORDING && mode != Mode.PAUSED)) return;
        long duration = recordingDuration();
        handler.removeCallbacks(ticker);
        try {
            // stop() can fail if the encoder has not received enough audio yet.
            recorder.stop();
            releaseRecorder();
            AudioRecording previousRecording = completedRecording;
            boolean previousPersisted = completedPersisted;
            completedRecording = recordingDao.completeDraft(pendingFile, duration);
            pendingFile = null;
            completedPersisted = false;
            savedState.set("audio_persisted", false);
            savedState.set("audio_path", completedRecording.getFile().getAbsolutePath());
            savedState.set("audio_duration", completedRecording.getDurationMs());
            // Only replace the previous draft after the new recording is finalized successfully.
            if (previousRecording != null && !previousPersisted) recordingDao.delete(previousRecording.getFile());
            mode = Mode.READY;
            publish(0);
        } catch (IOException | RuntimeException exception) {
            failRecording(R.string.recording_too_short);
        }
    }

    public void togglePlayback() {
        if (mode == Mode.PLAYING) {
            stopPlayback();
            return;
        }
        if (mode != Mode.READY || completedRecording == null) return;
        try {
            if (!requestAudioFocus()) {
                error.setValue(R.string.playback_error);
                return;
            }
            player = new MediaPlayer();
            MediaPlayer preparingPlayer = player;
            playerPrepared = false;
            player.setAudioAttributes(audioAttributes);
            player.setDataSource(completedRecording.getFile().getAbsolutePath());
            player.setOnPreparedListener(prepared -> {
                if (player != preparingPlayer || mode != Mode.PLAYING) return;
                playerPrepared = true;
                prepared.start();
                restartTicker();
            });
            player.setOnCompletionListener(finished -> stopPlayback());
            player.setOnErrorListener((failed, what, extra) -> {
                stopPlayback();
                error.setValue(R.string.playback_error);
                return true;
            });
            mode = Mode.PLAYING;
            publish(0);
            player.prepareAsync();
        } catch (IOException | RuntimeException exception) {
            stopPlayback();
            error.setValue(R.string.playback_error);
        }
    }

    /** Keep the same recording, but never continue capturing after leaving this screen. */
    public void onScreenHidden() {
        if (mode == Mode.RECORDING) toggleRecordingPause();
        stopPlayback();
    }

    private long recordingDuration() {
        return recordedMs + (mode == Mode.RECORDING
                ? SystemClock.elapsedRealtime() - segmentStartedAt : 0);
    }

    private void publish(float amplitude) {
        long elapsed = completedRecording == null ? 0 : completedRecording.getDurationMs();
        if (mode == Mode.RECORDING || mode == Mode.PAUSED) elapsed = recordingDuration();
        if (mode == Mode.PLAYING) {
            try {
                elapsed = playerPrepared && player != null ? player.getCurrentPosition() : 0;
            } catch (IllegalStateException exception) {
                elapsed = 0;
            }
        }
        state.setValue(new AudioRecordingState(mode, elapsed, amplitude));
    }

    private void restartTicker() {
        handler.removeCallbacks(ticker);
        ticker.run();
    }

    private void failRecording(int message) {
        handler.removeCallbacks(ticker);
        releaseRecorder();
        recordingDao.delete(pendingFile);
        pendingFile = null;
        mode = completedRecording == null ? Mode.IDLE : Mode.READY;
        publish(0);
        error.setValue(message);
    }

    private void releaseRecorder() {
        if (recorder != null) {
            recorder.setOnErrorListener(null);
            recorder.release();
            recorder = null;
        }
    }

    private void stopPlayback() {
        if (player != null) {
            player.setOnPreparedListener(null);
            player.setOnCompletionListener(null);
            player.setOnErrorListener(null);
            player.release();
            player = null;
        }
        playerPrepared = false;
        abandonAudioFocus();
        if (mode == Mode.PLAYING) {
            handler.removeCallbacks(ticker);
            mode = completedRecording == null ? Mode.IDLE : Mode.READY;
            publish(0);
        }
    }

    @SuppressWarnings("deprecation")
    private boolean requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(audioAttributes)
                    .setOnAudioFocusChangeListener(focusListener).build();
            return audioManager.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        }
        return audioManager.requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    @SuppressWarnings("deprecation")
    private void abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && focusRequest != null) {
            audioManager.abandonAudioFocusRequest(focusRequest);
            focusRequest = null;
        } else {
            audioManager.abandonAudioFocus(focusListener);
        }
    }

    @Override protected void onCleared() {
        if (recorder != null) finishRecording();
        releaseRecorder();
        stopPlayback();
        if (completedRecording != null && !completedPersisted) recordingDao.delete(completedRecording.getFile());
        handler.removeCallbacks(ticker);
    }
}
