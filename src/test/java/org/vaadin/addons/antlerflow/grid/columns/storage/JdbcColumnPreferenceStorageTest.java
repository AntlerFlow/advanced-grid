package org.vaadin.addons.antlerflow.grid.columns.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vaadin.flow.component.html.Div;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

class JdbcColumnPreferenceStorageTest {

    private static final String KEY = "af-advanced-grid.columns./people";

    private JdbcDataSource dataSource;
    private final AtomicReference<String> currentUser = new AtomicReference<>("alice");
    private JdbcColumnPreferenceStorage storage;
    private final ColumnPreferenceContext context = new ColumnPreferenceContext(KEY, new Div());

    @BeforeEach
    void setUp() {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        storage = new JdbcColumnPreferenceStorage(dataSource, currentUser::get);
        storage.initializeSchema();
    }

    @Test
    void initializeSchemaIsIdempotent() {
        storage.initializeSchema();
        storage.save(context, visibility("age", false));

        assertThat(load()).containsExactlyEntriesOf(visibility("age", false));
    }

    @Test
    void savesUpdatesAndLoadsInColumnOrder() {
        storage.save(context, visibility("id", true, "age", false));
        storage.save(context, visibility("id", false, "firstName", true, "age", true));

        assertThat(load())
                .containsExactlyEntriesOf(visibility("id", false, "firstName", true, "age", true));
    }

    @Test
    void keepsPreferencesPerUserAndPerKey() {
        storage.save(context, visibility("age", false));
        storage.save(new ColumnPreferenceContext("other", new Div()), visibility("id", false));

        currentUser.set("bob");
        assertThat(load()).isEmpty();
        storage.save(context, visibility("age", true));

        currentUser.set("alice");
        assertThat(load()).containsExactlyEntriesOf(visibility("age", false));
    }

    @Test
    void clearRemovesOnlyThatPreference() {
        storage.save(context, visibility("age", false));
        currentUser.set("bob");
        storage.save(context, visibility("age", false));

        storage.clear(context);

        assertThat(load()).isEmpty();
        currentUser.set("alice");
        assertThat(load()).isNotEmpty();
    }

    @Test
    void anonymousUsersAreNeitherReadNorWritten() throws SQLException {
        currentUser.set(null);
        storage.save(context, visibility("age", false));
        assertThat(load()).isEmpty();

        currentUser.set(" ");
        storage.save(context, visibility("age", false));
        assertThat(rowCount()).isZero();
    }

    @Test
    void databaseFailuresFallBackToDefaultsInsteadOfThrowing() throws SQLException {
        storage.save(context, visibility("age", false));
        try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {
            s.execute("DROP TABLE " + JdbcColumnPreferenceStorage.DEFAULT_TABLE_NAME);
        }

        assertThat(load()).isEmpty();
        storage.save(context, visibility("age", true)); // logged, not thrown
        storage.clear(context);
    }

    @Test
    void supportsSchemaQualifiedCustomTable() throws SQLException {
        try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {
            s.execute("CREATE SCHEMA app");
        }
        JdbcColumnPreferenceStorage custom =
                new JdbcColumnPreferenceStorage(dataSource, currentUser::get, "app.grid_prefs");
        custom.initializeSchema();
        custom.initializeSchema();

        custom.save(context, visibility("age", false));

        AtomicReference<Map<String, Boolean>> loaded = new AtomicReference<>();
        custom.load(context, loaded::set);
        assertThat(loaded.get()).containsExactlyEntriesOf(visibility("age", false));
    }

    @Test
    void rejectsTableNamesThatAreNotPlainIdentifiers() {
        assertThatThrownBy(
                        () ->
                                new JdbcColumnPreferenceStorage(
                                        dataSource, currentUser::get, "prefs; DROP TABLE users"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private Map<String, Boolean> load() {
        AtomicReference<Map<String, Boolean>> loaded = new AtomicReference<>();
        storage.load(context, loaded::set);
        assertThat(loaded.get()).as("callback invoked").isNotNull();
        return loaded.get();
    }

    private int rowCount() throws SQLException {
        try (Connection c = dataSource.getConnection();
                Statement s = c.createStatement();
                var rs =
                        s.executeQuery(
                                "SELECT COUNT(*) FROM "
                                        + JdbcColumnPreferenceStorage.DEFAULT_TABLE_NAME)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private static Map<String, Boolean> visibility(Object... idAndVisible) {
        Map<String, Boolean> map = new LinkedHashMap<>();
        for (int i = 0; i < idAndVisible.length; i += 2) {
            map.put((String) idAndVisible[i], (Boolean) idAndVisible[i + 1]);
        }
        return map;
    }
}
