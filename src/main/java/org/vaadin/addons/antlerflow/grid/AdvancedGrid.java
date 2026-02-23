package org.vaadin.addons.antlerflow.grid;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.data.provider.*;
import com.vaadin.flow.shared.Registration;
import lombok.Getter;
import lombok.Setter;
import org.vaadin.addons.antlerflow.grid.exporter.GridExportConfig;
import org.vaadin.addons.antlerflow.grid.exporter.GridExportHandler;
import org.vaadin.addons.antlerflow.grid.exporter.xdevsoftware.XDEVSoftwareGridExportHandler;
import org.vaadin.addons.antlerflow.grid.pagination.Paginator;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AdvancedGrid is a component that enhances the functionality of a standard Grid by incorporating
 * built-in pagination, data export capabilities, and a customizable toolbar. It is designed to work
 * with various data providers and supports dynamic setting of data, page sizes, and pagination
 * visibility.
 *
 * @param <T> the type of items contained in the grid
 */
@Tag("af-advanced-grid")
@JsModule("./antlerflow/advanced-grid/advanced-grid.ts")
public class AdvancedGrid<T> extends Component implements HasComponents, HasSize, HasStyle {

    @Getter private Grid<T> innerGrid;
    @Getter private Paginator paginator;
    @Getter @Setter private GridExportConfig<T> exportConfig;

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
    @Getter private Button exportButton;

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

        this.innerGrid = innerGrid;
        this.innerGrid.setSizeFull();
        this.innerGrid.getElement().setAttribute("slot", "grid");

        this.originalInnerGridDataProvider = this.innerGrid.getDataProvider();

        this.innerGrid.addSortListener(e -> resetPagination());
        registerDataProviderListeners(getBaseDataProvider());

        recalcTotalItems();
        refreshGridData();

        add(this.innerGrid);
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

    public void enableExport(boolean exportEnabled) {
        if (exportEnabled) {
            if (exportButton == null) {
                exportButton = new Button(VaadinIcon.DOWNLOAD.create());
                exportButton.getElement().setAttribute("slot", "export");
                exportButton.addClickListener(
                        event ->
                                exportHandler.handleExport(
                                        innerGrid, getBaseDataProvider(), exportConfig));
                add(exportButton);
            }
        } else {
            if (exportButton != null) {
                exportButton.removeFromParent();
                exportButton = null;
            }
        }
    }

    public void setPaginationVisibility(boolean visibility) {
        this.paginated = visibility;
        paginator.setVisible(visibility);

        if (!visibility) {
            restoreBaseDataProviderToGrid();
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

    /** Adds components to the "toolbar" slot. */
    public void addToActions(Component... components) {
        for (Component c : components) {
            c.getElement().setAttribute("slot", "action");
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

    private void refreshGridData() {
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
