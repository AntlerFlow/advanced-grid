# Advanced Grid

`advanced-grid` is a Vaadin add-on that wraps a standard Vaadin `Grid` and adds commonly needed “batteries included” features like:

- **Pagination UI** (page size chooser, navigation, optional page jump)
- **Export** (CSV/DOCX/HTML/ODS/ODT/PDF/PPTX/RTF/TEXT/XLSX via a pluggable exporter provider)
- **Convenient APIs** while still giving you access to the underlying Vaadin `Grid`

> The component is designed to **compose** a Vaadin `Grid`, so you can keep using normal Grid APIs and patterns.

---

## Demo / Running locally

Run the below command from the project root directory:
```
mvn spring-boot:run
```
Then open:

- `http://localhost:8080`

> If your environment uses a different port, check the Maven output for the actual URL/port.


## Installation

```xml
<dependency>
    <groupId>org.vaadin.addons.antlerflow</groupId>
    <artifactId>advanced-grid</artifactId>
    <version>${advanced-grid.version}</version>
</dependency>
```

## Quick start

### Wrap an existing `Grid`

Use this when you already have a configured `Grid` (columns, renderers, selection, etc.):

```java
Grid<Person> vaadinGrid = new Grid<>(Person.class);

AdvancedGrid<Person> grid = new AdvancedGrid<>(vaadinGrid);
```

### Create a new `AdvancedGrid`

```java
AdvancedGrid<Person> grid = new AdvancedGrid<>(Person.class);
// or
AdvancedGrid<Person> grid = new AdvancedGrid<>();
grid.getInnerGrid().addColumn(Person::getFirstName).setHeader("First Name");
grid.getInnerGrid().addColumn(Person::getLastName).setHeader("Last Name");
grid.getInnerGrid().addColumn(Person::getAge).setHeader("Age");
```

## Features

### Pagination

To enable pagination:
```java
grid.setPaginationVisibility(true);
```
To set default page size and size options:
```java
grid.setPageSizes(10, 20, 50, 100);
grid.setPageSize(20);
```

To customize paginator:
```java
grid.getPaginator().showPageSize(false);
grid.getPaginator().setHideEdges(true);
grid.getPaginator().showPageJump(false);
```

Add below css to make circly shaped pagination buttons:
```css
af-paginator::part(page-button) {
    width: var(--af-paginator-control-height);
    min-width: var(--af-paginator-control-height);
    padding: 0;
    border-radius: 9999px;

    display: inline-flex;
    align-items: center;
    justify-content: center;
}
```

#### Spring Data Grid Integration

To integrate with Spring Data uses `SpringPageableDataProvider` as the data provider.

Example:
```java
PersonFilter personFilter = new PersonFilter();
SpringPageableDataProvider<Person, PersonFilter> dataProvider =
        new SpringPageableDataProvider<>((filter, pageable) -> personService.findAll(filter, pageable));
dataProvider.setFilter(personFilter);
advancedGrid.setDataProvider(dataProvider);
```


### Grid Export

AdvancedGrid uses [GridExporter for Vaadin](https://vaadin.com/directory/component/gridexporter-for-vaadin) as the default export provider.

#### Enable export

```java
grid.enableExport(true);
```

#### Configure export

```java
GridExportConfig<Person> exportConfig =
        GridExportConfig.<Person>builder()
                .fileName("people-report")
                .size(ExportSize.ALL)
                .formats(List.of("pdf", "xlsx", "csv"))
                .limit(1000)
                .build();
grid.setExportConfig(exportConfig);

// Below is an example of customizing the export title
grid.setExportHandler(
    new XDEVSoftwareGridExportHandler<>() {
        @Override
        protected GridExporterProvider getProvider() {
            return new PredefinedTitleProvider("Custom Title!");
        }
});
```

The default export size is `ExportSize.CURRENT_PAGE`.
Tips:
- If you use `ExportSize.ALL`, ensure your backend/export limits are appropriate to avoid excessive memory usage.
- Prefer server-side paging for large datasets and export only what you actually need.


#### Export button state

The export button is enabled only while there is something to export: the grid has rows
(after filtering), and for `ExportSize.SELECTED` at least one row is selected. It updates
automatically on data, filter, page and selection changes.

#### Export selected rows only

When the grid allows selection and rows are selected, clicking the export button lets the user
choose between **Export all rows** (or **Export current page** for `CURRENT_DISPLAY` with
pagination) and **Export selected rows (n)**. With nothing selected it exports directly.

```java
grid.getInnerGrid().setSelectionMode(Grid.SelectionMode.MULTI);

// Always export only the selected rows (button disabled until a row is selected):
grid.setExportConfig(GridExportConfig.<Person>builder().size(ExportSize.SELECTED).build());

// Or keep the configured scope and hide the "selected rows" choice:
grid.setExportConfig(GridExportConfig.<Person>builder().selectedRowsOption(false).build());
```

Selected rows are exported in the grid's sort order (in-memory sorting), otherwise in selection
order, capped at `limit`.

#### Customize the export button
```java
grid.getExportButton().setText("Export");
grid.getExportSelectedButton(); // the "Export selected rows" option
grid.getExportScopeButton();    // the "Export all rows / current page" option
```

### Column selection (remembered per user)

```java
grid.enableColumnSelection(true);
```

Adds a button next to export that opens a list of the grid's columns. The user's choice of
displayed columns is stored (in the browser's `localStorage` by default, see
[Preference storage](#preference-storage)) and restored whenever the grid is shown again. At least one column always stays visible, and **Reset to default** restores the
visibility the columns had when the grid was built (and deletes the stored preference).

- Columns are identified by their key, otherwise their header text, otherwise their position;
  set `column.setKey(...)` for identifiers that survive header renames.
- The storage key is `af-advanced-grid.columns.<suffix>`, where the suffix is the value of
  `setColumnPreferenceKey(...)`, otherwise the component id, otherwise the route path. Give each
  grid its own key when a view holds several grids:

```java
grid.setColumnPreferenceKey("people-grid");
grid.setColumnPreferencePersisted(false); // keep the selector, do not store the choice
grid.clearColumnPreference();             // delete the stored choice
```

#### Preference storage

Choose where the column choice is kept with `setColumnPreferenceStorage(...)`:

| Storage | Kept | Scope |
|---|---|---|
| `ColumnPreferenceStorage.localStorage()` (default) | until browser data is cleared | one browser |
| `ColumnPreferenceStorage.sessionStorage()` | until the tab is closed | one tab |
| `JdbcColumnPreferenceStorage` | in your database | the signed-in user, on every device |
| `InMemoryColumnPreferenceStorage` | until the server restarts | the user; prototypes and tests |

```java
grid.setColumnPreferenceStorage(ColumnPreferenceStorage.sessionStorage());
```

**Database (per user).** Plain JDBC on any `DataSource`, so it works with any SQL database
and no extra dependency. Create one shared instance, for example as a Spring bean:

```java
@Bean
JdbcColumnPreferenceStorage columnPreferenceStorage(DataSource dataSource) {
    return new JdbcColumnPreferenceStorage(
            dataSource,
            // the current user's stable id; anonymous visitors (null) are not stored
            ServerSideColumnPreferenceStorage.requestPrincipalName());
}

// in the view
grid.setColumnPreferenceStorage(columnPreferenceStorage);
```

`requestPrincipalName()` reads the authenticated user of the current request (Spring Security
and servlet security both set it). Pass your own supplier for another user id. Create the table
with a migration:

```sql
CREATE TABLE af_grid_column_preference (
    user_id          VARCHAR(255)  NOT NULL,
    preference_key   VARCHAR(255)  NOT NULL,
    preference_value VARCHAR(4000) NOT NULL,
    PRIMARY KEY (user_id, preference_key)
);
```

or call `storage.initializeSchema()` once at startup (demos, simple deployments). A different
table, optionally schema-qualified, is the third constructor argument
(`new JdbcColumnPreferenceStorage(ds, users, "app.grid_prefs")`).

If the database is unavailable, the failure is logged as a warning
(`column_preference_load_failed` / `_save_failed` / `_clear_failed`) and the grid keeps its
default columns. A preference never breaks the page.

**Your own storage.** For any server-side destination (JPA repository, Redis, a REST service),
extend `ServerSideColumnPreferenceStorage`. It resolves the user, skips anonymous visitors and
logs failures, so you write three methods:

```java
public class JpaColumnPreferenceStorage extends ServerSideColumnPreferenceStorage {
    private final GridPreferenceRepository repository;

    public JpaColumnPreferenceStorage(GridPreferenceRepository repository) {
        super(ServerSideColumnPreferenceStorage.requestPrincipalName());
        this.repository = repository;
    }

    @Override
    protected Optional<Map<String, Boolean>> read(String userId, String key) {
        return repository.findByUserIdAndKey(userId, key).map(e -> ColumnPreferences.decode(e.getValue()));
    }

    @Override
    protected void write(String userId, String key, Map<String, Boolean> visibility) {
        repository.upsert(userId, key, ColumnPreferences.encode(visibility));
    }

    @Override
    protected void delete(String userId, String key) {
        repository.deleteByUserIdAndKey(userId, key);
    }
}
```

For asynchronous destinations, implement `ColumnPreferenceStorage` directly (`load` hands its
result to a callback, which may run later). `ColumnPreferences.encode/decode` convert a
preference to and from one text value (`+id` visible / `-id` hidden, one per line).

## License

Apache License 2.0 (see `LICENSE`).