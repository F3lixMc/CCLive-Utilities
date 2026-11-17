package net.felix.Community.chat;

/**
 * Positionen der Ignore-Ansicht. Die Spielerliste beginnt direkt unter dem Suchfeld.
 * Das Dropdown liegt nur bei Treffern darüber. Beide Listen scrollen, Feld und Schließen nicht.
 */
final class ChatIgnoreLayout {
    static final int FIELD_Y = 36;
    static final int FIELD_HEIGHT = 20;
    static final int DROPDOWN_TOP = FIELD_Y + FIELD_HEIGHT;
    static final int LIST_GAP = 4;
    static final int LIST_TOP = DROPDOWN_TOP + LIST_GAP;
    static final int DROPDOWN_ROW = 12;
    static final int LIST_ROW = 22;
    static final int VISIBLE_SUGGESTIONS = 6;
    static final int NAME_WIDTH = 180;
    static final int BUTTON_GAP = 6;
    static final int BUTTON_WIDTH = 94;
    static final int ROW_WIDTH = NAME_WIDTH + BUTTON_GAP + BUTTON_WIDTH;
    static final int CLOSE_FROM_BOTTOM = 32;

    private int suggestionScroll;
    private int listScroll;

    int fieldY() {
        return FIELD_Y;
    }

    int listTop() {
        return LIST_TOP;
    }

    int closeY(int screenHeight) {
        return screenHeight - CLOSE_FROM_BOTTOM;
    }

    int suggestionScroll() {
        return suggestionScroll;
    }

    int listScroll() {
        return listScroll;
    }

    void resetSuggestionScroll() {
        suggestionScroll = 0;
    }

    int listBottom(int screenHeight) {
        return closeY(screenHeight) - 8;
    }

    int visibleListRows(int screenHeight) {
        return Math.max(0, (listBottom(screenHeight) - LIST_TOP) / LIST_ROW);
    }

    int dropdownVisible(int suggestionCount, int screenHeight) {
        if (suggestionCount <= 0) {
            return 0;
        }
        int fit = Math.max(0, (listBottom(screenHeight) - DROPDOWN_TOP) / DROPDOWN_ROW);
        return Math.min(VISIBLE_SUGGESTIONS, Math.min(fit, suggestionCount));
    }

    int maxSuggestionScroll(int suggestionCount, int screenHeight) {
        return Math.max(0, suggestionCount - dropdownVisible(suggestionCount, screenHeight));
    }

    int maxListScroll(int nameCount, int screenHeight) {
        return Math.max(0, nameCount - visibleListRows(screenHeight));
    }

    void clamp(int suggestionCount, int nameCount, int screenHeight) {
        suggestionScroll = clamp(suggestionScroll, maxSuggestionScroll(suggestionCount, screenHeight));
        listScroll = clamp(listScroll, maxListScroll(nameCount, screenHeight));
    }

    static int fieldX(int screenWidth) {
        return screenWidth / 2 - 140;
    }

    int listRowY(int slot) {
        return LIST_TOP + slot * LIST_ROW;
    }

    int suggestionIndex(double pointerX, double pointerY, int screenWidth, int suggestionCount, int screenHeight) {
        int visible = dropdownVisible(suggestionCount, screenHeight);
        if (visible <= 0 || !inside(pointerX, pointerY, fieldX(screenWidth), DROPDOWN_TOP, ROW_WIDTH, visible * DROPDOWN_ROW)) {
            return -1;
        }
        int slot = (int) ((pointerY - DROPDOWN_TOP) / DROPDOWN_ROW);
        if (slot < 0 || slot >= visible) {
            return -1;
        }
        int index = suggestionScroll + slot;
        return index < suggestionCount ? index : -1;
    }

    int listIndex(double pointerX, double pointerY, int screenWidth, int nameCount, int screenHeight) {
        int visible = Math.min(visibleListRows(screenHeight), Math.max(0, nameCount - listScroll));
        int x = fieldX(screenWidth) + NAME_WIDTH + BUTTON_GAP;
        if (visible <= 0 || !inside(pointerX, pointerY, x, LIST_TOP, BUTTON_WIDTH, visible * LIST_ROW)) {
            return -1;
        }
        int slot = (int) ((pointerY - LIST_TOP) / LIST_ROW);
        if (slot < 0 || slot >= visible) {
            return -1;
        }
        int index = listScroll + slot;
        return index < nameCount ? index : -1;
    }

    boolean listSlotCovered(int slot, int suggestionCount, int screenHeight) {
        int visible = dropdownVisible(suggestionCount, screenHeight);
        if (visible <= 0) {
            return false;
        }
        int dropBottom = DROPDOWN_TOP + visible * DROPDOWN_ROW;
        int rowTop = listRowY(slot);
        return rowTop < dropBottom && rowTop + LIST_ROW > DROPDOWN_TOP;
    }

    boolean scroll(double pointerX, double pointerY, double vertical, int screenWidth, int screenHeight,
                   int suggestionCount, int nameCount) {
        if (vertical == 0) {
            return false;
        }
        int delta = vertical > 0 ? -1 : 1;
        if (overDropdown(pointerX, pointerY, screenWidth, suggestionCount, screenHeight)) {
            if (maxSuggestionScroll(suggestionCount, screenHeight) > 0) {
                suggestionScroll = clamp(suggestionScroll + delta, maxSuggestionScroll(suggestionCount, screenHeight));
            }
            return true;
        }
        if (overList(pointerX, pointerY, screenWidth, screenHeight, nameCount)
            && maxListScroll(nameCount, screenHeight) > 0) {
            listScroll = clamp(listScroll + delta, maxListScroll(nameCount, screenHeight));
            return true;
        }
        return false;
    }

    private boolean overDropdown(double pointerX, double pointerY, int screenWidth, int suggestionCount, int screenHeight) {
        int visible = dropdownVisible(suggestionCount, screenHeight);
        return visible > 0 && inside(pointerX, pointerY, fieldX(screenWidth), DROPDOWN_TOP, ROW_WIDTH, visible * DROPDOWN_ROW);
    }

    private boolean overList(double pointerX, double pointerY, int screenWidth, int screenHeight, int nameCount) {
        int visible = Math.min(visibleListRows(screenHeight), nameCount);
        return visible > 0 && inside(pointerX, pointerY, fieldX(screenWidth), LIST_TOP, ROW_WIDTH, visible * LIST_ROW);
    }

    private static boolean inside(double pointerX, double pointerY, int x, int y, int width, int height) {
        return pointerX >= x && pointerX < x + width && pointerY >= y && pointerY < y + height;
    }

    private static int clamp(int value, int max) {
        return Math.max(0, Math.min(max, value));
    }
}
