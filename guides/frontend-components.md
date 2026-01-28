# GridForm Component

A reusable Svelte 5 component for creating editable data grids with add, edit, and delete functionality.

## Features

- Add, edit, and delete rows
- Inline validation with error messages
- Support for multiple field types (text, number, date, select, textarea)
- Configurable minimum and maximum rows
- Total calculations and derived values
- Responsive table layout

## Usage

### Basic Example

```svelte
<script lang="ts">
	import GridForm, { type GridColumn } from '$lib/components/GridForm.svelte';
	import { rules } from '$lib/utils/validation';

	let gridFormRef: GridForm;
	let gridData = $state<Record<string, unknown>[]>([]);

	const columns: GridColumn[] = [
		{
			name: 'item',
			label: 'Item Name',
			type: 'text',
			placeholder: 'Enter item name',
			validation: [
				rules.required('Item name is required'),
				rules.minLength(3, 'Minimum 3 characters')
			]
		},
		{
			name: 'quantity',
			label: 'Quantity',
			type: 'number',
			min: 1,
			step: 1,
			validation: [
				rules.required('Quantity is required'),
				rules.positive('Must be positive')
			]
		},
		{
			name: 'price',
			label: 'Price',
			type: 'number',
			min: 0.01,
			step: 0.01,
			validation: [
				rules.required('Price is required'),
				rules.positive('Must be positive')
			]
		}
	];

	function handleDataChange(data: Record<string, unknown>[]) {
		gridData = data;
	}

	function handleSubmit() {
		// Validate all rows before submission
		if (gridFormRef && !gridFormRef.validateAll()) {
			alert('Please fix errors in the grid');
			return;
		}

		// Check for unsaved editing rows
		if (gridFormRef && gridFormRef.hasEditingRows()) {
			alert('Please save or cancel all editing rows');
			return;
		}

		// Submit data
		console.log('Submitting:', gridData);
	}
</script>

<form onsubmit={(e) => { e.preventDefault(); handleSubmit(); }}>
	<GridForm
		bind:this={gridFormRef}
		columns={columns}
		label="Items"
		description="Add and manage items in the grid"
		minRows={1}
		maxRows={10}
		onDataChange={handleDataChange}
	/>

	<button type="submit">Submit</button>
</form>
```

## Column Configuration

Each column can have the following properties:

```typescript
interface GridColumn {
  name: string; // Field name (required)
  label: string; // Display label (required)
  type: 'text' | 'number' | 'date' | 'select' | 'textarea'; // Input type (required)
  validation?: ValidationRule[]; // Validation rules (optional)
  options?: string[]; // Options for select type (optional)
  placeholder?: string; // Placeholder text (optional)
  min?: number; // Minimum value for number type (optional)
  max?: number; // Maximum value for number type (optional)
  step?: number; // Step value for number type (optional)
}
```

## Component Props

```typescript
interface Props {
  columns: GridColumn[]; // Column definitions (required)
  label?: string; // Grid label (optional)
  description?: string; // Grid description (optional)
  minRows?: number; // Minimum number of rows (default: 0)
  maxRows?: number; // Maximum number of rows (optional)
  initialData?: Record<string, unknown>[]; // Initial data (optional)
  onDataChange?: (data: Record<string, unknown>[]) => void; // Data change callback (optional)
}
```

## Component Methods

The component exposes the following methods via component reference:

- `getData()`: Returns the current grid data
- `validateAll()`: Validates all rows and returns true if all valid
- `hasEditingRows()`: Returns true if any rows are in editing mode

## Field Types

### Text

```typescript
{
	name: 'description',
	label: 'Description',
	type: 'text',
	placeholder: 'Enter description',
	validation: [rules.required(), rules.minLength(5)]
}
```

### Number

```typescript
{
	name: 'amount',
	label: 'Amount',
	type: 'number',
	min: 0,
	max: 1000,
	step: 0.01,
	validation: [rules.required(), rules.positive()]
}
```

### Date

```typescript
{
	name: 'dueDate',
	label: 'Due Date',
	type: 'date',
	validation: [rules.required()]
}
```

### Select

```typescript
{
	name: 'category',
	label: 'Category',
	type: 'select',
	options: ['Option 1', 'Option 2', 'Option 3'],
	validation: [rules.required()]
}
```

### Textarea

```typescript
{
	name: 'notes',
	label: 'Notes',
	type: 'textarea',
	placeholder: 'Enter notes',
	validation: [rules.minLength(10)]
}
```

## Complete Example (Purchase Line Items)

See `/frontend/src/routes/processes/purchase/+page.svelte` for a complete example of using GridForm with line items, including:

- Multiple column types
- Calculated totals
- Form validation
- Integration with process submission

## Validation

The GridForm component integrates with the application's validation system (`$lib/utils/validation.ts`). Each column can have validation rules applied:

```typescript
import { rules } from '$lib/utils/validation';

const columns: GridColumn[] = [
  {
    name: 'email',
    label: 'Email',
    type: 'text',
    validation: [rules.required('Email is required'), rules.email('Please enter a valid email')]
  }
];
```

Available validation rules:

- `rules.required(message?)`
- `rules.minLength(min, message?)`
- `rules.maxLength(max, message?)`
- `rules.min(value, message?)`
- `rules.max(value, message?)`
- `rules.positive(message?)`
- `rules.email(message?)`
- `rules.dateAfter(getMinDate, message?)`
- `rules.dateBefore(getMaxDate, message?)`

# Notifications Page

A dedicated page for managing system notifications (`/notifications`).

## Features

- Lists all user notifications
- Filtering by type (icon indicators)
- "Mark all as read" functionality
- Optimistic UI updates
- Links to relevant tasks or processes

## Usage

This page is automatically linked from the `NotificationBell` component in the navbar.

# Profile Page

A user settings page (`/profile`) allowing users to manage their personal information.

## Features

- View read-only username and roles
- Update First Name, Last Name, and Email
- Real-time feedback with toast notifications
