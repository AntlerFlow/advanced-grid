package org.vaadin.addons.antlerflow.grid.columns.storage;

import com.vaadin.flow.function.SerializableConsumer;
import org.vaadin.addons.antlerflow.grid.columns.ColumnPreferences;

import java.util.Map;

/**
 * Stores preferences in the browser's Web Storage ({@code localStorage} or {@code
 * sessionStorage}) as {@code {"version":1,"columns":{"<id>":true|false}}}. Use {@link
 * ColumnPreferenceStorage#localStorage()} or {@link ColumnPreferenceStorage#sessionStorage()}.
 *
 * <p>Storage errors in the browser (private mode, quota, corrupted JSON) are handled there: a
 * failed read yields an empty preference and a failed write leaves the choice in effect for the
 * current view only.
 */
public final class BrowserColumnPreferenceStorage implements ColumnPreferenceStorage {

    static final BrowserColumnPreferenceStorage LOCAL =
            new BrowserColumnPreferenceStorage("localStorage");
    static final BrowserColumnPreferenceStorage SESSION =
            new BrowserColumnPreferenceStorage("sessionStorage");

    /** $0 = Web Storage area name, $1 = key. Returns the {@link ColumnPreferences} wire format. */
    private static final String READ_JS =
            """
            try {
              const stored = JSON.parse(window[$0].getItem($1) || 'null');
              if (!stored || typeof stored.columns !== 'object' || stored.columns === null) {
                return '';
              }
              return Object.entries(stored.columns)
                  .map(([id, visible]) => (visible === true ? '+' : '-') + id)
                  .join('\\n');
            } catch (e) {
              return '';
            }
            """;

    /** $0 = area, $1 = key, $2 = wire format; an empty value removes the entry. */
    private static final String WRITE_JS =
            """
            try {
              if (!$2) {
                window[$0].removeItem($1);
                return;
              }
              const columns = {};
              $2.split('\\n').forEach(line => {
                if (line.length > 1) {
                  columns[line.substring(1)] = line.charAt(0) === '+';
                }
              });
              window[$0].setItem($1, JSON.stringify({version: 1, columns}));
            } catch (e) {
              // Storage unavailable: the choice still applies to the current view.
            }
            """;

    private final String area;

    private BrowserColumnPreferenceStorage(String area) {
        this.area = area;
    }

    /** Returns the Web Storage area: {@code localStorage} or {@code sessionStorage}. */
    public String getArea() {
        return area;
    }

    @Override
    public void load(
            ColumnPreferenceContext context, SerializableConsumer<Map<String, Boolean>> onLoaded) {
        context.component()
                .getElement()
                .executeJs(READ_JS, area, context.key())
                .then(String.class, raw -> onLoaded.accept(ColumnPreferences.decode(raw)));
    }

    @Override
    public void save(ColumnPreferenceContext context, Map<String, Boolean> visibility) {
        write(context, ColumnPreferences.encode(visibility));
    }

    @Override
    public void clear(ColumnPreferenceContext context) {
        write(context, "");
    }

    private void write(ColumnPreferenceContext context, String encoded) {
        context.component().getElement().executeJs(WRITE_JS, area, context.key(), encoded);
    }

    /** Keeps the two shared instances unique after deserialization. */
    private Object readResolve() {
        return "sessionStorage".equals(area) ? SESSION : LOCAL;
    }
}
