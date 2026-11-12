package dev.pncwm.client.jei;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WindowTextBoxTest {
    @Test
    void categoryBoxShowsBothWindowLines() {
        WindowTextBox box = WindowTextBox.category();
        assertTrue(box.x >= 0 && box.y >= 0);
        assertTrue(box.x + box.width <= WindowTextBox.BACKGROUND_WIDTH);
        assertTrue(box.y + box.height <= WindowTextBox.BACKGROUND_HEIGHT);
        assertTrue(box.width > 4, "a 4px box truncates the pressure and temperature lines");
        assertTrue(box.fits("70-80 bar", 2));
        assertTrue(box.fits("25-40 C", 2));
        assertTrue(box.fits("12.5-20.5 bar", 2));
        assertTrue(box.fits("Minimum size 12", 6));
        assertTrue(box.fits("Lubricant 8 mB/s", 6));
        assertTrue(box.fits("Needs 2 coils", 6));
    }
}
