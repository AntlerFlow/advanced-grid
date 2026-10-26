import {LitElement, html} from 'lit';
import {advancedGridStyles} from './advanced-grid.styles';
import {property, state} from "lit/decorators.js";

const TOOLBAR_SLOTS = ['filter', 'action', 'columns', 'export'];
const FOOTER_SLOTS = ['paginator', 'footer'];

class AdvancedGridElement extends LitElement {

    @property({type: Boolean}) summaryVisible = true;

    /**
     * Sections without visible slotted content are not rendered, so the container's gap does not
     * leave space above or below the grid. Content hidden from the server (setVisible(false) sets
     * the `hidden` attribute) counts as absent.
     */
    @state() private toolbarEmpty = true;
    @state() private footerEmpty = true;

    private readonly childObserver = new MutationObserver(() => this.updateSections());

    static styles = advancedGridStyles;

    connectedCallback() {
        super.connectedCallback();
        this.childObserver.observe(this, {
            childList: true,
            attributes: true,
            attributeFilter: ['hidden', 'slot'],
            subtree: false,
        });
        // Direct children's attributes are not covered by `subtree: false`; observe them too.
        this.observeChildren();
        this.updateSections();
    }

    disconnectedCallback() {
        this.childObserver.disconnect();
        super.disconnectedCallback();
    }

    render() {
        return html`
            <div class="container" part="container">
                <div class="toolbar" part="toolbar" ?hidden=${this.toolbarEmpty}>
                    <div class="custom-bar" part="custom-bar">
                        <div class="filters" part="filters">
                            <slot name="filter" @slotchange=${this.onSlotChange}></slot>
                        </div>
                        <div class="actions" part="actions">
                            <slot name="action" @slotchange=${this.onSlotChange}></slot>
                        </div>
                    </div>
                    <div class="tools" part="tools">
                        <slot name="export" part="export" @slotchange=${this.onSlotChange}></slot>
                        <slot name="columns" part="columns" @slotchange=${this.onSlotChange}></slot>
                    </div>
                </div>

                <div class="grid-host" part="grid-host">
                    <slot name="grid">
                        <div part="grid-placeholder">No grid provided</div>
                    </slot>
                </div>

                <div class="footer" part="footer" ?hidden=${this.footerEmpty}>
                    <slot name="paginator" @slotchange=${this.onSlotChange}></slot>
                    <slot name="footer" @slotchange=${this.onSlotChange}></slot>
                </div>
            </div>
        `;
    }

    private onSlotChange = () => {
        this.observeChildren();
        this.updateSections();
    };

    private observeChildren() {
        for (const child of Array.from(this.children)) {
            this.childObserver.observe(child, {attributes: true, attributeFilter: ['hidden', 'slot']});
        }
    }

    private updateSections() {
        this.toolbarEmpty = !this.hasVisibleContent(TOOLBAR_SLOTS);
        this.footerEmpty = !this.hasVisibleContent(FOOTER_SLOTS);
    }

    private hasVisibleContent(slotNames: string[]): boolean {
        return Array.from(this.children).some(child =>
            slotNames.includes(child.getAttribute('slot') ?? '') && !child.hasAttribute('hidden'));
    }
}

customElements.define('af-advanced-grid', AdvancedGridElement);
