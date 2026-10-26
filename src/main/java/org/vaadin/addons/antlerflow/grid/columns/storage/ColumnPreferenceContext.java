package org.vaadin.addons.antlerflow.grid.columns.storage;

import com.vaadin.flow.component.Component;

import java.util.Objects;

/**
 * Identifies the preference being loaded or stored.
 *
 * @param key the storage key of one grid, e.g. {@code af-advanced-grid.columns./orders}
 * @param component the grid component, attached to a UI; browser storages run their script on it
 */
public record ColumnPreferenceContext(String key, Component component) {

    public ColumnPreferenceContext {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(component, "component must not be null");
    }
}
