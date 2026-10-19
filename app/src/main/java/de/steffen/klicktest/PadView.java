package de.steffen.klicktest;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/** Das kleine Touchpad: oben eine Griffleiste zum Verschieben und ein ✕ zum Ausblenden. */
final class PadView extends View {
    static final float WIDTH_DP = 190f;
    static final float HEIGHT_DP = 150f;
    static final float HANDLE_DP = 30f;

    private final float density;
    private final Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint handle = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint title = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    PadView(Context context) {
        super(context);
        density = context.getResources().getDisplayMetrics().density;
        float sp = context.getResources().getDisplayMetrics().scaledDensity;
        bg.setColor(Color.argb(200, 15, 35, 45));
        handle.setColor(Color.argb(230, 30, 111, 143));
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(1.5f * density);
        border.setColor(Color.argb(220, 79, 209, 197));
        title.setColor(Color.WHITE);
        title.setTextSize(13 * sp);
        title.setFakeBoldText(true);
        hint.setColor(Color.argb(220, 230, 240, 245));
        hint.setTextSize(11 * sp);
        hint.setTextAlign(Paint.Align.CENTER);
        setContentDescription("Klicktest-Touchpad");
    }

    int widthPx() { return Math.round(WIDTH_DP * density); }
    int heightPx() { return Math.round(HEIGHT_DP * density); }
    float handlePx() { return HANDLE_DP * density; }

    /** Liegt der Punkt (in View-Koordinaten) auf dem ✕? */
    boolean isOnClose(float x, float y) {
        return y < handlePx() && x > getWidth() - handlePx() * 1.3f;
    }

    boolean isOnHandle(float y) {
        return y < handlePx();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float r = 14 * density;
        float w = getWidth();
        float h = getHeight();
        float hp = handlePx();
        rect.set(0, 0, w, h);
        canvas.drawRoundRect(rect, r, r, bg);

        canvas.save();
        canvas.clipRect(0, 0, w, hp);
        canvas.drawRoundRect(rect, r, r, handle);
        canvas.restore();

        float baseline = hp / 2 - (title.descent() + title.ascent()) / 2;
        canvas.drawText("⠿ Klicktest", 10 * density, baseline, title);
        canvas.drawText("✕", w - hp + 4 * density, baseline, title);

        float cx = w / 2;
        float lh = hint.getTextSize() * 1.35f;
        float y = hp + (h - hp) / 2 - lh * 1.1f;
        canvas.drawText("Wischen: Zeiger bewegen", cx, y, hint);
        canvas.drawText("Tippen: klicken", cx, y + lh, hint);
        canvas.drawText("Halten: langer Klick", cx, y + 2 * lh, hint);
        canvas.drawText("2 Finger: scrollen", cx, y + 3 * lh, hint);

        rect.inset(border.getStrokeWidth() / 2, border.getStrokeWidth() / 2);
        canvas.drawRoundRect(rect, r, r, border);
    }
}
