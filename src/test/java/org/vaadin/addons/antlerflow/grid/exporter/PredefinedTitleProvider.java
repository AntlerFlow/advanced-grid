package org.vaadin.addons.antlerflow.grid.exporter;

import java.util.List;

import software.xdev.dynamicreports.jasper.builder.JasperReportBuilder;
import software.xdev.dynamicreports.jasper.builder.export.Exporters;
import software.xdev.dynamicreports.jasper.builder.export.JasperPdfExporterBuilder;
import software.xdev.vaadin.grid_exporter.GridExporterProvider;
import software.xdev.vaadin.grid_exporter.Translator;
import software.xdev.vaadin.grid_exporter.jasper.config.JasperConfigsLocalization;
import software.xdev.vaadin.grid_exporter.jasper.config.header.HeaderConfigComponent;
import software.xdev.vaadin.grid_exporter.jasper.config.highlight.HighlightConfigComponent;
import software.xdev.vaadin.grid_exporter.jasper.config.page.PageConfigComponent;
import software.xdev.vaadin.grid_exporter.jasper.config.title.TitleConfig;
import software.xdev.vaadin.grid_exporter.jasper.config.title.TitleConfigComponent;
import software.xdev.vaadin.grid_exporter.jasper.format.AbstractJasperReportFormat;

public class PredefinedTitleProvider extends GridExporterProvider {
    public PredefinedTitleProvider(final String predefinedTitle) {
        super(
                JasperConfigsLocalization.DEFAULT_VALUES,
                List.of(new PredefinedTitlePdfFormat222(predefinedTitle)));
    }

    public static class PredefinedTitlePdfFormat222
            extends AbstractJasperReportFormat<JasperPdfExporterBuilder> {
        public PredefinedTitlePdfFormat222(final String defaultTitle) {
            super(
                    "PDF",
                    "pdf",
                    "application/pdf",
                    true,
                    true,
                    JasperReportBuilder::toPdf,
                    Exporters::pdfExporter);
            this.withConfigComponents(
                    translator -> new PreDefinedTitleConfigComponent(translator, defaultTitle),
                    HeaderConfigComponent::new,
                    HighlightConfigComponent::new,
                    PageConfigComponent::new);
        }
    }

    public static class PreDefinedTitleConfigComponent extends TitleConfigComponent {
        public PreDefinedTitleConfigComponent(
                final Translator translator, final String defaultTitle) {
            super(translator);

            this.setNewConfigSupplier(
                    () -> {
                        final TitleConfig cfg = new TitleConfig();
                        cfg.setTitle(defaultTitle);
                        return cfg;
                    });
        }
    }
}
