package com.relayapi.ratelimiter.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class ScheduledWindowTest {

    @Test
    void testStandardWindowActiveState() {
        ScheduledWindow window = new ScheduledWindow(
                LocalTime.of(2, 0),
                LocalTime.of(4, 0),
                1200,
                20.0
        );

        assertTrue(window.isActiveAt(LocalTime.of(2, 0)), "Start time should be inclusive");
        assertTrue(window.isActiveAt(LocalTime.of(3, 0)), "Middle time should be active");
        assertTrue(window.isActiveAt(LocalTime.of(3, 59, 59)), "Time right before end should be active");

        assertFalse(window.isActiveAt(LocalTime.of(1, 59)), "Time before start should be inactive");
        assertFalse(window.isActiveAt(LocalTime.of(4, 0)), "End time should be exclusive");
        assertFalse(window.isActiveAt(LocalTime.of(5, 0)), "Time after end should be inactive");
    }

    @Test
    void testOvernightWindowActiveState() {
        ScheduledWindow overnightWindow = new ScheduledWindow(
                LocalTime.of(23, 0),
                LocalTime.of(2, 0),
                1000,
                15.0
        );

        assertTrue(overnightWindow.isActiveAt(LocalTime.of(23, 30)), "Late night before midnight should be active");
        assertTrue(overnightWindow.isActiveAt(LocalTime.of(1, 0)), "Early morning after midnight should be active");
        assertFalse(overnightWindow.isActiveAt(LocalTime.of(12, 0)), "Midday should be inactive");
    }
}
