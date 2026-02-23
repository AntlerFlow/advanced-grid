package org.vaadin.addons.antlerflow.grid.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ConfigurableFilterDataProvider;
import com.vaadin.flow.function.SerializablePredicate;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;
import org.vaadin.addons.antlerflow.grid.AdvancedGrid;
import org.vaadin.addons.antlerflow.grid.filter.PersonFilter;
import org.vaadin.addons.antlerflow.grid.layout.MainLayout;
import org.vaadin.addons.antlerflow.grid.model.Person;
import org.vaadin.addons.antlerflow.grid.service.PersonService;

@Route(value = "basic-usage", layout = MainLayout.class)
@Menu(order = 0, title = "Basic Usage")
public class BasicUsageView extends VerticalLayout {

    public BasicUsageView(PersonService personService) {
        setSizeFull();
        Grid<Person> grid = new Grid<>(Person.class);
        grid.setItems(personService.generatePeople(500));

        PersonFilter personFilter = new PersonFilter();
        ConfigurableFilterDataProvider<Person, Void, SerializablePredicate<Person>> dataProvider =
                (ConfigurableFilterDataProvider<Person, Void, SerializablePredicate<Person>>)
                        grid.getDataProvider().withConfigurableFilter();
        dataProvider.setFilter(personFilter.getPredicate());
        grid.setDataProvider(dataProvider);

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

        AdvancedGrid<Person> advancedGrid = new AdvancedGrid<>(grid);
        advancedGrid.enableExport(true);

        advancedGrid.addToFilters(
                nameFilterField, agelessEqualFilterField, ageGreaterEqualFilterField);
        advancedGrid.addToActions(
                new Button("New Item", VaadinIcon.PLUS.create()),
                new Button("Refresh", VaadinIcon.REFRESH.create()));
        add(advancedGrid);
    }
}
