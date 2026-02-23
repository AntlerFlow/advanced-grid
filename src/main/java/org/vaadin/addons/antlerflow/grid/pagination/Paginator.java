package org.vaadin.addons.antlerflow.grid.pagination;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.shared.Registration;
import lombok.Getter;

@Tag("af-paginator")
@JsModule("./antlerflow/advanced-grid/pagination/paginator.ts")
public class Paginator extends Component {
    public static final String PAGE_CHANGE_EVENT = "page-change";

    @Synchronize(PAGE_CHANGE_EVENT)
    public int getPage() {
        return getElement().getProperty("page", 1);
    }

    public void setPage(int page) {
        gotoPage(page);
    }

    @Synchronize(PAGE_CHANGE_EVENT)
    public int getPageSize() {
        return getElement().getProperty("pageSize", 25);
    }

    public void setPageSize(int pageSize) {
        getElement().setProperty("pageSize", pageSize);
    }

    public void gotoPage(int page) {
        getElement().callJsFunction("gotoPage", page);
    }

    @Synchronize(PAGE_CHANGE_EVENT)
    public String getPageSizeOptions() {
        return getElement().getProperty("pageSizeOptions");
    }

    public void setPageSizeOptions(String pageSizeOptions) {
        getElement().setProperty("pageSizeOptions", pageSizeOptions);
    }

    @Synchronize(PAGE_CHANGE_EVENT)
    public int getOffset() {
        int page = getPage();       // 1-based
        int pageSize = getPageSize();
        return Math.max(0, (page - 1) * pageSize);
    }

    @Synchronize(PAGE_CHANGE_EVENT)
    public long getTotalItems() {
        return (long) getElement().getProperty("totalItems", 0);
    }

    public void setTotalItems(long totalItems) {
        getElement().setProperty("totalItems", totalItems);
    }

    @Synchronize(PAGE_CHANGE_EVENT)
    public int getTotalPages() {
        return getElement().getProperty("totalPages", 0);
    }

    public void setTotalPages(int totalPages) {
        getElement().setProperty("totalPages", totalPages);
    }

    public void setMaxButtons(int maxButtons) {
        getElement().setProperty("maxButtons", maxButtons);
    }

    public void setHideEdges(boolean hideEdges) {
        getElement().setProperty("hideEdges", hideEdges);
    }

    public void showPageSize(boolean showPageSize) {
        getElement().setProperty("showPageSize", showPageSize);
    }

    public void showSummaryText(boolean showSummaryText) {
        getElement().setProperty("showSummaryText", showSummaryText);
    }

    public void showPageJump(boolean showPageJump) {
        getElement().setProperty("showPageJump", showPageJump);
    }

    public void setEnabled(boolean enabled) {
        getElement().setProperty("disabled", !enabled);
    }

    public Registration addPageChangeListener(ComponentEventListener<PageChangeEvent> listener) {
        return addListener(PageChangeEvent.class, listener);
    }

    @Getter
    @DomEvent(PAGE_CHANGE_EVENT)
    public static class PageChangeEvent extends ComponentEvent<Paginator> {
        private final int page; // 1-based
        private final int pageSize;
        private final int totalPages;
        private final int offset;
        private final int limit;

        public PageChangeEvent(
                Paginator source,
                boolean fromClient,
                @EventData("event.detail.page") int page,
                @EventData("event.detail.pageSize") int pageSize,
                @EventData("event.detail.totalPages") int totalPages,
                @EventData("event.detail.offset") int offset,
                @EventData("event.detail.limit") int limit) {
            super(source, fromClient);
            this.page = page;
            this.pageSize = pageSize;
            this.totalPages = totalPages;
            this.offset = offset;
            this.limit = limit;
        }
    }
}
