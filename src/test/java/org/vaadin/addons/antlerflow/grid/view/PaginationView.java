package org.vaadin.addons.antlerflow.grid.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;
import org.vaadin.addons.antlerflow.grid.AdvancedGrid;
import org.vaadin.addons.antlerflow.grid.exporter.ExportSize;
import org.vaadin.addons.antlerflow.grid.exporter.GridExportConfig;
import org.vaadin.addons.antlerflow.grid.exporter.PredefinedTitleProvider;
import org.vaadin.addons.antlerflow.grid.exporter.xdevsoftware.XDEVSoftwareGridExportHandler;
import org.vaadin.addons.antlerflow.grid.filter.PersonFilter;
import org.vaadin.addons.antlerflow.grid.layout.MainLayout;
import org.vaadin.addons.antlerflow.grid.model.Person;
import org.vaadin.addons.antlerflow.grid.service.PersonService;
import software.xdev.vaadin.grid_exporter.GridExporterProvider;

import java.util.List;

@PreserveOnRefresh
@Route(value = "pagination", layout = MainLayout.class)
@Menu(order = 1, title = "Pagination")
public class PaginationView extends VerticalLayout {

    public PaginationView(PersonService personService) {
        setSizeFull();
        AdvancedGrid<Person> advancedGrid = new AdvancedGrid<>();
        advancedGrid
                .getInnerGrid()
                .addColumn(Person::getId)
                .setHeader("ID")
                .setSortable(true)
                .setResizable(true);
        advancedGrid
                .getInnerGrid()
                .addColumn(Person::getFirstName)
                .setHeader("First Name")
                .setSortable(true)
                .setResizable(true);
        advancedGrid
                .getInnerGrid()
                .addColumn(Person::getLastName)
                .setHeader("Last Name")
                .setSortable(true)
                .setResizable(true);
        advancedGrid
                .getInnerGrid()
                .addColumn(Person::getAge)
                .setHeader("Age")
                .setSortable(true)
                .setResizable(true);
        advancedGrid.getInnerGrid().setAllRowsVisible(true);

        PersonFilter personFilter = new PersonFilter();
        ListDataProvider<Person> dataProvider =
                new ListDataProvider<>(personService.generatePeople(500));
        dataProvider.setFilter(personFilter.getPredicate());

        advancedGrid.setDataProvider(dataProvider);

        TextField nameFilterField = new TextField("Filter by Name");
        nameFilterField.addValueChangeListener(
                e -> {
                    personFilter.setName(e.getValue());
                    dataProvider.refreshAll();
                });
        NumberField ageGreaterEqualFilterField = new NumberField("Filter by Age greater equal");
        ageGreaterEqualFilterField.addValueChangeListener(
                e -> {
                    personFilter.setAgeGreaterEqual(
                            e.getValue() == null ? null : e.getValue().intValue());
                    dataProvider.refreshAll();
                });
        NumberField agelessEqualFilterField = new NumberField("Filter by Age less equal");
        agelessEqualFilterField.addValueChangeListener(
                e -> {
                    personFilter.setAgeLessEqual(
                            e.getValue() == null ? null : e.getValue().intValue());
                    dataProvider.refreshAll();
                });
        advancedGrid.addToFilters(
                nameFilterField, agelessEqualFilterField, ageGreaterEqualFilterField);
        advancedGrid.addToActions(
                new Button("New Item", VaadinIcon.PLUS.create()),
                new Button("Refresh", VaadinIcon.REFRESH.create()));

        advancedGrid.setPaginationVisibility(true);
        GridExportConfig<Person> exportConfig =
                GridExportConfig.<Person>builder()
                        .size(ExportSize.ALL)
                        .formats(List.of("pdf", "xlsx", "csv"))
                        .build();
        advancedGrid.setExportConfig(exportConfig);
        advancedGrid.setExportHandler(
                new XDEVSoftwareGridExportHandler<>() {
                    @Override
                    protected GridExporterProvider getProvider() {
                        return new PredefinedTitleProvider("Custom Title!");
                    }
                });
        advancedGrid.enableExport(true);

        advancedGrid.getPaginator().setHideEdges(true);
        advancedGrid.getPaginator().showPageJump(true);
        advancedGrid.getPaginator().showPageSize(true);

        add(advancedGrid);
    }
}
