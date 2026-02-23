package org.vaadin.addons.antlerflow.grid.exporter.xdevsoftware;

import software.xdev.vaadin.grid_exporter.format.Format;
import software.xdev.vaadin.grid_exporter.jasper.format.*;

public enum XDEVSoftwareFormat {
    CSV {
        @Override
        public Format getFormat() {
            return new CsvFormat();
        }
    },
    DOCX {
        @Override
        public Format getFormat() {
            return new DocxFormat();
        }
    },
    HTML {
        @Override
        public Format getFormat() {
            return new HtmlFormat();
        }
    },
    ODS {
        @Override
        public Format getFormat() {
            return new OdsFormat();
        }
    },
    ODT {
        @Override
        public Format getFormat() {
            return new OdtFormat();
        }
    },
    PDF {
        @Override
        public Format getFormat() {
            return new PdfFormat();
        }
    },
    PPTX {
        @Override
        public Format getFormat() {
            return new PptxFormat();
        }
    },
    RTF {
        @Override
        public Format getFormat() {
            return new RtfFormat();
        }
    },
    TEXT {
        @Override
        public Format getFormat() {
            return new TextFormat();
        }
    },
    XLSX {
        @Override
        public Format getFormat() {
            return new XlsxFormat();
        }
    };

    public abstract Format getFormat();

    public static XDEVSoftwareFormat fromString(String format) {
        for (XDEVSoftwareFormat value : values()) {
            if (value.name().equalsIgnoreCase(format)) {
                return value;
            }
        }
        return null;
    }
}
