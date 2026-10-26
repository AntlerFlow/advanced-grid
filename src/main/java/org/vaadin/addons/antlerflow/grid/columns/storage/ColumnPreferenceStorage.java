package org.vaadin.addons.antlerflow.grid.columns.storage;

import com.vaadin.flow.function.SerializableConsumer;

import java.io.Serializable;
import java.util.Map;

/**
 * Stores a user's column visibility preference for an {@code AdvancedGrid}.
 *
 * <p>A preference is a map of column id to visibility (see {@code ColumnPreferences#columnId}),
 * kept under a key that identifies one grid (see {@link ColumnPreferenceContext#key()}).
 *
 * <p>Ready-made implementations:
 *
 * <ul>
 *   <li>{@link #localStorage()}: the browser's {@code localStorage}; kept until the user clears
 *       browser data, per browser and device (the default).
 *   <li>{@link #sessionStorage()}: the browser's {@code sessionStorage}; kept until the tab closes.
 *   <li>{@link JdbcColumnPreferenceStorage}: a database table, per signed-in user, following the
 *       user across browsers and devices.
 *   <li>{@link InMemoryColumnPreferenceStorage}: server memory, per user; for prototypes and tests.
 * </ul>
 *
 * <p>To store elsewhere (a REST service, Redis, a JPA repository), extend {@link
 * ServerSideColumnPreferenceStorage} for any synchronous server-side destination, or implement
 * this interface directly for asynchronous ones.
 *
 * <p>Implementations must not throw from these methods for storage failures: a preference is a
 * convenience, and the grid must keep working with its default columns when storage is
 * unavailable. Log the failure and report an empty preference instead.
 */
public interface ColumnPreferenceStorage extends Serializable {

    /**
     * Loads the preference and passes it to {@code onLoaded}, possibly later (for example after a
     * browser round trip). Pass an empty map when nothing is stored. Must be called on the UI
     * thread (with the session locked), as the grid applies the result directly.
     */
    void load(ColumnPreferenceContext context, SerializableConsumer<Map<String, Boolean>> onLoaded);

    /** Stores the preference, replacing any previous one under the same key. */
    void save(ColumnPreferenceContext context, Map<String, Boolean> visibility);

    /** Removes the preference stored under the context's key, if any. */
    void clear(ColumnPreferenceContext context);

    /** Stores preferences in the browser's {@code localStorage}. */
    static ColumnPreferenceStorage localStorage() {
        return BrowserColumnPreferenceStorage.LOCAL;
    }

    /** Stores preferences in the browser's {@code sessionStorage} (cleared when the tab closes). */
    static ColumnPreferenceStorage sessionStorage() {
        return BrowserColumnPreferenceStorage.SESSION;
    }
}
