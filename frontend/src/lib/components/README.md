# Component Library Documentation

This document provides an overview of the reusable components available in the Flowable BPM Frontend.

## Table of Contents

- [UI Components](#ui-components)
  - [Skeleton](#skeleton)
  - [DashboardSkeleton](#dashboardskeleton)
  - [ConfirmDialog](#confirmdialog)
  - [FormField](#formfield)
  - [FormErrorSummary](#formerrorsummary)
  - [Breadcrumbs](#breadcrumbs)
  - [Modal](#modal)
  - [Toast](#toast)
- [Form Components](#form-components)
  - [DynamicForm](#dynamicform)
  - [DynamicGrid](#dynamicgrid)
- [Display Components](#display-components)
  - [ProcessCard](#processcard)
  - [TaskCard](#taskcard)
  - [EmptyState](#emptystate)
  - [Loading](#loading)

---

## UI Components

### Skeleton

Animated placeholder for loading states. Shows content-aware shapes while data loads.

```svelte
<script>
  import Skeleton from '$lib/components/Skeleton.svelte';
</script>

<!-- Text skeleton (default) -->
<Skeleton />

<!-- Circular skeleton (for avatars) -->
<Skeleton variant="circular" width="40px" height="40px" />

<!-- Card skeleton -->
<Skeleton variant="card" />

<!-- Table row skeleton -->
<Skeleton variant="table-row" />

<!-- Stat card skeleton -->
<Skeleton variant="stat-card" />
```

**Props:**
| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `variant` | `'text' \| 'circular' \| 'rectangular' \| 'card' \| 'stat-card' \| 'table-row'` | `'text'` | Shape variant |
| `width` | `string` | `'100%'` | CSS width |
| `height` | `string` | auto | CSS height |
| `class` | `string` | `''` | Additional CSS classes |

---

### DashboardSkeleton

Dashboard-specific skeleton layout showing the expected structure while loading.

```svelte
<script>
  import DashboardSkeleton from '$lib/components/DashboardSkeleton.svelte';
</script>

{#if loading}
  <DashboardSkeleton />
{:else}
  <!-- Dashboard content -->
{/if}
```

**Props:** None

---

### ConfirmDialog

Modal dialog for confirming destructive or important actions.

```svelte
<script>
  import ConfirmDialog from '$lib/components/ConfirmDialog.svelte';

  let showDialog = false;
  let isDeleting = false;

  async function handleConfirm() {
    isDeleting = true;
    await deleteItem();
    showDialog = false;
    isDeleting = false;
  }
</script>

<button onclick={() => showDialog = true}>Delete</button>

<ConfirmDialog
  open={showDialog}
  title="Delete Item"
  message="Are you sure you want to delete this item? This action cannot be undone."
  confirmText="Delete"
  cancelText="Cancel"
  variant="danger"
  loading={isDeleting}
  onConfirm={handleConfirm}
  onCancel={() => showDialog = false}
/>
```

**Props:**
| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `open` | `boolean` | required | Whether dialog is visible |
| `title` | `string` | required | Dialog title |
| `message` | `string` | required | Confirmation message |
| `confirmText` | `string` | `'Confirm'` | Confirm button text |
| `cancelText` | `string` | `'Cancel'` | Cancel button text |
| `variant` | `'danger' \| 'warning' \| 'info'` | `'danger'` | Visual style |
| `loading` | `boolean` | `false` | Shows loading state |
| `focusConfirm` | `boolean` | `false` | Focus confirm button instead of cancel |
| `onConfirm` | `() => void \| Promise<void>` | required | Confirm callback |
| `onCancel` | `() => void` | required | Cancel callback |

**Accessibility:**

- Focus is automatically moved to the cancel button when opened (safer default)
- Use `focusConfirm={true}` to focus the confirm button instead
- Tab key cycles through buttons (tab trapping)
- Escape key closes the dialog
- Focus is restored to the previously focused element on close
- Uses `role="alertdialog"` and `aria-modal="true"`

---

### FormField

Wrapper component for form inputs with consistent styling and validation feedback.

```svelte
<script>
  import FormField from '$lib/components/FormField.svelte';

  let email = '';
  let error = '';
  let touched = false;
</script>

<FormField
  name="email"
  label="Email Address"
  required={true}
  error={error}
  touched={touched}
  helpText="We'll never share your email"
>
  <input
    type="email"
    bind:value={email}
    onblur={() => touched = true}
    class="w-full px-3 py-2"
  />
</FormField>
```

**Props:**
| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `name` | `string` | required | Field name/id |
| `label` | `string` | required | Field label |
| `required` | `boolean` | `false` | Shows required indicator |
| `error` | `string \| undefined` | `undefined` | Error message |
| `helpText` | `string \| undefined` | `undefined` | Help text below field |
| `tooltip` | `string \| undefined` | `undefined` | Tooltip on hover |
| `touched` | `boolean` | `false` | Whether field was interacted with |
| `showSuccess` | `boolean` | `true` | Show green checkmark when valid |
| `disabled` | `boolean` | `false` | Disabled state |
| `class` | `string` | `''` | Additional CSS classes |
| `children` | `Snippet` | required | Input element(s) |

**Accessibility:**

- Tooltip is keyboard-accessible (focus/blur on help icon)
- Error messages use `aria-live="assertive"` for immediate announcement
- Required fields have screen reader text "(required)"
- Validation icons have `aria-hidden="true"`

---

### FormErrorSummary

Displays a summary of all form validation errors with links to fields.

```svelte
<script>
  import FormErrorSummary from '$lib/components/FormErrorSummary.svelte';

  let errors = {
    email: 'Email is required',
    password: 'Password must be at least 8 characters'
  };
</script>

<FormErrorSummary
  errors={errors}
  title="Please fix the following errors:"
  fieldLabels={{ email: 'Email Address', password: 'Password' }}
/>
```

**Props:**
| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `errors` | `Record<string, string>` | required | Field errors map |
| `title` | `string` | `'Please fix...'` | Summary title |
| `dismissable` | `boolean` | `false` | Can be dismissed |
| `onDismiss` | `() => void` | `undefined` | Dismiss callback |
| `fieldLabels` | `Record<string, string>` | `{}` | Field name to label map |
| `autoFocus` | `boolean` | `false` | Auto-focus first error link |

**Accessibility:**

- Arrow keys (up/down/left/right) navigate between error links
- Home/End keys jump to first/last error
- Enter key on an error link scrolls to and focuses the field
- Uses `role="alert"` and `aria-live="polite"` for screen reader announcements
- Error count is announced to screen readers

---

### Breadcrumbs

Navigation breadcrumbs with context-aware labels.

```svelte
<script>
  import Breadcrumbs from '$lib/components/Breadcrumbs.svelte';
  import { setBreadcrumbLabel } from '$lib/stores/breadcrumbContext.svelte';

  // Set context label for dynamic segment (e.g., task ID)
  setBreadcrumbLabel('abc123', 'Review Invoice #1234');
</script>

<Breadcrumbs loading={isRefreshing} />
```

**Props:**
| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `loading` | `boolean` | `false` | Shows spinner on current page |
| `overrides` | `Record<string, string>` | `{}` | Label overrides for segments |

**Context Functions:**

- `setBreadcrumbLabel(segment, label)` - Set label for a URL segment
- `clearBreadcrumbLabel(segment)` - Remove a label
- `clearAllBreadcrumbLabels()` - Clear all labels

---

### Modal

Generic modal dialog component.

```svelte
<script>
  import Modal from '$lib/components/Modal.svelte';
</script>

<Modal
  open={showModal}
  title="Modal Title"
  onClose={() => showModal = false}
  maxWidth="lg"
>
  <p>Modal content goes here</p>

  {#snippet footer()}
    <button onclick={() => showModal = false}>Close</button>
  {/snippet}
</Modal>
```

**Props:**
| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `open` | `boolean` | required | Whether modal is visible |
| `title` | `string` | required | Modal title |
| `onClose` | `() => void` | required | Close callback |
| `maxWidth` | `'sm' \| 'md' \| 'lg' \| 'xl' \| '2xl' \| '3xl' \| '4xl' \| 'full'` | `'md'` | Maximum width |
| `children` | `Snippet` | required | Modal content |
| `footer` | `Snippet` | optional | Footer content |

---

### Toast

Toast notification component (see notifications store).

```svelte
<script>
  import { addToast } from '$lib/stores/notifications.svelte';

  function showSuccess() {
    addToast({
      type: 'success',
      message: 'Item saved successfully!'
    });
  }
</script>
```

---

## Form Components

### DynamicForm

Renders forms dynamically based on form definitions.

```svelte
<script>
  import DynamicForm from '$lib/components/DynamicForm.svelte';
  import type { FormField, FormGrid, GridConfig } from '$lib/types';

  let formRef: DynamicForm;

  function handleSubmit() {
    if (formRef.validate()) {
      const values = formRef.getValues();
      // Submit values
    }
  }
</script>

<DynamicForm
  bind:this={formRef}
  fields={formDefinition.fields}
  grids={formDefinition.grids}
  gridConfig={formDefinition.gridConfig}
  values={initialValues}
  onValuesChange={(values) => console.log(values)}
/>

<button onclick={handleSubmit}>Submit</button>
```

**Props:**
| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `fields` | `FormField[]` | required | Field definitions |
| `grids` | `FormGrid[]` | required | Grid definitions |
| `gridConfig` | `GridConfig` | required | Layout configuration |
| `values` | `Record<string, unknown>` | `{}` | Initial values |
| `errors` | `Record<string, string>` | `{}` | External errors |
| `readonly` | `boolean` | `false` | Read-only mode |
| `onValuesChange` | `(values) => void` | optional | Values change callback |
| `conditionRules` | `FieldConditionRule[]` | `[]` | Condition rules |
| `processVariables` | `Record<string, unknown>` | `{}` | Process context |
| `userContext` | `UserContext` | optional | User context |
| `task` | `Task` | optional | Task context |

**Methods:**

- `validate(): boolean` - Validates all fields
- `getValues(): Record<string, unknown>` - Gets current values
- `reset()` - Resets form to initial values

---

## Display Components

### EmptyState

Empty state placeholder with optional action.

```svelte
<script>
  import EmptyState from '$lib/components/EmptyState.svelte';
</script>

<EmptyState
  title="No tasks"
  description="You have no pending tasks"
  actionText="View All Tasks"
  actionHref="/tasks"
/>
```

### ProcessCard

Card displaying process instance information.

### TaskCard

Card displaying task information with actions.

### Loading

Full-page or inline loading spinner.

```svelte
<script>
  import Loading from '$lib/components/Loading.svelte';
</script>

<Loading message="Loading data..." />
```

---

## Best Practices

### 1. Use Skeleton Loaders

Always provide skeleton loaders for async content:

```svelte
{#if loading}
  <Skeleton variant="card" />
{:else}
  <ActualContent />
{/if}
```

### 2. Confirm Destructive Actions

Use ConfirmDialog for delete/remove operations:

```svelte
<ConfirmDialog
  variant="danger"
  title="Delete Item"
  message="This cannot be undone."
  ...
/>
```

### 3. Provide Form Validation Feedback

Use FormErrorSummary for multiple errors and FormField for inline validation.

### 4. Set Breadcrumb Context

For pages with dynamic IDs, set meaningful labels:

```svelte
<script>
  import { setBreadcrumbLabel } from '$lib/stores/breadcrumbContext.svelte';
  import { onMount } from 'svelte';

  onMount(() => {
    setBreadcrumbLabel(taskId, task.name);
    return () => clearBreadcrumbLabel(taskId);
  });
</script>
```
