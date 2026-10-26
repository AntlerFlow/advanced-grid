package org.vaadin.addons.antlerflow.grid.columns;

import com.vaadin.flow.component.grid.Grid;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Identifies grid columns and converts a user's column visibility preference to and from the
 * line-based wire format exchanged with the browser.
 *
 * <p>Each line holds one column: {@code +id} for a visible column, {@code -id} for a hidden one.
 * The client element stores the preference in {@code localStorage} as JSON; this format is only
 * used between server and client.
 */
public final class ColumnPreferences {

    static final char VISIBLE = '+';
    static final char HIDDEN = '-';
    private static final String LINE_SEPARATOR = "\n";

    private ColumnPreferences() {}

    /**
     * Returns a stable identifier for the column: its key, otherwise its header text, otherwise
     * its position. Set a column key ({@link Grid.Column#setKey(String)}) for identifiers that
     * survive header renames and column reordering.
     */
    public static <T> String columnId(Grid.Column<T> column, int index) {
        String key = column.getKey();
        if (key != null && !key.isBlank()) {
            return sanitize(key);
        }
        String header = column.getHeaderText();
        if (header != null && !header.isBlank()) {
            return sanitize(header);
        }
        return "#" + index;
    }

    /** Returns the label shown for the column in the column selector. */
    public static <T> String label(Grid.Column<T> column, int index) {
        String header = column.getHeaderText();
        if (header != null && !header.isBlank()) {
            return header.strip();
        }
        String key = column.getKey();
        if (key != null && !key.isBlank()) {
            return key.strip();
        }
        return "Column " + (index + 1);
    }

    /** Encodes column id to visibility entries in the wire format. */
    public static String encode(Map<String, Boolean> visibility) {
        return visibility.entrySet().stream()
                .map(e -> (Boolean.TRUE.equals(e.getValue()) ? VISIBLE : HIDDEN) + e.getKey())
                .collect(Collectors.joining(LINE_SEPARATOR));
    }

    /**
     * Decodes the wire format into column id to visibility entries. Malformed lines are ignored, so
     * a corrupted or outdated preference never breaks the grid.
     */
    public static Map<String, Boolean> decode(String raw) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptyMap();
        }
        Map<String, Boolean> visibility = new LinkedHashMap<>();
        for (String line : raw.split(LINE_SEPARATOR)) {
            if (line.length() < 2) {
                continue;
            }
            char flag = line.charAt(0);
            if (flag == VISIBLE || flag == HIDDEN) {
                visibility.put(line.substring(1), flag == VISIBLE);
            }
        }
        return visibility;
    }

    private static String sanitize(String value) {
        return value.strip().replaceAll("[\\r\\n]+", " ");
    }
}
