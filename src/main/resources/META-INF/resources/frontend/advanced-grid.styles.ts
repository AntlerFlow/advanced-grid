import {css} from 'lit';

export const advancedGridStyles = css`
    :host {
        display: block;
        width: 100%;
        height: 100%;
        box-sizing: border-box;
    }

    .container {
        display: flex;
        flex-direction: column;
        width: 100%;
        height: 100%;
        min-height: 0;
        gap: 0.25em;
        box-sizing: border-box;
    }

    .toolbar, .custom-bar {
        display: flex;
        width: 100%;
        justify-content: space-between;
        gap: 0.5em;
        align-items: baseline;
    }

    .filters, .actions {
        display: flex;
        gap: 0.5em;
        align-items: baseline;
    }

    .footer {
        flex: 0 0 auto;
        padding-top: 0.25em;
        box-sizing: border-box;
    }

    .grid-host {
        flex: 1 1 auto;
        min-height: 0; /* allows vaadin-grid internal scroller to size correctly */
        box-sizing: border-box;
        display: flex;
    }

    ::slotted([slot="grid"]) {
        flex: 1 1 auto;
        min-height: 0;
        width: 100%;
        height: 100%;
    }
`;