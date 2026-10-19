package de.steffen.klicktest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CursorMathTest {

    @Test
    public void clampKeepsValueInsideScreen() {
        assertEquals(0f, CursorMath.clamp(-5f, 0f, 100f), 0f);
        assertEquals(100f, CursorMath.clamp(140f, 0f, 100f), 0f);
        assertEquals(42f, CursorMath.clamp(42f, 0f, 100f), 0f);
    }

    @Test
    public void clampWithInvertedRangeReturnsMin() {
        assertEquals(0f, CursorMath.clamp(50f, 0f, -10f), 0f);
    }

    @Test
    public void slowMovementStaysFine() {
        // 1 px bei Tempo 1 und Referenz 20 px bleibt fast 1:1.
        assertEquals(1.05f, CursorMath.accelerate(1f, 1f, 20f), 0.001f);
    }

    @Test
    public void fastMovementIsAcceleratedButCapped() {
        assertEquals(40f, CursorMath.accelerate(20f, 1f, 20f), 0.001f);
        // Beschleunigung höchstens dreifach.
        assertEquals(300f, CursorMath.accelerate(100f, 1f, 20f), 0.001f);
        assertEquals(-300f, CursorMath.accelerate(-100f, 1f, 20f), 0.001f);
    }

    @Test
    public void speedScalesLinearly() {
        assertEquals(2f * CursorMath.accelerate(5f, 1f, 20f), CursorMath.accelerate(5f, 2f, 20f), 0.001f);
    }

    @Test
    public void insideChecksHalfOpenRectangle() {
        assertTrue(CursorMath.inside(10, 10, 10, 10, 5, 5));
        assertTrue(CursorMath.inside(14.9f, 14.9f, 10, 10, 5, 5));
        assertFalse(CursorMath.inside(15, 12, 10, 10, 5, 5));
        assertFalse(CursorMath.inside(9.9f, 12, 10, 10, 5, 5));
    }
}
