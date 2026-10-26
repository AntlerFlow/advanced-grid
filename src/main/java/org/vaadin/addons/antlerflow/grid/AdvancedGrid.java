package org.vaadin.addons.antlerflow.grid;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.component.popover.PopoverPosition;
import com.vaadin.flow.data.provider.*;
import com.vaadin.flow.data.selection.MultiSelectionEvent;
import com.vaadin.flow.data.selection.SingleSelectionEvent;
import com.vaadin.flow.shared.Registration;
import lombok.Getter;
import lombok.Setter;
import org.vaadin.addons.antlerflow.grid.columns.ColumnSelector;
import org.vaadin.addons.antlerflow.grid.columns.storage.ColumnPreferenceContext;
import org.vaadin.addons.antlerflow.grid.columns.storage.ColumnPreferenceStorage;
import org.vaadin.addons.antlerflow.grid.exporter.ExportSize;
import org.vaadin.addons.antlerflow.grid.exporter.GridExportConfig;
import org.vaadin.addons.antlerflow.grid.exporter.GridExportHandler;
import org.vaadin.addons.antlerflow.grid.exporter.xdevsoftware.XDEVSoftwareGridExportHandler;
import org.vaadin.addons.antlerflow.grid.pagination.Paginator;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AdvancedGrid is a component that enhances the functionality of a standard Grid by incorporating
 * built-in pagination, data export capabilities, a column selector that remembers the user's
 * choice (in the browser, a database or any custom storage), and a customizable toolbar. It is designed to work with various data
 * providers and supports dynamic setting of data, page sizes, and pagination visibility.
 *
 * @param <T> the type of items contained in the grid
 */
@Tag("af-advanced-grid")
@JsModule("./antlerflow/advanced-grid/advanced-grid.ts")
public class AdvancedGrid<T> extends Component implements HasComponents, HasSize, HasStyle {

    /** Prefix of the storage keys holding column preferences. */
    public static final String COLUMN_PREFERENCE_KEY_PREFIX = "af-advanced-grid.columns.";

    @Getter private Grid<T> innerGrid;
    @Getter private Paginator paginator;
    @Getter private GridExportConfig<T> exportConfig;

    @Getter @Setter
    private GridExportHandler<T> exportHandler = new XDEVSoftwareGridExportHandler<>();

    private SortedSet<Integer> pageSizes = new TreeSet<>(List.of(25, 50, 100));

    /**
     * If set, this is the data source for pagination + exporting. Otherwise, we use the grid's
     * original provider.
     */
    private DataProvider<T, ?> externalDataProvider;

    /**
     * The provider that was on the injected inner grid (before we temporarily replace it with a
     * paged provider).
     */
    private DataProvider<T, ?> originalInnerGridDataProvider;

    private boolean paginated = false;

    private final List<Registration> registrations = new ArrayList<>();
    private final List<Registration> innerGridRegistrations = new ArrayList<>();

    @Getter private Button exportButton;
    private Popover exportPopover;
    @Getter private Button exportScopeButton;
    @Getter private Button exportSelectedButton;

    @Getter private Button columnSelectionButton;
    @Getter private ColumnSelector<T> columnSelector;
    private Popover columnPopover;

    /** Explicit preference key; when {@code null} a key is derived on attach. */
    private String columnPreferenceKey;

    @Getter private boolean columnPreferencePersisted = true;

    @Getter
    private ColumnPreferenceStorage columnPreferenceStorage =
            ColumnPreferenceStorage.localStorage();

    public AdvancedGrid() {
        this(new Grid<>());
    }

    public AdvancedGrid(Grid<T> grid) {
        initPagination();
        setInnerGrid(grid);
    }

    public AdvancedGrid(DataProvider<T, Void> dataProvider) {
        this(new Grid<>(dataProvider));
    }

    public AdvancedGrid(BackEndDataProvider<T, Void> dataProvider) {
        this(new Grid<>(dataProvider));
    }

    public AdvancedGrid(InMemoryDataProvider<T> inMemoryDataProvider) {
        this(new Grid<>(inMemoryDataProvider));
    }

    public AdvancedGrid(ListDataProvider<T> dataProvider) {
        this(new Grid<>(dataProvider));
    }

    public AdvancedGrid(Collection<T> items) {
        this(new Grid<>(items));
    }

    public AdvancedGrid(int pageSize) {
        this(new Grid<>(pageSize));
    }

    public AdvancedGrid(Class<T> beanType, boolean autoCreateColumns) {
        this(new Grid<>(beanType, autoCreateColumns));
    }

    public AdvancedGrid(Class<T> beanType) {
        this(beanType, true);
    }

    private void initPagination() {
        paginator = new Paginator();
        paginator.setVisible(false);
        paginator.getElement().setAttribute("slot", "paginator");
        setPageSizes(pageSizes.toArray(new Integer[0]));
        paginator.addPageChangeListener(e -> refreshGridData());
        add(paginator);
    }

    /** Injects the given Grid into the "grid" slot inside the Lit template. */
    public void setInnerGrid(Grid<T> innerGrid) {
        Objects.requireNonNull(innerGrid, "grid must not be null");

        if (this.innerGrid != null) {
            this.innerGrid.getElement().removeAttribute("slot");
            this.innerGrid.removeFromParent();
        }
        innerGridRegistrations.forEach(Registration::remove);
        innerGridRegistrations.clear();

        this.innerGrid = innerGrid;
        this.innerGrid.setSizeFull();
        this.innerGrid.getElement().setAttribute("slot", "grid");

        this.originalInnerGridDataProvider = this.innerGrid.getDataProvider();

        innerGridRegistrations.add(this.innerGrid.addSortListener(e -> resetPagination()));
        // Listen on the grid rather than via Grid#addSelectionListener: that binds to the
        // current selection model and is lost when setSelectionMode(...) is called later.
        innerGridRegistrations.add(onSelectionEvent(MultiSelectionEvent.class));
        innerGridRegistrations.add(onSelectionEvent(SingleSelectionEvent.class));
        registerDataProviderListeners(getBaseDataProvider());

        recalcTotalItems();
        refreshGridData();

        add(this.innerGrid);

        if (columnSelector != null) {
            columnSelector.setGrid(innerGrid);
            loadColumnPreference();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Registration onSelectionEvent(Class<? extends ComponentEvent> eventType) {
        ComponentEventListener listener = event -> updateExportState();
        return ComponentUtil.addListener(innerGrid, (Class) eventType, listener);
    }

    private void resetPagination() {
        paginator.setPage(1);
        refreshGridData();
    }

    public void setItems(List<T> items) {
        Objects.requireNonNull(items, "items must not be null");
        setDataProvider(new ListDataProvider<>(items));
    }

    public void setDataProvider(DataProvider<T, ?> dataProvider) {
        Objects.requireNonNull(dataProvider, "dataProvider must not be null");
        this.externalDataProvider = dataProvider;
        registerDataProviderListeners(dataProvider);
        if (!paginated) {
            innerGrid.setDataProvider(dataProvider);
        }
        recalcTotalItems();
        resetPagination();
    }

    private void registerDataProviderListeners(DataProvider<T, ?> dataProvider) {
        clearRegistrations();
        if (dataProvider != null) {
            registrations.add(dataProvider.addDataProviderListener(e -> resetPagination()));
        }
    }

    private void clearRegistrations() {
        registrations.forEach(Registration::remove);
        registrations.clear();
    }

    private void recalcTotalItems() {
        if (!paginated) {
            return;
        }
        DataProvider<T, ?> base = getBaseDataProvider();
        if (base == null) {
            paginator.setTotalItems(0);
            return;
        }
        Query query = buildQuery(paginator.getOffset(), paginator.getPageSize());
        paginator.setTotalItems(base.size(query));
    }

    public void setPageSizes(Integer... pageSizes) {
        this.pageSizes = new TreeSet<>(List.of(pageSizes));
        paginator.setPageSizeOptions(
                this.pageSizes.stream().map(String::valueOf).collect(Collectors.joining(",")));
    }

    public void setPageSize(int pageSize) {
        if (!pageSizes.contains(pageSize)) {
            pageSizes.add(pageSize);
            setPageSizes(pageSizes.toArray(new Integer[0]));
        }
        innerGrid.setPageSize(pageSize);
        paginator.setPageSize(pageSize);
        resetPagination();
    }

    // ---------------------------------------------------------------- export

    public void setExportConfig(GridExportConfig<T> exportConfig) {
        this.exportConfig = exportConfig;
        updateExportState();
    }

    /**
     * Shows or removes the export button. The button is enabled only while the grid has data to
     * export (and, for {@link ExportSize#SELECTED}, while rows are selected). When rows are
     * selected and {@link GridExportConfig#isSelectedRowsOption()} is on, clicking it lets the user
     * export either the configured rows or only the selected rows.
     */
    public void enableExport(boolean exportEnabled) {
        if (exportEnabled) {
            if (exportButton == null) {
                exportButton = new Button(VaadinIcon.DOWNLOAD.create());
                applyTertiary(exportButton);
                exportButton.setAriaLabel("Export");
                exportButton.setTooltipText("Export");
                exportButton.getElement().setAttribute("slot", "export");
                exportButton.addClickListener(event -> onExportClicked());
                add(exportButton);
                initExportPopover();
            }
            updateExportState();
        } else {
            if (exportPopover != null) {
                exportPopover.close();
                exportPopover.setTarget(null);
                exportPopover.removeFromParent();
                exportPopover = null;
            }
            if (exportButton != null) {
                exportButton.removeFromParent();
                exportButton = null;
            }
            exportScopeButton = null;
            exportSelectedButton = null;
        }
    }

    /**
     * Returns {@code true} when there is something to export with the current configuration: the
     * grid has rows, and for {@link ExportSize#SELECTED} at least one row is selected.
     */
    public boolean isExportAvailable() {
        if (exportSize() == ExportSize.SELECTED) {
            return !innerGrid.getSelectedItems().isEmpty();
        }
        return hasData();
    }

    private void initExportPopover() {
        exportScopeButton = new Button();
        exportScopeButton.addClickListener(
                e -> {
                    exportPopover.close();
                    export(exportConfig);
                });
        exportSelectedButton = new Button();
        exportSelectedButton.addClickListener(
                e -> {
                    exportPopover.close();
                    export(selectedRowsConfig());
                });

        Div content = new Div(exportScopeButton, exportSelectedButton);
        content.addClassName("af-export-options");
        content.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column");

        exportPopover = new Popover(content);
        exportPopover.setTarget(exportButton);
        exportPopover.setOpenOnClick(false);
        exportPopover.setPosition(PopoverPosition.BOTTOM_END);
        exportPopover.setAutofocus(true);
        exportPopover.setAriaLabel("Export options");
    }

    private void onExportClicked() {
        if (!isExportAvailable()) {
            updateExportState();
            return;
        }
        if (offersSelectedRowsChoice()) {
            int selectedCount = innerGrid.getSelectedItems().size();
            exportScopeButton.setText(
                    exportSize() == ExportSize.CURRENT_DISPLAY && paginated
                            ? "Export current page"
                            : "Export all rows");
            exportSelectedButton.setText(
                    "Export selected rows (" + selectedCount + ")");
            exportPopover.open();
        } else {
            export(exportConfig);
        }
    }

    private boolean offersSelectedRowsChoice() {
        boolean optionEnabled = exportConfig == null || exportConfig.isSelectedRowsOption();
        return optionEnabled
                && exportSize() != ExportSize.SELECTED
                && innerGrid.getSelectionMode() != Grid.SelectionMode.NONE
                && !innerGrid.getSelectedItems().isEmpty();
    }

    private void export(GridExportConfig<T> config) {
        exportHandler.handleExport(innerGrid, getBaseDataProvider(), config);
    }

    private GridExportConfig<T> selectedRowsConfig() {
        GridExportConfig<T> base =
                exportConfig != null ? exportConfig : GridExportConfig.<T>builder().build();
        return base.toBuilder().size(ExportSize.SELECTED).build();
    }

    private ExportSize exportSize() {
        return exportConfig == null || exportConfig.getSize() == null
                ? ExportSize.CURRENT_DISPLAY
                : exportConfig.getSize();
    }

    private void updateExportState() {
        if (exportButton == null) {
            return;
        }
        boolean available = isExportAvailable();
        exportButton.setEnabled(available);
        if (!available && exportPopover != null && exportPopover.isOpened()) {
            exportPopover.close();
        }
    }

    private boolean hasData() {
        DataProvider<T, ?> base = getBaseDataProvider();
        if (base == null || innerGrid == null) {
            return false;
        }
        if (paginated) {
            return paginator.getTotalItems() > 0;
        }
        Query query = buildQuery(0, Integer.MAX_VALUE);
        return base.size(query) > 0;
    }

    // ------------------------------------------------------ column selection

    /**
     * Shows or removes the column selector button. The user's choice of displayed columns is kept
     * in the {@link #setColumnPreferenceStorage(ColumnPreferenceStorage) column preference storage}
     * (the browser's {@code localStorage} by default) under {@link #getColumnPreferenceStorageKey()}
     * and restored whenever the grid is attached.
     */
    public void enableColumnSelection(boolean enabled) {
        if (enabled) {
            if (columnSelectionButton == null) {
                columnSelector = new ColumnSelector<>(innerGrid, this::saveColumnPreference);

                columnSelectionButton = new Button(VaadinIcon.FILTER.create());
                applyTertiary(columnSelectionButton);
                columnSelectionButton.setAriaLabel("Choose columns");
                columnSelectionButton.setTooltipText("Choose columns");
                columnSelectionButton.getElement().setAttribute("slot", "columns");
                add(columnSelectionButton);

                columnPopover = new Popover(columnSelector);
                columnPopover.setTarget(columnSelectionButton);
                columnPopover.setPosition(PopoverPosition.BOTTOM_END);
                columnPopover.setAutofocus(true);
                columnPopover.setAriaLabel("Choose columns");
                columnPopover.addOpenedChangeListener(
                        e -> {
                            if (e.isOpened()) {
                                columnSelector.refresh();
                            }
                        });
                loadColumnPreference();
            }
        } else if (columnSelectionButton != null) {
            columnPopover.close();
            columnPopover.setTarget(null);
            columnPopover.removeFromParent();
            columnSelectionButton.removeFromParent();
            columnPopover = null;
            columnSelectionButton = null;
            columnSelector = null;
        }
    }

    /**
     * Sets the storage key suffix under which the column preference is stored. Give
     * each grid its own key when a view holds several grids. When not set, the component id is
     * used, or else the current route path.
     */
    public void setColumnPreferenceKey(String key) {
        this.columnPreferenceKey = key == null || key.isBlank() ? null : key.strip();
        loadColumnPreference();
    }

    /** Returns the full storage key, or empty while it cannot be derived yet. */
    public Optional<String> getColumnPreferenceStorageKey() {
        String suffix = columnPreferenceKey;
        if (suffix == null) {
            suffix = getId().orElse(null);
        }
        if (suffix == null) {
            suffix =
                    getUI().map(ui -> ui.getInternals().getActiveViewLocation())
                            .map(location -> location.getPath())
                            .map(path -> path.isEmpty() ? "/" : path)
                            .orElse(null);
        }
        return Optional.ofNullable(suffix).map(s -> COLUMN_PREFERENCE_KEY_PREFIX + s);
    }

    /**
     * Sets where the column preference is stored, e.g. {@link
     * ColumnPreferenceStorage#sessionStorage()} or a {@link
     * org.vaadin.addons.antlerflow.grid.columns.storage.JdbcColumnPreferenceStorage}. The
     * preference is loaded from the new storage right away when the grid is attached.
     */
    public void setColumnPreferenceStorage(ColumnPreferenceStorage storage) {
        this.columnPreferenceStorage = Objects.requireNonNull(storage, "storage must not be null");
        loadColumnPreference();
    }

    /**
     * Turns storing the column preference on (default) or off. Turning it off does not delete an
     * already stored preference; use {@link #clearColumnPreference()} for that.
     */
    public void setColumnPreferencePersisted(boolean persisted) {
        this.columnPreferencePersisted = persisted;
    }

    /** Removes the stored column preference from the storage. */
    public void clearColumnPreference() {
        saveColumnPreference(null);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        loadColumnPreference();
    }

    private void loadColumnPreference() {
        if (columnSelector == null || !columnPreferencePersisted) {
            return;
        }
        Optional<ColumnPreferenceContext> context = columnPreferenceContext();
        if (context.isEmpty()) {
            return; // retried on attach
        }
        ColumnSelector<T> selector = columnSelector;
        columnPreferenceStorage.load(
                context.get(),
                visibility -> {
                    // Ignore late answers for a selector that has since been replaced.
                    if (selector == columnSelector) {
                        selector.applyPreference(visibility);
                    }
                });
    }

    /** Receives the user's column choice; {@code null} means "back to default". */
    private void saveColumnPreference(Map<String, Boolean> visibility) {
        if (!columnPreferencePersisted) {
            return;
        }
        columnPreferenceContext()
                .ifPresent(
                        context -> {
                            if (visibility == null) {
                                columnPreferenceStorage.clear(context);
                            } else {
                                columnPreferenceStorage.save(context, visibility);
                            }
                        });
    }

    private Optional<ColumnPreferenceContext> columnPreferenceContext() {
        if (getUI().isEmpty()) {
            return Optional.empty();
        }
        return getColumnPreferenceStorageKey().map(key -> new ColumnPreferenceContext(key, this));
    }

    // ---------------------------------------------------------------- layout

    public void setPaginationVisibility(boolean visibility) {
        this.paginated = visibility;
        paginator.setVisible(visibility);

        if (!visibility) {
            restoreBaseDataProviderToGrid();
            updateExportState();
        } else {
            resetPagination();
        }
    }

    /** Adds components to the "toolbar" slot. */
    public void addToFilters(Component... components) {
        for (Component c : components) {
            c.getElement().setAttribute("slot", "filter");
            add(c);
        }
    }

    /**
     * Adds components to the toolbar's action area. Buttons get the tertiary theme variant so
     * actions stay quiet next to the grid; remove it with {@code
     * button.removeThemeVariants(ButtonVariant.LUMO_TERTIARY)} when an action must stand out.
     */
    public void addToActions(Component... components) {
        for (Component c : components) {
            c.getElement().setAttribute("slot", "action");
            if (c instanceof Button button) {
                applyTertiary(button);
            }
            add(c);
        }
    }

    /** Adds components to the "footer" slot. */
    public void addToFooter(Component... components) {
        for (Component c : components) {
            c.getElement().setAttribute("slot", "footer");
            add(c);
        }
    }

    /** Lumo and Aura share the theme name "tertiary", so this works with either theme. */
    private static void applyTertiary(Button button) {
        button.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    }

    private void refreshGridData() {
        try {
            DataProvider<T, ?> base = getBaseDataProvider();
            if (base == null || innerGrid == null) {
                return;
            }

            if (!paginated) {
                restoreBaseDataProviderToGrid();
                recalcTotalItems();
                return;
            }

            int offset = paginator.getOffset();
            int limit = paginator.getPageSize();

            Query query = buildQuery(offset, limit);
            DataProvider<T, ?> pageProvider = DataProvider.fromStream(base.fetch(query));

            innerGrid.setDataProvider(pageProvider);
            recalcTotalItems();
        } finally {
            updateExportState();
        }
    }

    private void restoreBaseDataProviderToGrid() {
        DataProvider<T, ?> base = getBaseDataProvider();
        if (base != null) {
            innerGrid.setDataProvider(base);
        }
    }

    private DataProvider<T, ?> getBaseDataProvider() {
        return externalDataProvider != null ? externalDataProvider : originalInnerGridDataProvider;
    }

    private Query<T, Object> buildQuery(int offset, int pageSize) {
        return innerGrid.getDataCommunicator().buildQuery(offset, pageSize);
    }
}
