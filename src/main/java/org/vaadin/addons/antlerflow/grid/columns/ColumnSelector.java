package org.vaadin.addons.antlerflow.grid.columns;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.checkbox.CheckboxGroupVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.function.SerializableConsumer;
import lombok.Getter;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Lets the user choose which columns of a grid are displayed. At least one column always stays
 * visible. Every change made by the user is reported to the change listener as the complete column
 * id to visibility map, ready to be persisted.
 *
 * @param <T> the type of items in the grid
 */
public class ColumnSelector<T> extends Composite<Div> {

    @Getter private final CheckboxGroup<Grid.Column<T>> columnCheckboxes = new CheckboxGroup<>();
    @Getter private final Button resetButton = new Button("Reset to default");

    /** Visibility each column had when the selector first saw it (the developer's default). */
    private final Map<String, Boolean> defaultVisibility = new HashMap<>();

    private final SerializableConsumer<Map<String, Boolean>> changeListener;
    private Grid<T> grid;
    private boolean updating;

    public ColumnSelector(
            Grid<T> grid, SerializableConsumer<Map<String, Boolean>> changeListener) {
        this.changeListener = Objects.requireNonNull(changeListener, "changeListener");
        setGrid(grid);

        columnCheckboxes.setLabel("Columns");
        columnCheckboxes.addThemeVariants(CheckboxGroupVariant.LUMO_VERTICAL);
        columnCheckboxes.addValueChangeListener(
                e -> {
                    if (!updating && e.isFromClient()) {
                        applySelection(e.getValue());
                    }
                });
        resetButton.addClickListener(e -> reset());
        resetButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        Div content = getContent();
        content.addClassName("af-column-selector");
        content.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column");
        content.add(columnCheckboxes, resetButton);
    }

    /** Switches the selector to another grid and forgets the previous grid's defaults. */
    public void setGrid(Grid<T> grid) {
        this.grid = Objects.requireNonNull(grid, "grid must not be null");
        defaultVisibility.clear();
        refresh();
    }

    /** Re-reads the grid's columns; call it after columns are added or removed. */
    public void refresh() {
        List<Grid.Column<T>> columns = grid.getColumns();
        for (int i = 0; i < columns.size(); i++) {
            defaultVisibility.putIfAbsent(
                    ColumnPreferences.columnId(columns.get(i), i), columns.get(i).isVisible());
        }

        updating = true;
        try {
            columnCheckboxes.setItemLabelGenerator(
                    column -> ColumnPreferences.label(column, grid.getColumns().indexOf(column)));
            columnCheckboxes.setItems(columns);
            columnCheckboxes.setValue(visibleColumns());
            Set<Grid.Column<T>> visible = visibleColumns();
            // The last visible column cannot be hidden: a grid without columns is unusable.
            columnCheckboxes.setItemEnabledProvider(
                    column -> !(visible.size() == 1 && visible.contains(column)));
        } finally {
            updating = false;
        }
    }

    /**
     * Applies a stored preference. Unknown ids are ignored and columns missing from the
     * preference keep their default visibility. If the preference would hide every column, it is
     * not applied.
     */
    public void applyPreference(Map<String, Boolean> visibility) {
        if (visibility == null || visibility.isEmpty()) {
            return;
        }
        refresh();
        List<Grid.Column<T>> columns = grid.getColumns();
        boolean anyVisible = false;
        for (int i = 0; i < columns.size(); i++) {
            String id = ColumnPreferences.columnId(columns.get(i), i);
            anyVisible |= visibility.getOrDefault(id, columns.get(i).isVisible());
        }
        if (!anyVisible) {
            return;
        }
        for (int i = 0; i < columns.size(); i++) {
            Boolean visible = visibility.get(ColumnPreferences.columnId(columns.get(i), i));
            if (visible != null) {
                columns.get(i).setVisible(visible);
            }
        }
        refresh();
    }

    /** Returns the current column id to visibility map, in column order. */
    public Map<String, Boolean> getVisibility() {
        Map<String, Boolean> visibility = new LinkedHashMap<>();
        List<Grid.Column<T>> columns = grid.getColumns();
        for (int i = 0; i < columns.size(); i++) {
            visibility.put(
                    ColumnPreferences.columnId(columns.get(i), i), columns.get(i).isVisible());
        }
        return visibility;
    }

    /** Restores the default visibility and reports it to the change listener. */
    public void reset() {
        List<Grid.Column<T>> columns = grid.getColumns();
        for (int i = 0; i < columns.size(); i++) {
            Boolean visible = defaultVisibility.get(ColumnPreferences.columnId(columns.get(i), i));
            columns.get(i).setVisible(visible == null || visible);
        }
        refresh();
        changeListener.accept(null);
    }

    private void applySelection(Set<Grid.Column<T>> selected) {
        if (selected.isEmpty()) {
            refresh(); // revert: keep the last column visible
            return;
        }
        grid.getColumns().forEach(column -> column.setVisible(selected.contains(column)));
        refresh();
        changeListener.accept(getVisibility());
    }

    private Set<Grid.Column<T>> visibleColumns() {
        Set<Grid.Column<T>> visible = new LinkedHashSet<>();
        grid.getColumns().stream().filter(Grid.Column::isVisible).forEach(visible::add);
        return visible;
    }
}
