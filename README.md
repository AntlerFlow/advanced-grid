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


#### Customize the export button
```java
grid.getExportButton().setText("Export");
```

## License

Apache License 2.0 (see `LICENSE`).