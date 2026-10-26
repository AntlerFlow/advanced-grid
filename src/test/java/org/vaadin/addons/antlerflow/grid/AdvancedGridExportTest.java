package org.vaadin.addons.antlerflow.grid;

import static org.assertj.core.api.Assertions.assertThat;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.provider.DataProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.vaadin.addons.antlerflow.grid.exporter.ExportSize;
import org.vaadin.addons.antlerflow.grid.exporter.GridExportConfig;
import org.vaadin.addons.antlerflow.grid.exporter.GridExportHandler;
import org.vaadin.addons.antlerflow.grid.model.Person;

import java.util.ArrayList;
import java.util.List;

class AdvancedGridExportTest {

    private final List<GridExportConfig<Person>> exports = new ArrayList<>();
    private final GridExportHandler<Person> recordingHandler =
            (Grid<Person> grid, DataProvider<Person, ?> provider, GridExportConfig<Person> c) ->
                    exports.add(c);

    private AdvancedGrid<Person> advancedGrid;
    private Person ann;
    private Person bob;

    @BeforeEach
    void setUp() {
        ann = person(1L, "Ann");
        bob = person(2L, "Bob");
        advancedGrid = new AdvancedGrid<>();
        advancedGrid.getInnerGrid().addColumn(Person::getFirstName).setHeader("First name");
        advancedGrid.getInnerGrid().setSelectionMode(Grid.SelectionMode.MULTI);
        advancedGrid.setExportHandler(recordingHandler);
        advancedGrid.enableExport(true);
    }

    @Test
    void exportButtonIsDisabledWithoutData() {
        assertThat(advancedGrid.getExportButton().isEnabled()).isFalse();

        advancedGrid.setItems(List.of());
        assertThat(advancedGrid.getExportButton().isEnabled()).isFalse();
    }

    @Test
    void exportButtonFollowsDataAvailability() {
        advancedGrid.setItems(List.of(ann, bob));
        assertThat(advancedGrid.getExportButton().isEnabled()).isTrue();

        advancedGrid.setItems(List.of());
        assertThat(advancedGrid.getExportButton().isEnabled()).isFalse();
    }

    @Test
    void exportButtonFollowsDataAvailabilityWhenPaginated() {
        advancedGrid.setPaginationVisibility(true);
        assertThat(advancedGrid.getExportButton().isEnabled()).isFalse();

        advancedGrid.setItems(List.of(ann));
        assertThat(advancedGrid.getExportButton().isEnabled()).isTrue();
    }

    @Test
    void exportButtonFollowsFilterChanges() {
        var provider = DataProvider.ofCollection(List.of(ann, bob));
        advancedGrid.setDataProvider(provider);
        assertThat(advancedGrid.getExportButton().isEnabled()).isTrue();

        provider.setFilter(p -> false);
        assertThat(advancedGrid.getExportButton().isEnabled()).isFalse();

        provider.clearFilters();
        assertThat(advancedGrid.getExportButton().isEnabled()).isTrue();
    }

    @Test
    void selectedSizeNeedsASelection() {
        advancedGrid.setItems(List.of(ann, bob));
        advancedGrid.setExportConfig(
                GridExportConfig.<Person>builder().size(ExportSize.SELECTED).build());
        assertThat(advancedGrid.getExportButton().isEnabled()).isFalse();

        advancedGrid.getInnerGrid().select(bob);
        assertThat(advancedGrid.getExportButton().isEnabled()).isTrue();

        advancedGrid.getExportButton().click();
        assertThat(exports).singleElement()
                .extracting(GridExportConfig::getSize).isEqualTo(ExportSize.SELECTED);
    }

    @Test
    void withoutSelectionTheButtonExportsDirectly() {
        advancedGrid.setItems(List.of(ann, bob));

        advancedGrid.getExportButton().click();

        assertThat(exports).hasSize(1);
        assertThat(exports.getFirst()).isNull(); // the (unset) configured export
    }

    @Test
    void withSelectionTheUserChoosesSelectedRows() {
        advancedGrid.setItems(List.of(ann, bob));
        advancedGrid.setExportConfig(
                GridExportConfig.<Person>builder().size(ExportSize.ALL).fileName("people").build());
        advancedGrid.getInnerGrid().select(ann);

        advancedGrid.getExportButton().click();

        assertThat(exports).isEmpty(); // options shown instead
        assertThat(advancedGrid.getExportSelectedButton().getText())
                .isEqualTo("Export selected rows (1)");
        assertThat(advancedGrid.getExportScopeButton().getText()).isEqualTo("Export all rows");

        advancedGrid.getExportSelectedButton().click();

        assertThat(exports).singleElement()
                .satisfies(
                        c -> {
                            assertThat(c.getSize()).isEqualTo(ExportSize.SELECTED);
                            assertThat(c.getFileName()).isEqualTo("people");
                        });
    }

    @Test
    void withSelectionTheUserCanStillExportTheConfiguredRows() {
        advancedGrid.setItems(List.of(ann, bob));
        GridExportConfig<Person> config =
                GridExportConfig.<Person>builder().size(ExportSize.ALL).build();
        advancedGrid.setExportConfig(config);
        advancedGrid.getInnerGrid().select(ann);

        advancedGrid.getExportButton().click();
        advancedGrid.getExportScopeButton().click();

        assertThat(exports).containsExactly(config);
    }

    @Test
    void selectedRowsOptionCanBeTurnedOff() {
        advancedGrid.setItems(List.of(ann, bob));
        GridExportConfig<Person> config =
                GridExportConfig.<Person>builder().selectedRowsOption(false).build();
        advancedGrid.setExportConfig(config);
        advancedGrid.getInnerGrid().select(ann);

        advancedGrid.getExportButton().click();

        assertThat(exports).containsExactly(config);
    }

    @Test
    void toolbarButtonsUseTheTertiaryVariant() {
        advancedGrid.enableColumnSelection(true);
        Button action = new Button("New item");
        Span notAButton = new Span("label");
        advancedGrid.addToActions(action, notAButton);

        assertThat(advancedGrid.getExportButton().getThemeNames()).contains("tertiary");
        assertThat(advancedGrid.getColumnSelectionButton().getThemeNames()).contains("tertiary");
        assertThat(action.getThemeNames()).contains("tertiary");
        assertThat(notAButton.getElement().getAttribute("theme")).isNull();
        assertThat(advancedGrid.getColumnSelectionButton().getIcon().getElement()
                        .getAttribute("icon"))
                .isEqualTo("vaadin:filter");
    }

    private static Person person(long id, String firstName) {
        return Person.builder().id(id).firstName(firstName).build();
    }
}
