package org.vaadin.addons.antlerflow.grid.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;
import org.vaadin.addons.antlerflow.grid.AdvancedGrid;
import org.vaadin.addons.antlerflow.grid.dataprovider.SpringPageableDataProvider;
import org.vaadin.addons.antlerflow.grid.filter.PersonFilter;
import org.vaadin.addons.antlerflow.grid.layout.MainLayout;
import org.vaadin.addons.antlerflow.grid.model.Person;
import org.vaadin.addons.antlerflow.grid.service.PersonService;

@Route(value = "with-spring-data", layout = MainLayout.class)
@Menu(order = 2, title = "With Spring Data")
public class WithSpringDataView extends VerticalLayout {

    public WithSpringDataView(PersonService personService) {
        setSizeFull();
        AdvancedGrid<Person> advancedGrid = new AdvancedGrid<>();
        advancedGrid
                .getInnerGrid()
                .addColumn(Person::getId)
                .setHeader("ID")
                .setSortable(true)
                .setResizable(true)
                .setSortProperty("id");
        advancedGrid
                .getInnerGrid()
                .addColumn(Person::getFirstName)
                .setHeader("First Name")
                .setSortable(true)
                .setResizable(true)
                .setSortProperty("firstName");
        advancedGrid
                .getInnerGrid()
                .addColumn(Person::getLastName)
                .setHeader("Last Name")
                .setSortable(true)
                .setResizable(true)
                .setSortProperty("lastName");
        advancedGrid
                .getInnerGrid()
                .addColumn(Person::getAge)
                .setHeader("Age")
                .setSortable(true)
                .setResizable(true)
                .setSortProperty("age");

        PersonFilter personFilter = new PersonFilter();
        SpringPageableDataProvider<Person, PersonFilter> dataProvider =
                new SpringPageableDataProvider<>(personService::search);
        dataProvider.setFilter(personFilter);
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
        advancedGrid.enableExport(true);
        add(advancedGrid);
    }
}
