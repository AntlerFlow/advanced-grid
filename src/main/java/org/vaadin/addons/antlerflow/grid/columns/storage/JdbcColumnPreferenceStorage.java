package org.vaadin.addons.antlerflow.grid.columns.storage;

import com.vaadin.flow.function.SerializableSupplier;
import org.vaadin.addons.antlerflow.grid.columns.ColumnPreferences;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

/**
 * Stores preferences per user in a relational database through plain JDBC, so they follow the
 * user across browsers and devices. Works with any {@link DataSource} (Spring Boot's pooled one
 * included) and any SQL database; no ORM is required.
 *
 * <p>Table (create it with your migrations from {@link #getCreateTableSql()}, or call {@link
 * #initializeSchema()} once at startup):
 *
 * <pre>
 * CREATE TABLE af_grid_column_preference (
 *     user_id          VARCHAR(255)  NOT NULL,
 *     preference_key   VARCHAR(255)  NOT NULL,
 *     preference_value VARCHAR(4000) NOT NULL,
 *     PRIMARY KEY (user_id, preference_key)
 * )
 * </pre>
 *
 * <p>{@code preference_value} holds one line per column, {@code +id} visible or {@code -id}
 * hidden. Create one instance per application (for example a Spring bean) and share it.
 *
 * <p>The data source is not serialized with the session. If sessions are serialized
 * (clustering, session persistence), set the storage on the grid when the view is created; a
 * deserialized instance logs a warning and keeps the default columns instead of failing.
 */
public class JdbcColumnPreferenceStorage extends ServerSideColumnPreferenceStorage {

    public static final String DEFAULT_TABLE_NAME = "af_grid_column_preference";

    /** Plain or schema-qualified SQL identifier; the table name cannot be a bound parameter. */
    private static final Pattern TABLE_NAME =
            Pattern.compile("[A-Za-z_][A-Za-z0-9_]{0,62}(\\.[A-Za-z_][A-Za-z0-9_]{0,62})?");

    private static final String INTEGRITY_VIOLATION_SQL_STATE_CLASS = "23";

    private final transient DataSource dataSource;
    private final String tableName;

    /** Uses the {@link #DEFAULT_TABLE_NAME default table}. */
    public JdbcColumnPreferenceStorage(
            DataSource dataSource, SerializableSupplier<String> userIdSupplier) {
        this(dataSource, userIdSupplier, DEFAULT_TABLE_NAME);
    }

    /**
     * @param tableName a table name, optionally schema-qualified ({@code app.grid_prefs})
     * @throws IllegalArgumentException if the table name is not a plain SQL identifier
     */
    public JdbcColumnPreferenceStorage(
            DataSource dataSource,
            SerializableSupplier<String> userIdSupplier,
            String tableName) {
        super(userIdSupplier);
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
        Objects.requireNonNull(tableName, "tableName must not be null");
        if (!TABLE_NAME.matcher(tableName).matches()) {
            throw new IllegalArgumentException("Invalid table name: " + tableName);
        }
        this.tableName = tableName;
    }

    public String getTableName() {
        return tableName;
    }

    /** Returns portable DDL for the preference table, for use in schema migrations. */
    public String getCreateTableSql() {
        return "CREATE TABLE "
                + tableName
                + " (user_id VARCHAR(255) NOT NULL,"
                + " preference_key VARCHAR(255) NOT NULL,"
                + " preference_value VARCHAR(4000) NOT NULL,"
                + " PRIMARY KEY (user_id, preference_key))";
    }

    /**
     * Creates the preference table when it does not exist. Prefer schema migrations (Flyway,
     * Liquibase) in production; this is a convenience for demos and simple deployments.
     *
     * @throws ColumnPreferenceStorageException if the database cannot be inspected or changed
     */
    public void initializeSchema() {
        try (Connection connection = dataSource.getConnection()) {
            if (!tableExists(connection.getMetaData())) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute(getCreateTableSql());
                }
            }
        } catch (SQLException ex) {
            throw new ColumnPreferenceStorageException(
                    "Could not create column preference table " + tableName, ex);
        }
    }

    @Override
    protected Optional<Map<String, Boolean>> read(String userId, String key) {
        String sql =
                "SELECT preference_value FROM "
                        + tableName
                        + " WHERE user_id = ? AND preference_key = ?";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userId);
            statement.setString(2, key);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next()
                        ? Optional.of(ColumnPreferences.decode(resultSet.getString(1)))
                        : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new ColumnPreferenceStorageException(
                    "Could not read column preference " + key, ex);
        }
    }

    @Override
    protected void write(String userId, String key, Map<String, Boolean> visibility) {
        String value = ColumnPreferences.encode(visibility);
        try (Connection connection = dataSource.getConnection()) {
            if (update(connection, userId, key, value) > 0) {
                return;
            }
            try {
                insert(connection, userId, key, value);
            } catch (SQLException ex) {
                // Another request inserted the row first (e.g. two tabs): update it instead.
                if (!isIntegrityViolation(ex) || update(connection, userId, key, value) == 0) {
                    throw ex;
                }
            }
        } catch (SQLException ex) {
            throw new ColumnPreferenceStorageException(
                    "Could not write column preference " + key, ex);
        }
    }

    @Override
    protected void delete(String userId, String key) {
        String sql = "DELETE FROM " + tableName + " WHERE user_id = ? AND preference_key = ?";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userId);
            statement.setString(2, key);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new ColumnPreferenceStorageException(
                    "Could not delete column preference " + key, ex);
        }
    }

    private int update(Connection connection, String userId, String key, String value)
            throws SQLException {
        String sql =
                "UPDATE "
                        + tableName
                        + " SET preference_value = ? WHERE user_id = ? AND preference_key = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            statement.setString(2, userId);
            statement.setString(3, key);
            return statement.executeUpdate();
        }
    }

    private void insert(Connection connection, String userId, String key, String value)
            throws SQLException {
        String sql =
                "INSERT INTO "
                        + tableName
                        + " (user_id, preference_key, preference_value) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userId);
            statement.setString(2, key);
            statement.setString(3, value);
            statement.executeUpdate();
        }
    }

    private boolean tableExists(DatabaseMetaData metaData) throws SQLException {
        String schema = null;
        String table = tableName;
        int dot = tableName.indexOf('.');
        if (dot > 0) {
            schema = tableName.substring(0, dot);
            table = tableName.substring(dot + 1);
        }
        // Databases fold unquoted identifiers to upper (H2, Oracle) or lower case (PostgreSQL).
        List<UnaryOperator<String>> foldings =
                List.of(UnaryOperator.identity(), String::toUpperCase, String::toLowerCase);
        for (UnaryOperator<String> fold : foldings) {
            String schemaPattern = schema == null ? null : fold.apply(schema);
            try (ResultSet tables =
                    metaData.getTables(
                            null, schemaPattern, fold.apply(table), new String[] {"TABLE"})) {
                if (tables.next()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isIntegrityViolation(SQLException ex) {
        return ex.getSQLState() != null
                && ex.getSQLState().startsWith(INTEGRITY_VIOLATION_SQL_STATE_CLASS);
    }
}
