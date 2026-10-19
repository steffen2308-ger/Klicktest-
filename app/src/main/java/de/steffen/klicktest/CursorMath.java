package de.steffen.klicktest;

/** Reine Rechenhilfen ohne Android-Abhängigkeiten (dadurch einfach testbar). */
final class CursorMath {
    private CursorMath() {}

    static float clamp(float value, float min, float max) {
        if (max < min) return min;
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Wandelt eine Fingerbewegung auf dem Touchpad in eine Zeigerbewegung um.
     * Langsame Bewegungen bleiben fein, schnelle Bewegungen werden beschleunigt.
     *
     * @param delta    Fingerbewegung in Pixeln seit dem letzten Ereignis
     * @param speed    Grundgeschwindigkeit (1.0 = 1:1)
     * @param accelRef Bewegung in Pixeln, ab der die Beschleunigung deutlich greift (> 0)
     */
    static float accelerate(float delta, float speed, float accelRef) {
        float boost = 1f + Math.min(Math.abs(delta) / accelRef, 2f);
        return delta * speed * boost;
    }

    static boolean inside(float x, float y, float left, float top, float width, float height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }
}
