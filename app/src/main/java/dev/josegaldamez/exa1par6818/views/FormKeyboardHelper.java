package dev.josegaldamez.exa1par6818.views;

import android.graphics.Rect;
import android.text.Editable;
import android.text.Layout;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

import java.util.List;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsAnimationCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;

/** Keeps the focused field (or its current line for long notes) above the keyboard. */
final class FormKeyboardHelper {
    private final NestedScrollView scroll;
    private final Runnable revealFocus = this::revealFocusedInput;

    FormKeyboardHelper(View root, NestedScrollView scroll, EditText... inputs) {
        this.scroll = scroll;
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets keyboard = insets.getInsets(WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, keyboard.bottom));
            scheduleReveal();
            return insets;
        });
        ViewCompat.setWindowInsetsAnimationCallback(scroll, new WindowInsetsAnimationCompat.Callback(
                WindowInsetsAnimationCompat.Callback.DISPATCH_MODE_CONTINUE_ON_SUBTREE) {
            @Override public WindowInsetsCompat onProgress(WindowInsetsCompat insets,
                    List<WindowInsetsAnimationCompat> runningAnimations) {
                return insets;
            }

            @Override public void onEnd(WindowInsetsAnimationCompat animation) {
                scheduleReveal();
            }
        });
        scroll.addOnLayoutChangeListener((view, left, top, right, bottom,
                oldLeft, oldTop, oldRight, oldBottom) -> scheduleReveal());
        for (EditText input : inputs) {
            input.addOnLayoutChangeListener((view, left, top, right, bottom,
                    oldLeft, oldTop, oldRight, oldBottom) -> scheduleReveal());
            input.setOnFocusChangeListener((view, focused) -> {
                if (focused) scheduleReveal();
            });
            input.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
                @Override public void afterTextChanged(Editable text) { scheduleReveal(); }
            });
        }
        ViewCompat.requestApplyInsets(root);
    }

    private void scheduleReveal() {
        scroll.removeCallbacks(revealFocus);
        scroll.post(revealFocus);
    }

    private void revealFocusedInput() {
        WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(scroll);
        View focused = scroll.findFocus();
        if (insets == null || !insets.isVisible(WindowInsetsCompat.Type.ime())
                || !(focused instanceof EditText)) return;
        EditText input = (EditText) focused;
        int margin = Math.round(16 * scroll.getResources().getDisplayMetrics().density);
        int[] scrollLocation = new int[2];
        scroll.getLocationOnScreen(scrollLocation);
        Rect window = new Rect();
        scroll.getWindowVisibleDisplayFrame(window);
        int visibleTop = scrollLocation[1] + scroll.getPaddingTop() + margin;
        int visibleBottom = Math.min(window.bottom,
                scrollLocation[1] + scroll.getHeight() - scroll.getPaddingBottom()) - margin;
        if (visibleBottom <= visibleTop) return;

        int[] inputLocation = new int[2];
        input.getLocationOnScreen(inputLocation);
        int top = inputLocation[1];
        int bottom = top + input.getHeight();
        // A long multiline input may exceed the viewport. In that case follow the cursor.
        if (bottom - top > visibleBottom - visibleTop && input.getLayout() != null) {
            Layout layout = input.getLayout();
            int line = layout.getLineForOffset(Math.max(0, input.getSelectionEnd()));
            top = inputLocation[1] + input.getTotalPaddingTop()
                    + layout.getLineTop(line) - input.getScrollY();
            bottom = inputLocation[1] + input.getTotalPaddingTop()
                    + layout.getLineBottom(line) - input.getScrollY();
        }
        if (bottom > visibleBottom) scroll.scrollBy(0, bottom - visibleBottom);
        else if (top < visibleTop) scroll.scrollBy(0, top - visibleTop);
    }
}
