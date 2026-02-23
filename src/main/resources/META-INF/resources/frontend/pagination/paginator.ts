import {LitElement, html, nothing} from 'lit';
import {customElement, property} from 'lit/decorators.js';
import {paginatorStyles} from './paginator.styles.js';

type PageChangeDetail = {
    page: number;        // 1-based
    pageSize: number;
    totalPages: number;
    offset: number;      // (page-1) * pageSize
    limit: number;       // pageSize
};

@customElement('af-paginator')
export class Paginator extends LitElement {
    @property({type: Number}) page = 1;
    @property({type: Number, attribute: 'page-size'}) pageSize = 25;

    @property({type: Number, attribute: 'total-items'}) totalItems = 0;
    @property({type: Number, attribute: 'total-pages'}) totalPages = 0;

    @property({type: Number, attribute: 'max-buttons'}) maxButtons = 7;
    @property({type: Boolean, attribute: 'hide-edges'}) hideEdges = false;

    @property({type: Boolean, attribute: 'show-page-size'}) showPageSize = true;
    @property({type: String, attribute: 'page-size-options'}) pageSizeOptions = '10,25,50,100';
    @property({type: String, attribute: 'page-size-label'}) pageSizeLabel = 'Page size';
    @property({type: Boolean, attribute: 'show-summary-text'}) showSummaryText = true;
    @property({type: Boolean, attribute: 'show-page-jump'}) showPageJump = true;

    @property({type: Boolean, reflect: true}) disabled = false;

    static styles = paginatorStyles;

    private readonly numberFormat = new Intl.NumberFormat('en-US', {maximumFractionDigits: 0});

    private _pageSizeOptionsCache = '';
    private _parsedPageSizesCache: number[] = [];

    private normalizePageSize(value: unknown): number {
        const n = Number(value);
        return Number.isFinite(n) && n > 0 ? n : 1;
    }

    private normalizeTotalItems(value: unknown): number {
        const n = Number(value);
        return Number.isFinite(n) && n > 0 ? n : 0;
    }

    private get computedTotalPages(): number {
        const explicit = Number(this.totalPages || 0);
        if (explicit > 0) return Math.max(1, explicit);
        const size = this.normalizePageSize(this.pageSize);
        const items = this.normalizeTotalItems(this.totalItems);
        return Math.max(1, Math.ceil(items / size));
    }

    private clampPage(next: number): number {
        const tp = this.computedTotalPages;
        return Math.min(tp, Math.max(1, next));
    }

    protected willUpdate(): void {
        // Keep the internal state valid even if the caller sets out-of-range values.
        this.page = this.clampPage(this.page);
        this.pageSize = this.normalizePageSize(this.pageSize);
    }

    private emitChange() {
        const totalPages = this.computedTotalPages;
        const page = this.clampPage(this.page);
        const pageSize = this.normalizePageSize(this.pageSize);

        const detail: PageChangeDetail = {
            page,
            pageSize,
            totalPages,
            offset: (page - 1) * pageSize,
            limit: pageSize,
        };

        this.dispatchEvent(new CustomEvent<PageChangeDetail>('page-change', {
            detail,
            bubbles: true,
            composed: true,
        }));
    }

    private get parsedPageSizes(): number[] {
        // Memoize parsing so it’s not re-done on every render.
        if (this.pageSizeOptions === this._pageSizeOptionsCache) return this._parsedPageSizesCache;

        const sizes = (this.pageSizeOptions || '')
            .split(',')
            .map(s => Number(s.trim()))
            .filter(n => Number.isFinite(n) && n > 0);

        this._pageSizeOptionsCache = this.pageSizeOptions;
        this._parsedPageSizesCache = Array.from(new Set(sizes)).sort((a, b) => a - b);
        return this._parsedPageSizesCache;
    }

    private onPageSizeChange = (e: Event) => {
        if (this.disabled) return;

        const select = e.currentTarget as HTMLSelectElement;
        const nextSize = Number(select.value);
        if (!Number.isFinite(nextSize) || nextSize <= 0 || nextSize === this.pageSize) return;

        // Keep the user anchored: compute the old offset using the current page /pageSize
        const oldOffset = (this.clampPage(this.page) - 1) * this.normalizePageSize(this.pageSize);

        this.pageSize = nextSize;

        // Compute a new page from the old offset and new size (willUpdate will clamp)
        this.page = Math.floor(oldOffset / nextSize) + 1;

        this.emitChange();
    };

    private gotoPage(next: number) {
        if (this.disabled) return;
        const clamped = this.clampPage(next);
        if (clamped === this.page) return;
        this.page = clamped;
        this.emitChange();
    }

    private onFirstClick = () => this.gotoPage(1);
    private onPrevClick = () => this.gotoPage(this.page - 1);
    private onNextClick = () => this.gotoPage(this.page + 1);
    private onLastClick = () => this.gotoPage(this.computedTotalPages);

    private onGotoPageClick = (e: Event) => {
        const btn = e.currentTarget as HTMLButtonElement;
        const next = Number(btn.dataset.page);
        if (!Number.isFinite(next)) return;
        this.gotoPage(next);
    };

    private onPageJumpChange = (e: Event) => {
        if (this.disabled) return;
        const input = e.currentTarget as HTMLInputElement;

        const next = Number(input.value);
        if (!Number.isFinite(next)) return;

        this.gotoPage(next);

        // Keep the input visually in sync with the clamped page.
        input.value = String(this.page);
    };

    private onPageJumpKeyDown = (e: KeyboardEvent) => {
        if (e.key !== 'Enter') return;
        this.onPageJumpChange(e);
    };

    private model(): Array<number | '…'> {
        const tp = this.computedTotalPages;
        const current = this.page;
        const max = Math.max(5, this.maxButtons | 0);

        if (tp <= max) return Array.from({length: tp}, (_, i) => i + 1);

        const out: Array<number | '…'> = [];
        const windowSize = max - 2;
        const half = Math.floor(windowSize / 2);

        let start = Math.max(2, current - half);
        let end = Math.min(tp - 1, current + half);

        const actual = end - start + 1;
        if (actual < windowSize) {
            const deficit = windowSize - actual;
            start = Math.max(2, start - deficit);
            const actual2 = end - start + 1;
            if (actual2 < windowSize) end = Math.min(tp - 1, end + (windowSize - actual2));
        }

        out.push(1);
        if (start > 2) out.push('…');
        for (let p = start; p <= end; p++) out.push(p);
        if (end < tp - 1) out.push('…');
        out.push(tp);

        return out;
    }

    public get offset(): number {
        return (this.clampPage(this.page) - 1) * this.normalizePageSize(this.pageSize);
    }

    private get range(): { from: number; to: number; total: number } {
        const total = this.normalizeTotalItems(this.totalItems);
        const size = this.normalizePageSize(this.pageSize);
        const offset = this.offset;

        if (total === 0) return {from: 0, to: 0, total};

        const from = Math.min(total, offset + 1);
        const to = Math.min(total, offset + size);
        return {from, to, total};
    }

    render() {
        const tp = this.computedTotalPages;
        const current = this.page;
        const isFirst = current === 1;
        const isLast = current === tp;

        const {from, to, total} = this.range;

        const sizes = this.parsedPageSizes;
        const showSizeDropdown = this.showPageSize && sizes.length > 0;

        // Ensure the current pageSize appears in the dropdown (even if not in options)
        const effectiveSizes = sizes.includes(this.pageSize)
            ? sizes
            : [...sizes, this.pageSize].sort((a, b) => a - b);

        const showJump = this.showPageJump && tp > 1;

        return html`
            <div aria-label="Paginator" class="paginator-container">
                <div class="page-size" part="page-size">
                    ${showSizeDropdown ? html`
                        <label for="pageSizeSelect" part="page-size-label">${this.pageSizeLabel}</label>
                        <select
                                id="pageSizeSelect"
                                part="page-size-select"
                                .value=${String(this.pageSize)}
                                ?disabled=${this.disabled}
                                @change=${this.onPageSizeChange}
                                aria-label=${this.pageSizeLabel}
                        >
                            ${effectiveSizes.map(s => html`
                                <option value=${String(s)}>${s}</option>`)}
                        </select>
                    ` : nothing}
                </div>

                <div part="page-controls" class="page-controls">
                    ${this.showSummaryText && total > 0 ? html`
                        <span part="summary" class="summary" aria-live="polite">
                            Showing <strong>${from}</strong> - <strong>${to}</strong>
                            of <strong>${this.numberFormat.format(total)}</strong>
                        </span>
                    ` : nothing}

                    <div class="page-buttons" part="page-buttons">
                        ${this.hideEdges ? nothing : html`
                            <button class="page-button" part="page-button"
                                    ?disabled=${this.disabled || isFirst}
                                    @click=${this.onFirstClick}
                                    aria-label="First page"
                            >«
                            </button>
                        `}

                        <button class="page-button" part="page-button"
                                ?disabled=${this.disabled || isFirst}
                                @click=${this.onPrevClick}
                                aria-label="Previous page"
                        >‹
                        </button>

                        ${this.model().map(item => item === '…'
                                ? html`<span class="ellipsis" aria-hidden="true">…</span>`
                                : html`
                                    <button class="page-button" part="page-button"
                                            ?disabled=${this.disabled}
                                            data-page=${String(item)}
                                            aria-label="Page ${item}"
                                            aria-current=${item === current ? 'page' : nothing}
                                            @click=${this.onGotoPageClick}
                                    >
                                        ${item}
                                    </button>
                                `
                        )}

                        <button class="page-button" part="page-button"
                                ?disabled=${this.disabled || isLast}
                                @click=${this.onNextClick}
                                aria-label="Next page"
                        >›
                        </button>

                        ${this.hideEdges ? nothing : html`
                            <button class="page-button" part="page-button"
                                    ?disabled=${this.disabled || isLast}
                                    @click=${this.onLastClick}
                                    aria-label="Last page"
                            >»
                            </button>
                        `}
                    </div>

                    ${showJump ? html`
                        <div class="page-jump" part="page-jump">
                            <label for="pageJumpInput" part="page-jump-label">Page</label>
                            <input
                                    id="pageJumpInput"
                                    part="page-jump-input"
                                    type="number"
                                    inputmode="numeric"
                                    min="1"
                                    .max=${String(tp)}
                                    .value=${String(current)}
                                    ?disabled=${this.disabled}
                                    aria-label="Go to page"
                                    @change=${this.onPageJumpChange}
                                    @keydown=${this.onPageJumpKeyDown}
                            />
                            <span aria-hidden="true">/ ${tp}</span>
                        </div>
                    ` : nothing}
                </div>
            </div>
        `;
    }
}