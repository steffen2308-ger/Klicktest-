package de.steffen.klicktest;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/** Zeichnet einen klassischen Mauspfeil. Die Pfeilspitze liegt bei (TIP_OFFSET, TIP_OFFSET). */
final class CursorView extends View {
    static final float TIP_OFFSET_DP = 2f;
    static final float WIDTH_DP = 20f;
    static final float HEIGHT_DP = 28f;

    private final float density;
    private final Path arrow = new Path();
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean flashing;

    CursorView(Context context) {
        super(context);
        density = context.getResources().getDisplayMetrics().density;
        float o = TIP_OFFSET_DP * density;
        float s = 1.25f * density;
        arrow.moveTo(o, o);
        arrow.lineTo(o, o + 17 * s);
        arrow.lineTo(o + 4.5f * s, o + 13 * s);
        arrow.lineTo(o + 7.5f * s, o + 20 * s);
        arrow.lineTo(o + 10f * s, o + 19 * s);
        arrow.lineTo(o + 7f * s, o + 12 * s);
        arrow.lineTo(o + 12.5f * s, o + 12 * s);
        arrow.close();
        fill.setStyle(Paint.Style.FILL);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(1.4f * density);
        stroke.setStrokeJoin(Paint.Join.ROUND);
        stroke.setColor(Color.BLACK);
    }

    int widthPx() { return Math.round(WIDTH_DP * density); }
    int heightPx() { return Math.round(HEIGHT_DP * density); }
    int tipOffsetPx() { return Math.round(TIP_OFFSET_DP * density); }

    /** Kurzes farbiges Aufleuchten als Rückmeldung für einen Klick. */
    void flash() {
        flashing = true;
        invalidate();
        postDelayed(() -> { flashing = false; invalidate(); }, 180);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        fill.setColor(flashing ? Color.rgb(79, 209, 197) : Color.WHITE);
        canvas.drawPath(arrow, fill);
        canvas.drawPath(arrow, stroke);
    }
}
