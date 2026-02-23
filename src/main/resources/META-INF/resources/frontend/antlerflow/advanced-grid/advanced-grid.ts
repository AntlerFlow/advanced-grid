import {LitElement, html} from 'lit';
import {advancedGridStyles} from './advanced-grid.styles';
import {property} from "lit/decorators.js";

class AdvancedGridElement extends LitElement {

    @property({type: Boolean}) summaryVisible = true;

    static styles = advancedGridStyles;

    render() {
        return html`
            <div class="container" part="container">
                <div class="toolbar" part="toolbar">
                    <div class="custom-bar" part="custom-bar">
                        <div class="filters" part="filters">
                            <slot name="filter"></slot>
                        </div>
                        <div class="actions" part="actions">
                            <slot name="action"></slot>
                        </div>
                    </div>
                    <slot name="export" part="export"></slot>
                </div>

                <div class="grid-host" part="grid-host">
                    <slot name="grid">
                        <div part="grid-placeholder">No grid provided</div>
                    </slot>
                </div>

                <div class="footer" part="footer">
                    <slot name="paginator"></slot>
                    <slot name="footer"></slot>
                </div>
            </div>
        `;
    }
}

customElements.define('af-advanced-grid', AdvancedGridElement);