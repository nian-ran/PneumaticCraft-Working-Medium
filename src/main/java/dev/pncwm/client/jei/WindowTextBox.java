package dev.pncwm.client.jei;

/**
 * The rectangle the JEI window category passes to {@code addText(text, x, y, width, height)}.
 * The two-int overload is max width and max height at the origin, so the category must not use it.
 */
public final class WindowTextBox {
    public static final int BACKGROUND_WIDTH = 150;
    public static final int BACKGROUND_HEIGHT = 96;

    public final int x;
    public final int y;
    public final int width;
    public final int height;

    private WindowTextBox(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public static WindowTextBox category() {
        int x = 4;
        int y = 28;
        int width = BACKGROUND_WIDTH - 8;
        int height = BACKGROUND_HEIGHT - y - 4;
        return new WindowTextBox(x, y, width, height);
    }

    /** Default font is 6px wide for these ASCII labels and 9px tall per line. */
    public boolean fits(String line, int lineCount) {
        return line.length() * 6 <= width && lineCount * 9 <= height;
    }
}
