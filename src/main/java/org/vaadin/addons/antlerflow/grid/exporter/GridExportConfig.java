package org.vaadin.addons.antlerflow.grid.exporter;

import com.vaadin.flow.component.grid.Grid;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.function.Predicate;

@Data
@Builder(toBuilder = true)
public class GridExportConfig<T> {
    private String fileName;
    private List<String> formats;
    private String preSelectedFormat;
    @Builder.Default private ExportSize size = ExportSize.CURRENT_DISPLAY;
    @Builder.Default private Integer limit = Integer.MAX_VALUE;
    @Builder.Default private Predicate<Grid.Column<T>> columnFilter = column -> true;

    /**
     * When {@code true} (default) and rows are selected in the grid, the export button lets the
     * user choose between exporting the rows defined by {@link #size} and only the selected rows.
     * Has no effect when {@link #size} is {@link ExportSize#SELECTED}.
     */
    @Builder.Default private boolean selectedRowsOption = true;
}
