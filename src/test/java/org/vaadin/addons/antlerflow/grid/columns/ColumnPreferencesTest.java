package org.vaadin.addons.antlerflow.grid.columns;

import static org.assertj.core.api.Assertions.assertThat;

import com.vaadin.flow.component.grid.Grid;
import org.junit.jupiter.api.Test;
import org.vaadin.addons.antlerflow.grid.model.Person;

import java.util.LinkedHashMap;
import java.util.Map;

class ColumnPreferencesTest {

    @Test
    void encodeAndDecodeRoundTrip() {
        Map<String, Boolean> visibility = new LinkedHashMap<>();
        visibility.put("firstName", true);
        visibility.put("Last Name", false);

        String encoded = ColumnPreferences.encode(visibility);

        assertThat(encoded).isEqualTo("+firstName\n-Last Name");
        assertThat(ColumnPreferences.decode(encoded)).containsExactlyEntriesOf(visibility);
    }

    @Test
    void decodeIgnoresBlankAndMalformedInput() {
        assertThat(ColumnPreferences.decode(null)).isEmpty();
        assertThat(ColumnPreferences.decode("")).isEmpty();
        assertThat(ColumnPreferences.decode("garbage\n+\n-age\n?x"))
                .containsExactlyEntriesOf(Map.of("age", false));
    }

    @Test
    void columnIdPrefersKeyThenHeaderThenPosition() {
        Grid<Person> grid = new Grid<>();
        Grid.Column<Person> keyed = grid.addColumn(Person::getAge).setKey("age").setHeader("Age");
        Grid.Column<Person> headed = grid.addColumn(Person::getFirstName).setHeader("First\nName");
        Grid.Column<Person> bare = grid.addColumn(Person::getLastName);

        assertThat(ColumnPreferences.columnId(keyed, 0)).isEqualTo("age");
        assertThat(ColumnPreferences.columnId(headed, 1)).isEqualTo("First Name");
        assertThat(ColumnPreferences.columnId(bare, 2)).isEqualTo("#2");
        assertThat(ColumnPreferences.label(bare, 2)).isEqualTo("Column 3");
    }
}
