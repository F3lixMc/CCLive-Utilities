package net.felix.chat;

/**
 * Aktuelle AbstractContainerScreen-Geometrie aus dem HandledScreenMixin
 * (zuverlässiger als Reflection auf leftPos).
 */
public final class ContainerScreenBounds {
    private static int left;
    private static int top;
    private static int width = 176;
    private static int height = 166;
    private static boolean valid;

    private ContainerScreenBounds() {
    }

    public static void update(int leftPos, int topPos, int imageWidth, int imageHeight) {
        left = leftPos;
        top = topPos;
        width = imageWidth > 0 ? imageWidth : 176;
        height = imageHeight > 0 ? imageHeight : 166;
        valid = true;
    }

    public static boolean isValid() {
        return valid;
    }

    public static int left() {
        return left;
    }

    public static int top() {
        return top;
    }

    public static int width() {
        return width;
    }

    public static int height() {
        return height;
    }

    public static int[] asArray() {
        return new int[]{left, top, width, height};
    }
}
