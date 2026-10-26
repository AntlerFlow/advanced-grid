package org.vaadin.addons.antlerflow.grid.columns.storage;

import com.vaadin.flow.function.SerializableSupplier;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps preferences per user in server memory. They survive page reloads and new tabs but not a
 * server restart, and are not shared between cluster nodes: use it for prototypes and tests.
 * Share one instance (for example a singleton bean) across all grids and sessions.
 */
public class InMemoryColumnPreferenceStorage extends ServerSideColumnPreferenceStorage {

    private final ConcurrentHashMap<String, Map<String, Boolean>> preferences =
            new ConcurrentHashMap<>();

    public InMemoryColumnPreferenceStorage(SerializableSupplier<String> userIdSupplier) {
        super(userIdSupplier);
    }

    @Override
    protected Optional<Map<String, Boolean>> read(String userId, String key) {
        return Optional.ofNullable(preferences.get(entryKey(userId, key)))
                .map(LinkedHashMap::new);
    }

    @Override
    protected void write(String userId, String key, Map<String, Boolean> visibility) {
        preferences.put(entryKey(userId, key), new LinkedHashMap<>(visibility));
    }

    @Override
    protected void delete(String userId, String key) {
        preferences.remove(entryKey(userId, key));
    }

    private static String entryKey(String userId, String key) {
        return userId + '\u0000' + key;
    }
}
