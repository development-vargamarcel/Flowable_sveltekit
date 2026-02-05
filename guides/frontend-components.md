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
  type: "text" | "number" | "date" | "select" | "textarea"; // Input type (required)
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
import { rules } from "$lib/utils/validation";

const columns: GridColumn[] = [
  {
    name: "email",
    label: "Email",
    type: "text",
    validation: [
      rules.required("Email is required"),
      rules.email("Please enter a valid email"),
    ],
  },
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

# DelegateTaskModal Component

A modal component that facilitates task delegation (reassignment) to other users.

## Features

- Fetches available users from the backend
- Allows selection of a target user
- Handles the delegation API call and updates the UI

## Usage

Import and use `DelegateTaskModal` in task pages:

```svelte
<DelegateTaskModal
  open={showDelegateModal}
  taskId={taskId}
  currentAssignee={currentAssignee}
  onClose={() => (showDelegateModal = false)}
  onSuccess={() => {
    // Refresh or navigate
  }}
/>
```

# Dark Mode

The application supports a light and dark theme.

- **Toggle**: Located in the top navbar.
- **Persistence**: Preference is saved in `localStorage` (`theme` key).
- **System Preference**: Defaults to system preference if no manual selection is made.
- **Implementation**: Uses Tailwind CSS `dark:` variant and CSS variables for specialized components (like shadcn/ui or bits-ui).

# ProcessDiagram Component

A reusable component for visualizing BPMN 2.0 process diagrams using `bpmn-js`.

## Features

- Renders BPMN 2.0 XML
- Highlights active activities (tasks, gateways, etc.)
- Zoom and pan support via `bpmn-js` NavigatedViewer
- Responsive container

## Usage

```svelte
<script lang="ts">
  import ProcessDiagram from '$lib/components/ProcessDiagram.svelte';

  let bpmnXml = '...'; // BPMN 2.0 XML string
  let activeActivityIds = ['Activity_1', 'Activity_2']; // IDs of currently active elements
</script>

<div style="height: 500px;">
  <ProcessDiagram {bpmnXml} {activeActivityIds} />
</div>
```

## Props

- `bpmnXml` (string, required): The BPMN 2.0 XML content.
- `activeActivityIds` (string[]): List of activity IDs to highlight (e.g., current task).
- `height` (string, default: '500px'): Height of the diagram container.

# Comments Component

A component to display and add comments for a task or process.

## Features

- Display list of comments with author and timestamp
- Add new comments
- Optimistic updates
- Loading states

## Usage

```svelte
<script>
  import Comments from '$lib/components/Comments.svelte';

  let taskId = 'task-123';
</script>

<Comments taskId={taskId} />
```

## Props

- `taskId` (string, required): The ID of the task to associate comments with.

# TaskDocuments Component

A component to manage documents associated with a task.

## Features

- List documents with metadata (name, size, author, date)
- Upload new documents
- Delete documents
- File type icons

## Usage

```svelte
<script>
  import TaskDocuments from '$lib/components/TaskDocuments.svelte';

  let taskId = 'task-123';
</script>

<TaskDocuments taskId={taskId} />
```

## Props

- `taskId` (string, required): The ID of the task.
- `readonly` (boolean, default: false): If true, hides upload/delete actions.

# TaskTimeline Component

A component to visualize the audit trail/history of a task.

## Features

- Vertical timeline visualization
- Icons for different event types (created, assigned, commented, etc.)
- Relative time formatting

## Usage

```svelte
<script>
  import TaskTimeline from '$lib/components/TaskTimeline.svelte';

  let taskId = 'task-123';
</script>

<TaskTimeline taskId={taskId} />
```

## Props

- `taskId` (string, required): The ID of the task.

# TaskProperties Component

A component to view and edit task metadata (Priority, Due Date).

## Features

- View assignee, priority, and due date
- Edit priority (dropdown) and due date (date picker)
- Optimistic UI updates

## Usage

```svelte
<script>
  import TaskProperties from '$lib/components/TaskProperties.svelte';

  let task = { ... };

  function handleUpdate(updatedTask) {
    task = updatedTask;
  }
</script>

<TaskProperties {task} onUpdate={handleUpdate} />
```

## Props

- `task` (Task, required): The task object.
- `onUpdate` ((task: Task) => void, required): Callback when task is updated.

# TaskFilters Component

A reusable component for filtering and searching tasks.

## Features

- Text search (debounced)
- Assignee filter
- Priority filter
- **Sort By option (Newest, Priority, Due Date)**
- Clear filters button
- Save filter presets to reuse common searches
- Copy shareable task-filter links to clipboard
- Auto-restore last used filters on page refresh
- Keep URL query params synchronized with active filters

## Usage

```svelte
<script>
  import TaskFilters from '$lib/components/TaskFilters.svelte';

  function handleFilterChange(event) {
    const filters = event.detail;
    // Load tasks with filters
  }
</script>

<TaskFilters on:change={handleFilterChange} />
```

## Events

- `change`: Dispatched when any filter changes. Payload: `{ text: string, assignee: string, priority: string, sortBy: string }`.
- `share`: Dispatched when user clicks **Copy Filter Link** with active filters. Payload: `{ text: string, assignee: string, priority: string, sortBy: string }`.

## Presets & Persistence

- Use the "Save Preset" input to store the current filter combination in `localStorage`.
- Click a preset chip to apply it instantly.
- The last-used filters are stored automatically so reloading the page restores them.

# TaskCard Component

A card component representing a single task in a list.

## Features

- Visual indicators for **Overdue**, **Due Today**, and **Due Soon** tasks.
- Priority badges.
- Selection support for bulk actions.
- Process name and creation date display.

## Usage

```svelte
<TaskCard
  task={task}
  selected={isSelected}
  onSelect={handleSelect}
  onclick={handleClick}
/>
```

# GlobalSearch Component

A navbar-integrated search component for quickly finding tasks.

## Features

- Real-time text search with debounce
- Dropdown result list
- Navigation to task details
- Responsive design (hidden on mobile, visible on desktop)

## Usage

Automatically integrated into the `Navbar` component.

```svelte
<GlobalSearch />
```

# ProcessCompletionTrendWidget

A widget for the dashboard to visualize process completions over time.

## Features

- Bar chart visualization of completed processes.
- Filters for last 7, 14, or 30 days.
- Interactive tooltips showing exact counts and dates.
- Responsive design.

## Usage

```svelte
<script>
  import ProcessCompletionTrendWidget from '$lib/components/ProcessCompletionTrendWidget.svelte';
</script>

<ProcessCompletionTrendWidget />
```
