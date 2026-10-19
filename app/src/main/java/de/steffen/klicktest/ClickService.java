package de.steffen.klicktest;

import android.accessibilityservice.AccessibilityButtonController;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.res.Configuration;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;

/**
 * Bedienungshilfe-Dienst: zeigt Zeiger und Touchpad als Overlay über allen Apps
 * und löst an der Zeigerposition Klicks, lange Klicks und Scroll-Gesten aus.
 */
public class ClickService extends AccessibilityService {

    /** Laufende Instanz (gleicher Prozess wie MainActivity), sonst null. */
    static ClickService instance;

    private static final long TAP_MS = 50;
    private static final long LONG_TAP_MS = 800;
    private static final long LONG_PRESS_DETECT_MS = 500;
    private static final long SCROLL_MS = 280;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private WindowManager wm;
    private CursorView cursor;
    private PadView pad;
    private WindowManager.LayoutParams cursorLp;
    private WindowManager.LayoutParams padLp;
    private boolean shown;
    private boolean gestureRunning;

    private float cursorX;
    private float cursorY;
    private int screenW;
    private int screenH;
    private float density;
    private int touchSlop;

    // Zustand der laufenden Touchpad-Berührung
    private float lastRawX, lastRawY, downRawX, downRawY;
    private boolean moved, onHandle, onClose, twoFinger, longFired;
    private float twoStartX, twoStartY, scrollDx, scrollDy;
    private final Runnable longPressRunnable = () -> {
        if (!moved && !twoFinger && !onHandle) {
            longFired = true;
            tapAtCursor(LONG_TAP_MS);
        }
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        density = getResources().getDisplayMetrics().density;
        touchSlop = ViewConfiguration.get(this).getScaledTouchSlop();
        updateScreenSize();
        cursorX = screenW / 2f;
        cursorY = screenH / 2f;
        createViews();
        showOverlay();
        if (Build.VERSION.SDK_INT >= 26) registerAccessibilityButton();
    }

    @TargetApi(26)
    private void registerAccessibilityButton() {
        getAccessibilityButtonController().registerAccessibilityButtonCallback(
                new AccessibilityButtonController.AccessibilityButtonCallback() {
                    @Override
                    public void onClicked(AccessibilityButtonController controller) {
                        toggleOverlay();
                    }
                });
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Klicktest wertet keine Bildschirminhalte aus.
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onDestroy() {
        hideOverlay();
        handler.removeCallbacksAndMessages(null);
        if (instance == this) instance = null;
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateScreenSize();
        cursorX = CursorMath.clamp(cursorX, 0, screenW - 1);
        cursorY = CursorMath.clamp(cursorY, 0, screenH - 1);
        if (padLp != null) {
            padLp.x = (int) CursorMath.clamp(padLp.x, 0, screenW - pad.widthPx());
            padLp.y = (int) CursorMath.clamp(padLp.y, 0, screenH - pad.heightPx());
        }
        updateCursorLayout();
        updatePadLayout();
    }

    // ---------------------------------------------------------------- Overlay

    private void updateScreenSize() {
        if (Build.VERSION.SDK_INT >= 30) {
            Rect b = wm.getMaximumWindowMetrics().getBounds();
            screenW = b.width();
            screenH = b.height();
        } else {
            DisplayMetrics dm = new DisplayMetrics();
            //noinspection deprecation
            wm.getDefaultDisplay().getRealMetrics(dm);
            screenW = dm.widthPixels;
            screenH = dm.heightPixels;
        }
    }

    private WindowManager.LayoutParams baseParams(int w, int h, int extraFlags) {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                w, h,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | extraFlags,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        if (Build.VERSION.SDK_INT >= 28) {
            lp.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        return lp;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void createViews() {
        cursor = new CursorView(this);
        cursorLp = baseParams(cursor.widthPx(), cursor.heightPx(),
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

        pad = new PadView(this);
        padLp = baseParams(pad.widthPx(), pad.heightPx(), 0);
        int defX = screenW - pad.widthPx() - Math.round(16 * density);
        int defY = screenH - pad.heightPx() - Math.round(140 * density);
        padLp.x = (int) CursorMath.clamp(Prefs.get(this).getInt(Prefs.PAD_X, defX), 0, screenW - pad.widthPx());
        padLp.y = (int) CursorMath.clamp(Prefs.get(this).getInt(Prefs.PAD_Y, defY), 0, screenH - pad.heightPx());
        pad.setOnTouchListener((v, e) -> onPadTouch(e));

        applyPrefs();
        updateCursorLayout();
    }

    /** Übernimmt Sichtbarkeit/Geschwindigkeit aus den Einstellungen. */
    void applyPrefs() {
        if (cursor == null) return;
        cursor.setAlpha(Prefs.cursorAlpha(this) / 100f);
        pad.setAlpha(Prefs.padAlpha(this) / 100f);
    }

    boolean isShown() {
        return shown;
    }

    void toggleOverlay() {
        if (shown) hideOverlay(); else showOverlay();
    }

    void showOverlay() {
        if (shown || wm == null || cursor == null) return;
        try {
            wm.addView(pad, padLp);
            wm.addView(cursor, cursorLp);
            shown = true;
        } catch (RuntimeException ignored) {
            // Fenster konnte nicht angelegt werden (z. B. Dienst wird gerade beendet).
        }
    }

    void hideOverlay() {
        if (!shown || wm == null) return;
        try {
            wm.removeViewImmediate(cursor);
            wm.removeViewImmediate(pad);
        } catch (RuntimeException ignored) {
        }
        shown = false;
    }

    private void updateCursorLayout() {
        if (cursorLp == null) return;
        cursorLp.x = Math.round(cursorX) - cursor.tipOffsetPx();
        cursorLp.y = Math.round(cursorY) - cursor.tipOffsetPx();
        if (shown) wm.updateViewLayout(cursor, cursorLp);
    }

    private void updatePadLayout() {
        if (shown && padLp != null) wm.updateViewLayout(pad, padLp);
    }

    // ---------------------------------------------------------------- Touchpad

    private boolean onPadTouch(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastRawX = downRawX = e.getRawX();
                lastRawY = downRawY = e.getRawY();
                moved = false;
                twoFinger = false;
                longFired = false;
                onHandle = pad.isOnHandle(e.getY());
                onClose = pad.isOnClose(e.getX(), e.getY());
                if (!onHandle) handler.postDelayed(longPressRunnable, LONG_PRESS_DETECT_MS);
                return true;

            case MotionEvent.ACTION_POINTER_DOWN:
                if (!onHandle && e.getPointerCount() == 2) {
                    twoFinger = true;
                    handler.removeCallbacks(longPressRunnable);
                    twoStartX = (e.getX(0) + e.getX(1)) / 2f;
                    twoStartY = (e.getY(0) + e.getY(1)) / 2f;
                    scrollDx = 0;
                    scrollDy = 0;
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                if (twoFinger) {
                    if (e.getPointerCount() >= 2) {
                        scrollDx = (e.getX(0) + e.getX(1)) / 2f - twoStartX;
                        scrollDy = (e.getY(0) + e.getY(1)) / 2f - twoStartY;
                    }
                    return true;
                }
                float rx = e.getRawX();
                float ry = e.getRawY();
                if (!moved && Math.hypot(rx - downRawX, ry - downRawY) > touchSlop) {
                    moved = true;
                    handler.removeCallbacks(longPressRunnable);
                }
                if (moved) {
                    float dx = rx - lastRawX;
                    float dy = ry - lastRawY;
                    if (onHandle) movePad(dx, dy); else moveCursor(dx, dy);
                }
                lastRawX = rx;
                lastRawY = ry;
                return true;

            case MotionEvent.ACTION_UP:
                handler.removeCallbacks(longPressRunnable);
                if (twoFinger) {
                    if (Math.hypot(scrollDx, scrollDy) > touchSlop) scrollAtCursor(scrollDx, scrollDy);
                } else if (onHandle) {
                    if (!moved && onClose) hideOverlay();
                    if (moved) savePadPosition();
                } else if (!moved && !longFired) {
                    tapAtCursor(TAP_MS);
                }
                return true;

            case MotionEvent.ACTION_CANCEL:
                handler.removeCallbacks(longPressRunnable);
                return true;

            default:
                return true;
        }
    }

    private void moveCursor(float dx, float dy) {
        float speed = Prefs.speed(this);
        float ref = 20 * density;
        cursorX = CursorMath.clamp(cursorX + CursorMath.accelerate(dx, speed, ref), 0, screenW - 1);
        cursorY = CursorMath.clamp(cursorY + CursorMath.accelerate(dy, speed, ref), 0, screenH - 1);
        updateCursorLayout();
    }

    private void movePad(float dx, float dy) {
        padLp.x = (int) CursorMath.clamp(padLp.x + dx, 0, screenW - pad.widthPx());
        padLp.y = (int) CursorMath.clamp(padLp.y + dy, 0, screenH - pad.heightPx());
        updatePadLayout();
    }

    private void savePadPosition() {
        Prefs.get(this).edit().putInt(Prefs.PAD_X, padLp.x).putInt(Prefs.PAD_Y, padLp.y).apply();
    }

    // ---------------------------------------------------------------- Gesten

    private void tapAtCursor(long durationMs) {
        Path p = new Path();
        p.moveTo(cursorX, cursorY);
        cursor.flash();
        runGesture(p, durationMs, cursorX, cursorY, cursorX, cursorY);
    }

    /** Fingerbewegung nach oben scrollt den Inhalt wie auf dem Bildschirm (Wisch nach oben). */
    private void scrollAtCursor(float dx, float dy) {
        float factor = 2.5f;
        float endX = CursorMath.clamp(cursorX + dx * factor, 1, screenW - 2);
        float endY = CursorMath.clamp(cursorY + dy * factor, 1, screenH - 2);
        Path p = new Path();
        p.moveTo(cursorX, cursorY);
        p.lineTo(endX, endY);
        runGesture(p, SCROLL_MS, cursorX, cursorY, endX, endY);
    }

    private void runGesture(Path path, long durationMs, float x1, float y1, float x2, float y2) {
        if (gestureRunning) return;
        gestureRunning = true;

        // Liegt der Zeiger auf dem eigenen Touchpad, wird es kurz durchlässig gemacht,
        // damit der Klick die App darunter erreicht.
        boolean overPad = shown && (CursorMath.inside(x1, y1, padLp.x, padLp.y, pad.widthPx(), pad.heightPx())
                || CursorMath.inside(x2, y2, padLp.x, padLp.y, pad.widthPx(), pad.heightPx()));
        if (overPad) setPadPassThrough(true);

        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(path, 0, durationMs))
                .build();
        GestureResultCallback done = new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription g) { finish(); }

            @Override
            public void onCancelled(GestureDescription g) { finish(); }

            private void finish() {
                gestureRunning = false;
                if (overPad) setPadPassThrough(false);
            }
        };
        // Kleine Verzögerung, damit der eigene Finger das Touchpad sicher verlassen hat.
        handler.postDelayed(() -> {
            boolean ok = dispatchGesture(gesture, done, handler);
            if (!ok) {
                gestureRunning = false;
                if (overPad) setPadPassThrough(false);
            }
        }, overPad ? 80 : 30);
    }

    private void setPadPassThrough(boolean passThrough) {
        if (!shown) return;
        if (passThrough) {
            padLp.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            pad.setVisibility(android.view.View.INVISIBLE);
        } else {
            padLp.flags &= ~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            pad.setVisibility(android.view.View.VISIBLE);
        }
        updatePadLayout();
    }
}
