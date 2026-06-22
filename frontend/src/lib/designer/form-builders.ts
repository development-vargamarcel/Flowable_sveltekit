export type DesignerFormField = any;
export type DesignerFormGrid = any;
const identifier = /^[a-zA-Z_][a-zA-Z0-9_]*$/;
export function createFormField(index = 0): DesignerFormField {
  return {
    id: `field_${Date.now()}`,
    name: '',
    label: '',
    type: 'text',
    required: false,
    validation: {},
    options: [],
    placeholder: '',
    defaultValue: '',
    defaultExpression: '',
    tooltip: '',
    readonly: false,
    hidden: false,
    hiddenExpression: '',
    readonlyExpression: '',
    requiredExpression: '',
    calculationExpression: '',
    gridColumn: 1,
    gridRow: index + 1,
    gridWidth: 1,
    cssClass: '',
    onChange: '',
    onBlur: ''
  };
}
export function serializeFormFields(
  fields: DesignerFormField[],
  gridConfig: { columns: number; gap: number }
) {
  return fields.map((field, index) => ({
    ...field,
    ...(index === 0 ? { _gridConfig: gridConfig } : {})
  }));
}
export function validateFormFields(fields: DesignerFormField[]): string[] {
  const errors: string[] = [];
  const names = new Set<string>();
  fields.forEach((field, index) => {
    if (!field.name) errors.push(`Field ${index + 1}: Name is required`);
    else if (!identifier.test(field.name))
      errors.push(`Field ${index + 1}: Name must be a valid identifier`);
    else if (names.has(field.name))
      errors.push(`Field ${index + 1}: Duplicate name '${field.name}'`);
    else names.add(field.name);
    if (!field.label) errors.push(`Field ${index + 1}: Label is required`);
    if (['select', 'multiselect', 'radio'].includes(field.type) && field.options.length === 0)
      errors.push(`Field ${index + 1}: Options are required for ${field.type} type`);
  });
  return errors;
}
export function createGrid(index = 0, gridWidth = 2): DesignerFormGrid {
  return {
    id: `grid_${Date.now()}`,
    name: '',
    label: '',
    description: '',
    minRows: 0,
    maxRows: 0,
    columns: [],
    gridColumn: 1,
    gridRow: index + 1,
    gridWidth,
    cssClass: '',
    visibilityExpression: ''
  };
}
export function createGridColumn(): any {
  return {
    id: `col_${Date.now()}`,
    name: '',
    label: '',
    type: 'text',
    required: false,
    placeholder: '',
    options: [],
    validation: {},
    hiddenExpression: '',
    readonlyExpression: '',
    requiredExpression: '',
    calculationExpression: ''
  };
}
export function validateFormGrids(grids: DesignerFormGrid[]): string[] {
  const errors: string[] = [];
  const names = new Set<string>();
  grids.forEach((grid, gridIndex) => {
    if (!grid.name) errors.push(`Grid ${gridIndex + 1}: Name is required`);
    else if (!identifier.test(grid.name))
      errors.push(`Grid ${gridIndex + 1}: Name must be a valid identifier`);
    else if (names.has(grid.name))
      errors.push(`Grid ${gridIndex + 1}: Duplicate name '${grid.name}'`);
    else names.add(grid.name);
    if (!grid.label) errors.push(`Grid ${gridIndex + 1}: Label is required`);
    if (grid.columns.length === 0)
      errors.push(`Grid ${gridIndex + 1}: At least one column is required`);
    const columnNames = new Set<string>();
    grid.columns.forEach((column: any, colIndex: number) => {
      if (!column.name)
        errors.push(`Grid ${gridIndex + 1}, Column ${colIndex + 1}: Name is required`);
      else if (!identifier.test(column.name))
        errors.push(
          `Grid ${gridIndex + 1}, Column ${colIndex + 1}: Name must be a valid identifier`
        );
      else if (columnNames.has(column.name))
        errors.push(
          `Grid ${gridIndex + 1}, Column ${colIndex + 1}: Duplicate column name '${column.name}'`
        );
      else columnNames.add(column.name);
      if (!column.label)
        errors.push(`Grid ${gridIndex + 1}, Column ${colIndex + 1}: Label is required`);
      if (column.type === 'select' && column.options.length === 0)
        errors.push(
          `Grid ${gridIndex + 1}, Column ${colIndex + 1}: Options are required for select type`
        );
    });
  });
  return errors;
}
