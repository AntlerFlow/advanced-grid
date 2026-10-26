package org.vaadin.addons.antlerflow.grid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.server.VaadinSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.vaadin.addons.antlerflow.grid.columns.storage.ColumnPreferenceContext;
import org.vaadin.addons.antlerflow.grid.columns.storage.ColumnPreferenceStorage;
import org.vaadin.addons.antlerflow.grid.columns.storage.InMemoryColumnPreferenceStorage;
import org.vaadin.addons.antlerflow.grid.model.Person;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

class AdvancedGridColumnPreferenceTest {

    private static final String KEY = AdvancedGrid.COLUMN_PREFERENCE_KEY_PREFIX + "people";

    private final AtomicReference<String> user = new AtomicReference<>("alice");
    private final InMemoryColumnPreferenceStorage storage =
            new InMemoryColumnPreferenceStorage(user::get);

    private UI ui;
    private AdvancedGrid<Person> advancedGrid;
    private Grid.Column<Person> age;

    @BeforeEach
    void setUp() {
        VaadinSession session = mock(VaadinSession.class);
        when(session.hasLock()).thenReturn(true);
        ui = new UI();
        ui.getInternals().setSession(session);
        UI.setCurrent(ui);
        advancedGrid = new AdvancedGrid<>();
        advancedGrid.getInnerGrid().addColumn(Person::getFirstName).setKey("firstName");
        age = advancedGrid.getInnerGrid().addColumn(Person::getAge).setKey("age");
        advancedGrid.setColumnPreferenceKey("people");
        advancedGrid.setColumnPreferenceStorage(storage);
        advancedGrid.enableColumnSelection(true);
    }

    @AfterEach
    void tearDown() {
        UI.setCurrent(null);
    }

    @Test
    void defaultsToBrowserLocalStorage() {
        assertThat(new AdvancedGrid<Person>().getColumnPreferenceStorage())
                .isSameAs(ColumnPreferenceStorage.localStorage());
    }

    @Test
    void appliesTheStoredPreferenceOnAttach() {
        storage.save(context(), Map.of("age", false));

        ui.add(advancedGrid);

        assertThat(age.isVisible()).isFalse();
        assertThat(advancedGrid.getColumnPreferenceStorageKey()).contains(KEY);
    }

    @Test
    void preferencesArePerUser() {
        storage.save(context(), Map.of("age", false));
        user.set("bob");

        ui.add(advancedGrid);

        assertThat(age.isVisible()).isTrue();
    }

    @Test
    void resetClearsTheStoredPreference() {
        storage.save(context(), Map.of("age", false));
        ui.add(advancedGrid);

        advancedGrid.getColumnSelector().getResetButton().click();

        assertThat(age.isVisible()).isTrue();
        assertThat(loadStored()).isEmpty();
    }

    @Test
    void switchingStorageLoadsFromTheNewOne() {
        ui.add(advancedGrid);
        InMemoryColumnPreferenceStorage other = new InMemoryColumnPreferenceStorage(user::get);
        other.save(context(), Map.of("age", false));

        advancedGrid.setColumnPreferenceStorage(other);

        assertThat(age.isVisible()).isFalse();
    }

    @Test
    void nothingIsLoadedWhenPersistenceIsOff() {
        storage.save(context(), Map.of("age", false));
        advancedGrid.setColumnPreferencePersisted(false);

        ui.add(advancedGrid);

        assertThat(age.isVisible()).isTrue();
    }

    private ColumnPreferenceContext context() {
        return new ColumnPreferenceContext(KEY, new Div());
    }

    private Map<String, Boolean> loadStored() {
        AtomicReference<Map<String, Boolean>> loaded = new AtomicReference<>();
        storage.load(context(), loaded::set);
        return loaded.get();
    }
}
