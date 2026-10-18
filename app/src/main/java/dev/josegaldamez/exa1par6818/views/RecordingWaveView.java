package dev.josegaldamez.exa1par6818.views;

import dev.josegaldamez.exa1par6818.R;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

/** Animated recording indicator; the microphone level also changes the wave height. */
public class RecordingWaveView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ValueAnimator animator = ValueAnimator.ofFloat(0f, (float) (Math.PI * 2));
    private float phase;
    private float level;
    private boolean recording;

    public RecordingWaveView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        paint.setColor(ContextCompat.getColor(context, R.color.record_red));
        animator.setDuration(900);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.addUpdateListener(value -> {
            phase = (float) value.getAnimatedValue();
            invalidate();
        });
    }

    void setRecording(boolean active, float amplitude) {
        recording = active;
        level = amplitude;
        if (active && isAttachedToWindow() && !animator.isStarted()) animator.start();
        if (!active) animator.cancel();
        setAlpha(active ? 1f : 0.35f);
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float density = getResources().getDisplayMetrics().density;
        float step = Math.min(getWidth() / 21f, 11 * density);
        float width = Math.min(4 * density, step * 0.5f);
        for (int i = 0; i < 21; i++) {
            float wave = recording ? (float) (0.5 + 0.5 * Math.sin(phase + i * 0.7)) : 0.1f;
            float height = 4 * density + wave * (getHeight() - 4 * density) * (0.3f + 0.7f * level);
            float left = getWidth() / 2f + (i - 10) * step - width / 2;
            canvas.drawRoundRect(left, (getHeight() - height) / 2, left + width,
                    (getHeight() + height) / 2, width / 2, width / 2, paint);
        }
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (recording) animator.start();
    }

    @Override protected void onDetachedFromWindow() {
        animator.cancel();
        super.onDetachedFromWindow();
    }
}
