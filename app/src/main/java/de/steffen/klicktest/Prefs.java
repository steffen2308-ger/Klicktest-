package de.steffen.klicktest;

import android.content.Context;
import android.content.SharedPreferences;

/** Einstellungen der App. Werte in Prozent bzw. Zehnteln. */
final class Prefs {
    static final String CURSOR_ALPHA = "cursor_alpha"; // 5..100 %
    static final String PAD_ALPHA = "pad_alpha";       // 5..100 %
    static final String SPEED = "speed";               // 5..40 -> 0.5..4.0
    static final String PAD_X = "pad_x";
    static final String PAD_Y = "pad_y";

    static final int MIN_ALPHA = 5;

    private Prefs() {}

    static SharedPreferences get(Context c) {
        return c.getSharedPreferences("klicktest", Context.MODE_PRIVATE);
    }

    static int cursorAlpha(Context c) { return get(c).getInt(CURSOR_ALPHA, 90); }
    static int padAlpha(Context c) { return get(c).getInt(PAD_ALPHA, 45); }
    static int speedTenths(Context c) { return get(c).getInt(SPEED, 18); }
    static float speed(Context c) { return speedTenths(c) / 10f; }

    static void putInt(Context c, String key, int value) {
        get(c).edit().putInt(key, value).apply();
    }
}
