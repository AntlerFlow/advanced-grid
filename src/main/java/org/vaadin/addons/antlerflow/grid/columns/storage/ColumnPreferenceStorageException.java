package org.vaadin.addons.antlerflow.grid.columns.storage;

/** Thrown by server-side storages when the destination cannot be read or written. */
public class ColumnPreferenceStorageException extends RuntimeException {

    public ColumnPreferenceStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
