package org.vaadin.addons.antlerflow.grid.exporter.xdevsoftware;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.provider.DataProvider;
import org.vaadin.addons.antlerflow.grid.exporter.ExportSize;
import org.vaadin.addons.antlerflow.grid.exporter.GridExportConfig;
import org.vaadin.addons.antlerflow.grid.exporter.GridExportHandler;
import software.xdev.vaadin.grid_exporter.GridExportLocalizationConfig;
import software.xdev.vaadin.grid_exporter.GridExporter;
import software.xdev.vaadin.grid_exporter.GridExporterProvider;
import software.xdev.vaadin.grid_exporter.column.ColumnConfigurationBuilder;
import software.xdev.vaadin.grid_exporter.column.ColumnConfigurationHeaderResolvingStrategyBuilder;
import software.xdev.vaadin.grid_exporter.grid.GridDataExtractor;
import software.xdev.vaadin.grid_exporter.jasper.JasperGridExporterProvider;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class XDEVSoftwareGridExportHandler<T> implements GridExportHandler<T> {

    @Override
    public void handleExport(
            final Grid<T> grid,
            final DataProvider<T, ?> dataProvider,
            final GridExportConfig<T> config) {
        Objects.requireNonNull(grid, "grid must not be null");

        final GridExporter<T> exporter =
                new GridExporter<>(grid)
                        .loadFromProvider(
                                Objects.requireNonNull(
                                        getProvider(), "GridExporterProvider must not be null"))
                        .withColumnConfigurationBuilder(getColumnConfigurationBuilder());

        applyLocalization(exporter);
        applyConfig(exporter, grid, dataProvider, config);

        exporter.open();
    }

    protected GridExporterProvider getProvider() {
        return new JasperGridExporterProvider();
    }

    protected ColumnConfigurationBuilder getColumnConfigurationBuilder() {
        return new ColumnConfigurationBuilder()
                .withColumnConfigHeaderResolvingStrategyBuilder(
                        ColumnConfigurationHeaderResolvingStrategyBuilder
                                ::withVaadinInternalHeaderStrategy);
    }

    /** Override to provide localization. Return {@code null} to keep default behavior. */
    protected GridExportLocalizationConfig getLocalizationConfig() {
        return null;
    }

    private void applyLocalization(final GridExporter<T> exporter) {
        Optional.ofNullable(getLocalizationConfig()).ifPresent(exporter::withLocalizationConfig);
    }

    private void applyConfig(
            final GridExporter<T> exporter,
            final Grid<T> grid,
            final DataProvider<T, ?> dataProvider,
            final GridExportConfig<T> config) {
        if (config == null) {
            return;
        }

        Optional.ofNullable(config.getFileName()).ifPresent(exporter::withFileName);

        Optional.ofNullable(config.getFormats())
                .map(
                        formats ->
                                formats.stream()
                                        .map(XDEVSoftwareFormat::fromString)
                                        .filter(Objects::nonNull)
                                        .map(XDEVSoftwareFormat::getFormat)
                                        .toList())
                .ifPresent(exporter::withAvailableFormats);

        Optional.ofNullable(config.getPreSelectedFormat())
                .map(XDEVSoftwareFormat::fromString)
                .map(XDEVSoftwareFormat::getFormat)
                .ifPresent(exporter::withPreSelectedFormat);

        Optional.ofNullable(config.getColumnFilter()).ifPresent(exporter::withColumnFilter);

        // Avoid handler instance state: capture everything needed in the supplier.
        exporter.withGridDataExtractorSupplier(
                tGrid -> getGridDataExtractor(tGrid, dataProvider, config));
    }

    protected GridDataExtractor<T> getGridDataExtractor(
            final Grid<T> grid,
            final DataProvider<T, ?> dataProvider,
            final GridExportConfig<T> config) {
        if (config != null && config.getSize() == ExportSize.ALL) {
            Objects.requireNonNull(dataProvider, "dataProvider must not be null");
            return new GridDataExtractor<>(grid) {
                @Override
                protected Stream<T> getSortedAndFilteredData(final Grid<T> grid) {
                    final int limit =
                            Optional.ofNullable(config.getLimit()).orElse(Integer.MAX_VALUE);
                    return dataProvider.fetch(grid.getDataCommunicator().buildQuery(0, limit));
                }
            };
        }
        return new GridDataExtractor<>(grid);
    }
}
