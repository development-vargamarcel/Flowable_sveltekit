import { render, screen } from '@testing-library/svelte';
import { describe, it, expect } from 'vitest';
import DynamicForm from '../lib/components/DynamicForm.svelte';
import type { FormField, FormGrid, GridConfig } from '../lib/types';

describe('DynamicForm Header Component', () => {
    it('should render a header field as an h3 element with default styling', () => {
        const fields: FormField[] = [
            {
                id: 'header1',
                name: 'header1',
                label: 'Section Header',
                type: 'header',
                gridColumn: 1,
                gridWidth: 2
            }
        ];

        const grids: FormGrid[] = [];
        const gridConfig: GridConfig = { columns: 2, gap: 4 };

        render(DynamicForm, {
            fields,
            grids,
            gridConfig
        });

        const heading = screen.getByRole('heading', { level: 3, name: 'Section Header' });
        expect(heading).toBeTruthy();
        expect(heading.className).toContain('text-lg font-semibold');
    });

    it('should render a header field with custom cssClass correctly', () => {
        const fields: FormField[] = [
            {
                id: 'header2',
                name: 'header2',
                label: 'Styled Header',
                type: 'header',
                gridColumn: 1,
                gridWidth: 2,
                cssClass: 'custom-class'
            }
        ];

        const grids: FormGrid[] = [];
        const gridConfig: GridConfig = { columns: 2, gap: 4 };

        render(DynamicForm, {
            fields,
            grids,
            gridConfig
        });

        const heading = screen.getByRole('heading', { level: 3, name: 'Styled Header' });
        expect(heading).toBeTruthy();

        // Should not have default classes
        expect(heading.className).toBe('');
    });

    it('should NOT render a standard label for header field', () => {
        const fields: FormField[] = [
            {
                id: 'header3',
                name: 'header3',
                label: 'Header No Label',
                type: 'header',
                gridColumn: 1,
                gridWidth: 2
            }
        ];

        const grids: FormGrid[] = [];
        const gridConfig: GridConfig = { columns: 2, gap: 4 };

        render(DynamicForm, {
            fields,
            grids,
            gridConfig
        });

        // Check that H3 exists
        const heading = screen.getByRole('heading', { level: 3, name: 'Header No Label' });
        expect(heading).toBeTruthy();

        // Check that there is NO label element with text "Header No Label"
        // querying by text might find the H3 content, so we specifically check for 'label' tag
        // or ensure 'getByLabelText' fails.

        // getByLabelText looks for a label associated with a control or a label element containing text.
        // Since there is no control for header (it's just h3), getByLabelText should fail.

        expect(screen.queryByLabelText('Header No Label')).toBeNull();

        // Also manually verify no label tag contains the text
        const labels = document.querySelectorAll('label');
        let found = false;
        labels.forEach(l => {
            if (l.textContent?.includes('Header No Label')) found = true;
        });
        expect(found).toBe(false);
    });
});
