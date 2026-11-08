package com.example.andrs.presentation.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.andrs.R;
import com.google.android.material.color.MaterialColors;

/**
 * Anillo del ciclo: segmentos coloreados por fase (menstrual, folicular, ovulación, lútea) y un
 * marcador en el día actual. En el centro muestra el día del ciclo y la fase.
 */
public class CycleRingView extends View {
    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint segment = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint marker = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint markerRing = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bigText = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint smallText = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();

    private int cycleLength = 28;
    private int periodLength = 5;
    private int ovulationDay = 14;
    private int currentDay = 0;
    private String centerTitle = "";
    private String centerSubtitle = "";

    private final int colorMenstrual;
    private final int colorFollicular;
    private final int colorOvulation;
    private final int colorLuteal;

    public CycleRingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        float stroke = dp(18);
        track.setStyle(Paint.Style.STROKE);
        track.setStrokeWidth(stroke);
        track.setColor(ContextCompat.getColor(context, R.color.ring_track));
        segment.setStyle(Paint.Style.STROKE);
        segment.setStrokeWidth(stroke);
        segment.setStrokeCap(Paint.Cap.BUTT);
        marker.setStyle(Paint.Style.FILL);
        markerRing.setStyle(Paint.Style.STROKE);
        markerRing.setStrokeWidth(dp(3));

        int onSurface = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface);
        int onSurfaceVariant = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant);
        marker.setColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface));
        markerRing.setColor(onSurface);
        bigText.setColor(onSurface);
        bigText.setTextAlign(Paint.Align.CENTER);
        bigText.setTextSize(sp(40));
        bigText.setFakeBoldText(true);
        smallText.setColor(onSurfaceVariant);
        smallText.setTextAlign(Paint.Align.CENTER);
        smallText.setTextSize(sp(16));

        colorMenstrual = ContextCompat.getColor(context, R.color.phase_menstrual);
        colorFollicular = ContextCompat.getColor(context, R.color.phase_follicular);
        colorOvulation = ContextCompat.getColor(context, R.color.phase_ovulation);
        colorLuteal = ContextCompat.getColor(context, R.color.phase_luteal);
    }

    public void setCycle(int cycleLength, int periodLength, int ovulationDay, int currentDay,
                         String title, String subtitle) {
        this.cycleLength = Math.max(cycleLength, 1);
        this.periodLength = Math.max(1, Math.min(periodLength, this.cycleLength));
        this.ovulationDay = ovulationDay;
        this.currentDay = currentDay;
        this.centerTitle = title;
        this.centerSubtitle = subtitle;
        setContentDescription(title + ", " + subtitle);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float size = Math.min(getWidth(), getHeight());
        float pad = track.getStrokeWidth() / 2 + dp(10);
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float r = size / 2f - pad;
        oval.set(cx - r, cy - r, cx + r, cy + r);
        canvas.drawArc(oval, 0, 360, false, track);

        if (currentDay > 0) {
            float perDay = 360f / cycleLength;
            float gap = 1.2f;
            int ovStart = Math.max(periodLength + 1, ovulationDay - 1);
            int ovEnd = Math.min(cycleLength, ovulationDay + 1);
            drawSegment(canvas, 1, periodLength, perDay, gap, colorMenstrual);
            drawSegment(canvas, periodLength + 1, ovStart - 1, perDay, gap, colorFollicular);
            drawSegment(canvas, ovStart, ovEnd, perDay, gap, colorOvulation);
            drawSegment(canvas, ovEnd + 1, cycleLength, perDay, gap, colorLuteal);

            int day = Math.min(currentDay, cycleLength);
            double angle = Math.toRadians(-90 + (day - 0.5) * perDay);
            float mx = (float) (cx + r * Math.cos(angle));
            float my = (float) (cy + r * Math.sin(angle));
            canvas.drawCircle(mx, my, dp(12), marker);
            canvas.drawCircle(mx, my, dp(12), markerRing);
        }

        float textY = cy - (bigText.descent() + bigText.ascent()) / 2 - dp(10);
        canvas.drawText(centerTitle, cx, textY, bigText);
        canvas.drawText(centerSubtitle, cx, textY + dp(30), smallText);
    }

    private void drawSegment(Canvas canvas, int fromDay, int toDay, float perDay, float gap, int color) {
        if (toDay < fromDay) return;
        float start = -90 + (fromDay - 1) * perDay + gap / 2;
        float sweep = (toDay - fromDay + 1) * perDay - gap;
        if (sweep <= 0) return;
        segment.setColor(color);
        canvas.drawArc(oval, start, sweep, false, segment);
    }

    private float dp(float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    private float sp(float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, getResources().getDisplayMetrics());
    }
}
