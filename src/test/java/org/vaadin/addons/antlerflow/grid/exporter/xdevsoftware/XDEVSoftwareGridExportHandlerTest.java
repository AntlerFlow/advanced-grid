package org.vaadin.addons.antlerflow.grid.exporter.xdevsoftware;

import static org.assertj.core.api.Assertions.assertThat;

import com.vaadin.flow.component.grid.Grid;
import org.junit.jupiter.api.Test;
import org.vaadin.addons.antlerflow.grid.exporter.ExportSize;
import org.vaadin.addons.antlerflow.grid.exporter.GridExportConfig;
import org.vaadin.addons.antlerflow.grid.model.Person;

import java.util.Comparator;
import java.util.List;

class XDEVSoftwareGridExportHandlerTest {

    @Test
    void selectedRowsFollowGridSortOrderAndLimit() {
        Person ann = person("Ann");
        Person bob = person("Bob");
        Person cid = person("Cid");
        Grid<Person> grid = new Grid<>();
        grid.addColumn(Person::getFirstName);
        grid.setSelectionMode(Grid.SelectionMode.MULTI);
        grid.setItems(List.of(ann, bob, cid));
        grid.asMultiSelect().select(cid, ann, bob);
        grid.getDataCommunicator()
                .setInMemorySorting(Comparator.comparing(Person::getFirstName).reversed()::compare);

        GridExportConfig<Person> config =
                GridExportConfig.<Person>builder().size(ExportSize.SELECTED).limit(2).build();

        assertThat(XDEVSoftwareGridExportHandler.selectedItemsInGridOrder(grid, config))
                .containsExactly(cid, bob);
    }

    @Test
    void selectedRowsKeepSelectionOrderWithoutInMemorySorting() {
        Person ann = person("Ann");
        Person bob = person("Bob");
        Grid<Person> grid = new Grid<>();
        grid.setSelectionMode(Grid.SelectionMode.MULTI);
        grid.setItems(List.of(ann, bob));
        grid.asMultiSelect().select(bob, ann);

        GridExportConfig<Person> config =
                GridExportConfig.<Person>builder().size(ExportSize.SELECTED).build();

        assertThat(XDEVSoftwareGridExportHandler.selectedItemsInGridOrder(grid, config))
                .containsExactlyInAnyOrder(ann, bob);
    }

    private static Person person(String firstName) {
        return Person.builder().firstName(firstName).build();
    }
}
