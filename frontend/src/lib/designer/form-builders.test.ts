import { describe, expect, it } from 'vitest';
import { serializeFormFields, validateFormFields, validateFormGrids } from './form-builders';

describe('designer form/grid helpers', () => {
  it('validates duplicate and malformed form field names', () => {
    expect(
      validateFormFields([
        { name: 'amount', label: 'Amount', type: 'text', options: [] },
        { name: '1bad', label: '', type: 'select', options: [] },
        { name: 'amount', label: 'Again', type: 'text', options: [] }
      ])
    ).toEqual([
      'Field 2: Name must be a valid identifier',
      'Field 2: Label is required',
      'Field 2: Options are required for select type',
      "Field 3: Duplicate name 'amount'"
    ]);
  });

  it('serializes grid config onto the first field only', () => {
    expect(serializeFormFields([{ name: 'a' }, { name: 'b' }], { columns: 3, gap: 12 })).toEqual([
      { name: 'a', _gridConfig: { columns: 3, gap: 12 } },
      { name: 'b' }
    ]);
  });

  it('validates data grids and columns', () => {
    expect(
      validateFormGrids([
        {
          name: 'items',
          label: 'Items',
          columns: [
            { name: 'sku', label: 'SKU', type: 'text', options: [] },
            { name: 'sku', label: '', type: 'select', options: [] }
          ]
        },
        { name: 'items', label: 'Other', columns: [] }
      ])
    ).toEqual([
      "Grid 1, Column 2: Duplicate column name 'sku'",
      'Grid 1, Column 2: Label is required',
      'Grid 1, Column 2: Options are required for select type',
      "Grid 2: Duplicate name 'items'",
      'Grid 2: At least one column is required'
    ]);
  });
});
