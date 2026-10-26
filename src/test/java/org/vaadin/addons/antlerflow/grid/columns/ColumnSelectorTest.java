package org.vaadin.addons.antlerflow.grid.columns;

import static org.assertj.core.api.Assertions.assertThat;

import com.vaadin.flow.component.grid.Grid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.vaadin.addons.antlerflow.grid.model.Person;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class ColumnSelectorTest {

    private Grid<Person> grid;
    private Grid.Column<Person> firstName;
    private Grid.Column<Person> lastName;
    private Grid.Column<Person> age;
    private final List<Map<String, Boolean>> reported = new ArrayList<>();
    private ColumnSelector<Person> selector;

    @BeforeEach
    void setUp() {
        grid = new Grid<>();
        firstName = grid.addColumn(Person::getFirstName).setKey("firstName").setHeader("First");
        lastName = grid.addColumn(Person::getLastName).setKey("lastName").setHeader("Last");
        age = grid.addColumn(Person::getAge).setKey("age").setHeader("Age");
        age.setVisible(false); // developer default
        selector = new ColumnSelector<>(grid, reported::add);
    }

    @Test
    void applyPreferenceSetsVisibilityAndIgnoresUnknownColumns() {
        selector.applyPreference(Map.of("lastName", false, "age", true, "removed", true));

        assertThat(firstName.isVisible()).isTrue();
        assertThat(lastName.isVisible()).isFalse();
        assertThat(age.isVisible()).isTrue();
        assertThat(selector.getColumnCheckboxes().getValue()).containsExactlyInAnyOrder(firstName, age);
    }

    @Test
    void applyPreferenceNeverHidesEveryColumn() {
        selector.applyPreference(Map.of("firstName", false, "lastName", false, "age", false));

        assertThat(firstName.isVisible()).isTrue();
        assertThat(lastName.isVisible()).isTrue();
    }

    @Test
    void lastVisibleColumnCannotBeUnchecked() {
        selector.applyPreference(Map.of("lastName", false));

        var enabled = selector.getColumnCheckboxes().getItemEnabledProvider();
        assertThat(enabled.test(firstName)).isFalse();
        assertThat(enabled.test(lastName)).isTrue();
    }

    @Test
    void resetRestoresDeveloperDefaultsAndClearsPreference() {
        selector.applyPreference(Map.of("firstName", false, "age", true));

        selector.reset();

        assertThat(firstName.isVisible()).isTrue();
        assertThat(lastName.isVisible()).isTrue();
        assertThat(age.isVisible()).isFalse();
        assertThat(reported).containsExactly((Map<String, Boolean>) null);
    }

    @Test
    void refreshPicksUpColumnsAddedLater() {
        Grid.Column<Person> id = grid.addColumn(Person::getId).setKey("id");

        selector.refresh();

        assertThat(selector.getColumnCheckboxes().getListDataView().getItems())
                .contains(id);
        assertThat(selector.getVisibility()).containsKeys("firstName", "lastName", "age", "id");
    }
}
