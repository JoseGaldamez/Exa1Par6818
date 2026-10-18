package dev.josegaldamez.exa1par6818;

import android.os.SystemClock;
import android.text.Layout;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicBoolean;

import dev.josegaldamez.exa1par6818.views.MainActivity;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class DescriptionKeyboardTest {
    @Test
    public void descriptionAndMultilineCursorStayAboveKeyboardAndOutsideTapHidesIt() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                EditText input = activity.findViewById(R.id.descriptionInput);
                input.requestFocus();
                WindowCompat.getInsetsController(activity.getWindow(), input)
                        .show(WindowInsetsCompat.Type.ime());
            });
            awaitKeyboard(scenario, true);
            SystemClock.sleep(500);
            scenario.onActivity(activity -> {
                EditText input = activity.findViewById(R.id.descriptionInput);
                input.setText("Primera línea del apunte");
                input.setSelection(input.length());
            });
            SystemClock.sleep(500);
            scenario.onActivity(activity -> {
                EditText input = activity.findViewById(R.id.descriptionInput);
                int[] position = new int[2];
                input.getLocationOnScreen(position);
                assertTrue("The entire description field is above the IME",
                        position[1] + input.getHeight() <= keyboardTop(activity));
                StringBuilder text = new StringBuilder();
                for (int line = 0; line < 25; line++) text.append("Línea del apunte ").append(line).append('\n');
                input.setText(text.toString());
                input.setSelection(input.length());
            });
            SystemClock.sleep(500);
            scenario.onActivity(activity -> {
                EditText input = activity.findViewById(R.id.descriptionInput);
                Layout layout = input.getLayout();
                int line = layout.getLineForOffset(input.getSelectionEnd());
                int[] position = new int[2];
                input.getLocationOnScreen(position);
                int cursorBottom = position[1] + input.getTotalPaddingTop()
                        + layout.getLineBottom(line) - input.getScrollY();
                assertTrue("The current line stays above the IME", cursorBottom <= keyboardTop(activity));
                int[] scrollPosition = new int[2];
                View scroll = activity.findViewById(R.id.main);
                scroll.getLocationOnScreen(scrollPosition);
                float y = scrollPosition[1] + scroll.getPaddingTop() + 2;
                long now = SystemClock.uptimeMillis();
                MotionEvent down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 2, y, 0);
                MotionEvent up = MotionEvent.obtain(now, now + 10, MotionEvent.ACTION_UP, 2, y, 0);
                activity.dispatchTouchEvent(down);
                activity.dispatchTouchEvent(up);
                down.recycle();
                up.recycle();
            });
            awaitKeyboard(scenario, false);
        }
    }

    private int keyboardTop(MainActivity activity) {
        View decor = activity.getWindow().getDecorView();
        WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(decor);
        assertNotNull(insets);
        assertTrue(insets.isVisible(WindowInsetsCompat.Type.ime()));
        int[] position = new int[2];
        decor.getLocationOnScreen(position);
        return position[1] + decor.getHeight() - insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
    }

    private void awaitKeyboard(ActivityScenario<MainActivity> scenario, boolean visible) {
        AtomicBoolean matched = new AtomicBoolean();
        for (int attempt = 0; attempt < 80; attempt++) {
            scenario.onActivity(activity -> {
                WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(activity.findViewById(R.id.main));
                matched.set(insets != null && insets.isVisible(WindowInsetsCompat.Type.ime()) == visible);
            });
            if (matched.get()) return;
            SystemClock.sleep(100);
        }
        fail("Keyboard visibility did not become " + visible);
    }
}
