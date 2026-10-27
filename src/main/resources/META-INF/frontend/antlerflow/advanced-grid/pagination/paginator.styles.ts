import {css} from 'lit';

export const paginatorStyles = css`
    :host {
        --af-paginator-control-height: 2.1rem;
        --af-paginator-control-padding-x: 0.65rem;
        --af-paginator-control-border-color: var(--vaadin-input-field-border-color, rgb(31, 31, 31));
        --af-paginator-control-border: 1px solid var(--af-paginator-control-border-color);
        --af-paginator-control-border-radius: 0.5rem;
        --af-paginator-control-background: var(--lumo-base-color);
        --af-paginator-control-color: var(--lumo-body-text-color);
        --af-paginator-control-hover-border-color: color-mix(
                in srgb,
                var(--lumo-primary-color) 35%,
                var(--vaadin-input-field-border-color)
        );
        --af-paginator-control-focus-ring: 0 0 0 3px color-mix(
                in srgb,
                var(--lumo-primary-color) 30%,
                transparent
        );

        --af-paginator-button-page-border: var(--af-paginator-control-border);
        --af-paginator-button-current-page-background: var(--lumo-primary-color);
        --af-paginator-button-current-page-border: 1px solid var(--af-paginator-button-current-page-background);
        --af-paginator-button-current-page-color: var(--lumo-primary-contrast-color);
        --af-paginator-font-family: var(--lumo-font-family), serif;

        --af-paginator-select-border: var(--af-paginator-control-border);
        --af-paginator-select-border-radius: var(--af-paginator-control-border-radius);
        --af-paginator-select-background: var(--af-paginator-control-background);
        --af-paginator-select-color: var(--af-paginator-control-color);
        --af-paginator-select-focus-ring: var(--af-paginator-control-focus-ring);

        /* Caret (dropdown icon) tuning for native <select> */
        --af-paginator-select-caret-size: 1.25rem;
        --af-paginator-select-caret-gap: 0.65rem; /* space between text and caret */
        --af-paginator-select-caret-padding: 0.25rem;

        /* SVG chevron-down (Vaadin-like) as a background image */
        --af-paginator-select-caret-svg: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='16' height='16' viewBox='0 0 16 16'%3E%3Cpath d='M4.47 6.47a.75.75 0 0 1 1.06 0L8 8.94l2.47-2.47a.75.75 0 1 1 1.06 1.06l-3 3a.75.75 0 0 1-1.06 0l-3-3a.75.75 0 0 1 0-1.06z' fill='%236b7280'/%3E%3C/svg%3E");

        display: inline-block;
        font-family: var(--af-paginator-font-family);
        box-sizing: border-box;
        width: 100%;
    }

    .paginator-container {
        display: flex;
        gap: 0.5rem;
        width: 100%;
        align-items: center;
        flex-wrap: wrap;
        justify-content: space-between;
    }

    .page-size {
        display: inline-flex;
        align-items: center;
        gap: 0.5rem;
        flex-wrap: wrap;
    }

    .page-size select {
        -webkit-appearance: none;
        appearance: none;

        min-height: var(--af-paginator-control-height);
        padding: 0 var(--af-paginator-control-padding-x);
        padding-inline-end: calc(
                var(--af-paginator-control-padding-x) + var(--af-paginator-select-caret-gap) +
                var(--af-paginator-select-caret-size)
        );

        border-radius: var(--af-paginator-select-border-radius);
        border: var(--af-paginator-select-border);
        background: var(--af-paginator-select-background);
        color: var(--af-paginator-select-color);

        font: inherit;
        line-height: 1.2;
        box-sizing: border-box;
        cursor: pointer;

        /* Caret */
        background-image: var(--af-paginator-select-caret-svg);
        background-repeat: no-repeat;
        background-position: right var(--af-paginator-select-caret-padding) center;
        background-size: var(--af-paginator-select-caret-size) var(--af-paginator-select-caret-size);
    }

    .page-size select:hover:not(:disabled) {
        border-color: var(--af-paginator-control-hover-border-color);
    }

    .page-size select:focus-visible {
        outline: none;
        box-shadow: var(--af-paginator-select-focus-ring);
        border-color: var(--lumo-primary-color);
    }

    .page-size select:disabled {
        opacity: 0.55;
        cursor: not-allowed;
    }

    .page-controls {
        display: inline-flex;
        gap: 0.5rem;
        align-items: center;
        flex-wrap: wrap;
    }

    .page-buttons {
        display: inline-flex;
        gap: 0.35rem;
        align-items: center;
        flex-wrap: nowrap;
    }

    button {
        min-height: var(--af-paginator-control-height);
        padding: 0 var(--af-paginator-control-padding-x);
        border-radius: var(--af-paginator-control-border-radius);
        border: var(--af-paginator-button-page-border);
        background: var(--af-paginator-control-background);
        color: var(--af-paginator-control-color);

        font: inherit;
        line-height: 1.2;
        box-sizing: border-box;

        cursor: pointer;
    }

    button[aria-current='page'] {
        background: var(--af-paginator-button-current-page-background);
        border: var(--af-paginator-button-current-page-border);
        font-weight: 600;
        color: var(--af-paginator-button-current-page-color);
    }

    button:hover:not(:disabled):not([aria-current='page']) {
        border-color: var(--af-paginator-control-hover-border-color);
    }

    button:focus-visible {
        outline: none;
        box-shadow: var(--af-paginator-control-focus-ring);
        border-color: var(--lumo-primary-color);
    }

    button:disabled {
        opacity: 0.55;
        cursor: not-allowed;
    }

    .ellipsis {
        padding: 0 0.25rem;
        opacity: 0.7;
        user-select: none;
    }

    .page-jump {
        display: inline-flex;
        gap: 0.35rem;
        align-items: center;
        white-space: nowrap;
    }

    .page-jump input[type='number'] {
        width: 3.75rem;

        min-height: var(--af-paginator-control-height);
        padding: 0 var(--af-paginator-control-padding-x);

        border-radius: var(--af-paginator-control-border-radius);
        border: var(--af-paginator-control-border);
        background: var(--af-paginator-control-background);
        color: var(--af-paginator-control-color);

        font: inherit;
        line-height: 1.2;
        box-sizing: border-box;
    }

    .page-jump input[type='number']:hover:not(:disabled) {
        border-color: var(--af-paginator-control-hover-border-color);
    }

    .page-jump input[type='number']:focus-visible {
        outline: none;
        box-shadow: var(--af-paginator-control-focus-ring);
        border-color: var(--lumo-primary-color);
    }

    .page-jump input[type='number']:disabled {
        opacity: 0.55;
        cursor: not-allowed;
    }
`;