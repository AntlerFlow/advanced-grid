package org.vaadin.addons.antlerflow.grid.exporter;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.provider.DataProvider;

public interface GridExportHandler<T> {
    void handleExport(Grid<T> grid, DataProvider<T, ?> dataProvider, GridExportConfig<T> config);
}
