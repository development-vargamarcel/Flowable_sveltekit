import { render, screen, fireEvent } from '@testing-library/svelte';
import { describe, it, expect, vi } from 'vitest';
import DynamicGrid from './DynamicGrid.svelte';
import type { GridColumn } from '../types';

describe('DynamicGrid Component', () => {
  const mockColumns: GridColumn[] = [
    {
      id: 'column-product',
      name: 'product',
      label: 'Product',
      type: 'text',
      required: true
    },
    {
      id: 'column-quantity',
      name: 'quantity',
      label: 'Quantity',
      type: 'number',
      required: false
    }
  ];

  it('should render columns and initial data', () => {
    const initialData = [{ product: 'Laptop', quantity: 1 }];

    render(DynamicGrid, {
      columns: mockColumns,
      initialData
    });

    expect(screen.getByText('Product')).toBeTruthy();
    expect(screen.getByText('Quantity')).toBeTruthy();
    expect(screen.getByText('Laptop')).toBeTruthy();
    expect(screen.getByText('1')).toBeTruthy();
  });

  it('should add a new row when clicking Add Row', async () => {
    render(DynamicGrid, {
      columns: mockColumns,
      initialData: []
    });

    // Use getAllByRole or similar to avoid ambiguity if needed, but let's try finding the button specifically
    const addButton = screen.getByRole('button', { name: /Add Row/i });
    await fireEvent.click(addButton);

    // After clicking add row, it should be in editing mode
    // Let's look for the Save button which appears in editing mode
    expect(screen.getByText('Save')).toBeTruthy();
  });

  it('should show empty state message', () => {
    render(DynamicGrid, {
      columns: mockColumns,
      initialData: []
    });

    expect(screen.getByText(/No rows added yet/i)).toBeTruthy();
  });

  it('should handle row deletion', async () => {
    const initialData = [{ product: 'Laptop', quantity: 1 }];

    render(DynamicGrid, {
      columns: mockColumns,
      initialData
    });

    const deleteButton = screen.getByText('Delete');
    await fireEvent.click(deleteButton);

    expect(screen.queryByText('Laptop')).toBeNull();
  });
});
