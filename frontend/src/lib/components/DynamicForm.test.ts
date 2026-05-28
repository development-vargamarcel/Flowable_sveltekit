import { render, screen, fireEvent } from '@testing-library/svelte';
import { describe, it, expect, vi } from 'vitest';
import DynamicForm from './DynamicForm.svelte';
import type { FormField, GridConfig } from '../types';

describe('DynamicForm Component', () => {
  const mockFields: FormField[] = [
    {
      id: 'field-first-name',
      name: 'firstName',
      label: 'First Name',
      type: 'text',
      required: true,
      gridRow: 0,
      gridColumn: 0,
      gridWidth: 1
    },
    {
      id: 'field-age',
      name: 'age',
      label: 'Age',
      type: 'number',
      required: false,
      gridRow: 0,
      gridColumn: 1,
      gridWidth: 1
    }
  ];

  const mockGridConfig: GridConfig = {
    columns: 2,
    gap: 16
  };

  it('should render fields correctly', () => {
    render(DynamicForm, {
      fields: mockFields,
      grids: [],
      gridConfig: mockGridConfig
    });

    expect(screen.getByText('First Name')).toBeTruthy();
    expect(screen.getByText('Age')).toBeTruthy();
    expect(screen.getByLabelText(/First Name/i)).toBeTruthy();
  });

  it('should handle value changes', async () => {
    const onValuesChange = vi.fn();
    render(DynamicForm, {
      fields: mockFields,
      grids: [],
      gridConfig: mockGridConfig,
      onValuesChange
    });

    const input = screen.getByLabelText(/First Name/i);
    await fireEvent.input(input, { target: { value: 'John' } });

    expect(onValuesChange).toHaveBeenCalled();
    const call = onValuesChange.mock.calls[0][0];
    expect(call.firstName).toBe('John');
  });

  it('should show error message when validation fails', async () => {
    const { component } = render(DynamicForm, {
      fields: mockFields,
      grids: [],
      gridConfig: mockGridConfig
    });

    // Trigger validation via export function
    const isValid = (component as any).validate();
    expect(isValid).toBe(false);

    // Check if error message for required field appears
    // We need to wait for the DOM to update as Svelte state changes might be async
    const errorMsg = await screen.findByText('First Name is required');
    expect(errorMsg).toBeTruthy();
  });

  it('should render readonly fields', () => {
    render(DynamicForm, {
      fields: mockFields,
      grids: [],
      gridConfig: mockGridConfig,
      readonly: true,
      values: { firstName: 'John' }
    });

    const input = screen.getByLabelText(/First Name/i) as HTMLInputElement;
    expect(input.readOnly).toBe(true);
  });
});
