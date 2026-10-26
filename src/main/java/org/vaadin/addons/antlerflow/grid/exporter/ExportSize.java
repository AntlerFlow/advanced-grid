package org.vaadin.addons.antlerflow.grid.exporter;

public enum ExportSize {
    /** The rows the grid currently displays (the current page when paginated). */
    CURRENT_DISPLAY,
    /** Every row of the data provider, up to {@link GridExportConfig#getLimit()}. */
    ALL,
    /** Only the rows selected in the grid, in the grid's current sort order. */
    SELECTED
}
