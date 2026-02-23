package org.vaadin.addons.antlerflow.grid.exporter;

import com.vaadin.flow.component.grid.Grid;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.function.Predicate;

@Data
@Builder
public class GridExportConfig<T> {
    private String fileName;
    private List<String> formats;
    private String preSelectedFormat;
    @Builder.Default private ExportSize size = ExportSize.CURRENT_DISPLAY;
    @Builder.Default private Integer limit = Integer.MAX_VALUE;
    @Builder.Default private Predicate<Grid.Column<T>> columnFilter = column -> true;
}
