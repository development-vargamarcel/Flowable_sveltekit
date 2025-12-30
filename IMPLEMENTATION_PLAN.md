# Implementation Plan: Conditional Field Settings with Process-Level Definitions

## Implementation Status: COMPLETED

All core features have been implemented. See "Implementation Summary" at the end of this document.

## Problem Statement

Previously:
1. Fields and grids were defined **per task** (duplicated across tasks in the same process)
2. `hiddenExpression`, `readonlyExpression`, and `requiredExpression` were stored but **never evaluated**
3. No UI for defining visibility/readonly conditions
4. No "least access wins" logic for multiple conditions

## Proposed Solution

### Core Concepts

1. **Process-Level Field Library**: Define fields and grids once per process, reference them in tasks
2. **Condition Rules**: Define visibility/readonly rules that apply based on expressions
3. **Least Access Wins**: When multiple rules match, the most restrictive takes effect
4. **Dual Definition**: Conditions can be set in code (expressions) AND through simple UI

### Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                    PROCESS DEFINITION                            │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │               FIELD LIBRARY (defined once)                  ││
│  │  - Field: "customerName" (text, required, label, etc.)     ││
│  │  - Field: "amount" (currency)                              ││
│  │  - Grid: "products" (columns: name, price, qty)            ││
│  └─────────────────────────────────────────────────────────────┘│
│                                                                  │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │              CONDITION RULES (global)                       ││
│  │  Rule 1: IF ${userRole == 'viewer'} THEN all fields READONLY││
│  │  Rule 2: IF ${amount > 10000} THEN "approver" VISIBLE       ││
│  │  Rule 3: IF ${status == 'approved'} THEN form READONLY      ││
│  └─────────────────────────────────────────────────────────────┘│
│                                                                  │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │                    TASKS                                    ││
│  │  Task 1: "Enter Data"                                       ││
│  │    - Uses fields: [customerName, amount]                    ││
│  │    - Uses grids: [products]                                 ││
│  │    - Task-specific overrides (optional)                     ││
│  │                                                             ││
│  │  Task 2: "Review Data"                                      ││
│  │    - Uses fields: [customerName, amount, approver]          ││
│  │    - Task-specific: all fields READONLY                     ││
│  └─────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────┘
```

## Data Model Changes

### 1. New TypeScript Interfaces

```typescript
// Process-level field library
interface ProcessFieldLibrary {
  fields: FormField[];          // All fields available in this process
  grids: FormGrid[];            // All grids available in this process
}

// Condition rule for visibility/readonly
interface FieldConditionRule {
  id: string;
  name: string;                 // Human-readable name
  description?: string;         // Optional description
  condition: string;            // Expression: "${amount > 1000}"
  effect: 'hidden' | 'readonly' | 'visible' | 'editable';
  target: FieldConditionTarget; // What this rule affects
  priority: number;             // Higher priority = evaluated first (but least access wins)
}

interface FieldConditionTarget {
  type: 'all' | 'field' | 'grid' | 'group';  // Target type
  fieldNames?: string[];        // Specific field/grid names (if type is 'field' or 'grid')
  groupName?: string;           // Field group name (if type is 'group')
}

// Task field reference (instead of full field definition)
interface TaskFieldReference {
  fieldName: string;            // Reference to field in library
  overrides?: Partial<FormField>; // Optional task-specific overrides
  taskConditions?: FieldConditionRule[]; // Task-specific conditions
}

// Enhanced form definition
interface ProcessFormDefinition {
  fieldLibrary: ProcessFieldLibrary;
  globalConditions: FieldConditionRule[];  // Process-level conditions
  tasks: {
    [taskId: string]: {
      fieldRefs: TaskFieldReference[];
      gridRefs: string[];
      taskConditions?: FieldConditionRule[];  // Task-level conditions
    };
  };
}
```

### 2. Backend DTO Changes

New DTOs:
- `FieldConditionRuleDTO`
- `ProcessFieldLibraryDTO`
- `TaskFieldReferenceDTO`

## Implementation Steps

### Phase 1: Expression Evaluation Engine (Frontend)

**Files to modify:**
- `frontend/src/lib/utils/expression-evaluator.ts` (new file)
- `frontend/src/lib/components/DynamicForm.svelte`

**What to implement:**
1. Create expression evaluator that safely evaluates conditions
2. Support variables: form values, process variables, user context
3. Support operators: `==`, `!=`, `>`, `<`, `>=`, `<=`, `&&`, `||`, `!`
4. Re-evaluate on form value changes

```typescript
// Example usage
const evaluator = new ExpressionEvaluator({
  formValues: { amount: 5000, status: 'pending' },
  processVariables: { initiator: 'john' },
  userContext: { role: 'manager' }
});

evaluator.evaluate('${amount > 1000 && status == "pending"}'); // true
```

### Phase 2: Process-Level Field Library

**Files to modify:**
- `frontend/src/lib/types/index.ts`
- `frontend/src/routes/processes/designer/+page.svelte`
- `backend/src/main/java/com/demo/bpm/dto/` (new DTOs)
- `backend/src/main/java/com/demo/bpm/service/FormDefinitionService.java`

**What to implement:**
1. Add new section in designer for "Field Library"
2. Store field library in process-level BPMN extension
3. Allow tasks to reference fields from library
4. Support field inheritance with task-specific overrides

### Phase 3: Condition Rules UI

**Files to modify/create:**
- `frontend/src/lib/components/ConditionRuleEditor.svelte` (new)
- `frontend/src/lib/components/ConditionRuleList.svelte` (new)
- `frontend/src/routes/processes/designer/+page.svelte`

**UI Design (Simple):**

```
┌────────────────────────────────────────────────────────────────┐
│ Condition Rules                                          [+ Add]│
├────────────────────────────────────────────────────────────────┤
│ ┌────────────────────────────────────────────────────────────┐ │
│ │ Rule: "High Value Approval"                          [Edit] │ │
│ │ When: amount > 10000                                       │ │
│ │ Then: Show field "approverName"                            │ │
│ └────────────────────────────────────────────────────────────┘ │
│ ┌────────────────────────────────────────────────────────────┐ │
│ │ Rule: "Viewer Mode"                                  [Edit] │ │
│ │ When: userRole == "viewer"                                 │ │
│ │ Then: Make ALL fields Read-Only                            │ │
│ └────────────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────────┘

[Edit Rule Dialog]
┌────────────────────────────────────────────────────────────────┐
│ Edit Condition Rule                                            │
├────────────────────────────────────────────────────────────────┤
│ Name: [High Value Approval____________]                        │
│                                                                │
│ When this condition is true:                                   │
│ ┌──────────────────────────────────────────────────────────┐  │
│ │ Field: [amount ▼]  Operator: [> ▼]  Value: [10000____]   │  │
│ │ [+ Add condition] [AND ▼]                                │  │
│ └──────────────────────────────────────────────────────────┘  │
│                                                                │
│ Or write expression: [${amount > 10000}_______________]        │
│                                                                │
│ Then apply:                                                    │
│ ○ Show   ● Hide   ○ Read-Only   ○ Editable                    │
│                                                                │
│ To fields:                                                     │
│ ○ All fields                                                   │
│ ● Selected fields: [☑ approverName] [☐ amount] [☐ status]     │
│                                                                │
│                              [Cancel] [Save Rule]              │
└────────────────────────────────────────────────────────────────┘
```

### Phase 4: "Least Access Wins" Logic

**Implementation in DynamicForm.svelte:**

```typescript
function computeFieldState(field: FormField, rules: FieldConditionRule[], context: EvaluationContext) {
  let isHidden = field.hidden;
  let isReadonly = field.readonly;

  // Evaluate all matching rules
  for (const rule of rules) {
    if (!ruleAppliesToField(rule, field.name)) continue;

    const conditionMet = evaluator.evaluate(rule.condition, context);
    if (!conditionMet) continue;

    // Apply effect - LEAST ACCESS WINS
    switch (rule.effect) {
      case 'hidden':
        isHidden = true;  // Once hidden, stays hidden
        break;
      case 'readonly':
        isReadonly = true;  // Once readonly, stays readonly
        break;
      case 'visible':
        // Only applies if not already hidden by another rule
        if (!isHidden) isHidden = false;
        break;
      case 'editable':
        // Only applies if not already readonly by another rule
        if (!isReadonly) isReadonly = false;
        break;
    }
  }

  return { isHidden, isReadonly };
}
```

### Phase 5: Backend Support

**Files to modify:**
- `backend/src/main/java/com/demo/bpm/service/FormDefinitionService.java`
- `backend/src/main/java/com/demo/bpm/dto/FormDefinitionDTO.java`

**What to implement:**
1. Parse and store field library from BPMN extensions
2. Parse and store condition rules
3. Merge field references with library definitions
4. Return computed form definition to frontend

## File Changes Summary

| File | Action | Description |
|------|--------|-------------|
| `frontend/src/lib/types/index.ts` | Modify | Add new interfaces for conditions and field library |
| `frontend/src/lib/utils/expression-evaluator.ts` | Create | Safe expression evaluation engine |
| `frontend/src/lib/components/DynamicForm.svelte` | Modify | Add condition evaluation and least-access logic |
| `frontend/src/lib/components/ConditionRuleEditor.svelte` | Create | UI for editing condition rules |
| `frontend/src/lib/components/ConditionRuleList.svelte` | Create | UI for listing/managing rules |
| `frontend/src/lib/components/FieldLibraryPanel.svelte` | Create | UI for process-level field library |
| `frontend/src/routes/processes/designer/+page.svelte` | Modify | Integrate field library and condition rules |
| `backend/.../dto/FieldConditionRuleDTO.java` | Create | Backend DTO for condition rules |
| `backend/.../dto/FormDefinitionDTO.java` | Modify | Add field library and conditions |
| `backend/.../service/FormDefinitionService.java` | Modify | Parse field library and conditions |

## Questions for Clarification

1. **Condition Sources**: Should conditions be evaluable at:
   - Form value changes only (frontend)?
   - Process variable changes (backend + frontend)?
   - Both?

2. **Task-Level Overrides**: Should individual tasks be able to:
   - Add additional conditions beyond global ones?
   - Override/disable global conditions?
   - Both?

3. **Backward Compatibility**: Should the system support both:
   - Old per-task field definitions (for existing processes)?
   - New process-level library approach?

4. **User Context Variables**: What user context should be available in conditions?
   - userRole?
   - userId?
   - userGroups?
   - Custom claims?

5. **Grid-Level Conditions**: Should conditions apply to:
   - Entire grid visibility/readonly?
   - Individual grid rows based on row data?
   - Both?

## Recommended Implementation Order

1. ✅ Expression evaluator (foundational)
2. ✅ Least-access-wins logic in DynamicForm
3. ✅ Simple condition rules UI
4. ✅ Process-level field library
5. ✅ Backend integration
6. ✅ Task field references
7. ✅ Testing and refinement

## Estimated Complexity

- **Expression Evaluator**: Medium (secure parsing is tricky)
- **Condition Rules UI**: Medium (needs good UX)
- **Field Library**: Medium-High (architectural change)
- **Backend Changes**: Low-Medium (mostly DTO and parsing)
- **Least-Access Logic**: Low (straightforward logic)

---

## Implementation Summary

### What Was Built

#### Frontend Components

1. **Expression Evaluator** (`frontend/src/lib/utils/expression-evaluator.ts`)
   - Safe expression evaluation without `eval()`
   - Supports form values, process variables, and user context
   - Operators: `==`, `!=`, `>`, `<`, `>=`, `<=`, `&&`, `||`, `!`
   - Helper functions: `isEmpty()`, `isNotEmpty()`, `hasRole()`, `hasGroup()`, `hasAnyRole()`, `hasAnyGroup()`

2. **Condition State Computer** (`frontend/src/lib/utils/condition-state-computer.ts`)
   - Computes field/grid/column visibility and readonly states
   - Implements "least access wins" logic
   - Evaluates global and task-specific condition rules

3. **ConditionRuleEditor** (`frontend/src/lib/components/ConditionRuleEditor.svelte`)
   - Simple UI builder for creating condition rules
   - Supports both visual builder and raw expression input
   - Target selection: all fields, specific fields, grids, or columns

4. **ConditionRuleList** (`frontend/src/lib/components/ConditionRuleList.svelte`)
   - Lists and manages condition rules
   - Priority reordering with drag controls
   - Enable/disable rules

5. **FieldLibraryPanel** (`frontend/src/lib/components/FieldLibraryPanel.svelte`)
   - Define fields and grids once per process
   - Manages field properties, types, and options
   - Grid column management

6. **Updated DynamicForm** (`frontend/src/lib/components/DynamicForm.svelte`)
   - Accepts condition rules and evaluation context
   - Computes field states on form value changes
   - Hides/shows fields and applies readonly based on conditions

7. **Updated DynamicGrid** (`frontend/src/lib/components/DynamicGrid.svelte`)
   - Per-column visibility and readonly support
   - Respects computed column states from conditions

8. **Updated Process Designer** (`frontend/src/routes/processes/designer/+page.svelte`)
   - "Field Library" button to manage process-level fields
   - "Condition Rules" button to manage global rules
   - Auto-saves to BPMN process element attributes

#### Backend Components

1. **FieldConditionRuleDTO** (`backend/src/main/java/com/demo/bpm/dto/FieldConditionRuleDTO.java`)
   - DTO for condition rules with nested target structure

2. **ProcessFieldLibraryDTO** (`backend/src/main/java/com/demo/bpm/dto/ProcessFieldLibraryDTO.java`)
   - DTO for process-level field library

3. **ProcessFormConfigDTO** (`backend/src/main/java/com/demo/bpm/dto/ProcessFormConfigDTO.java`)
   - Complete process form configuration DTO

4. **Updated FormDefinitionService** (`backend/src/main/java/com/demo/bpm/service/FormDefinitionService.java`)
   - `getProcessFormConfig()` method to retrieve field library and condition rules
   - Parses `flowable:fieldLibrary` and `flowable:conditionRules` from BPMN XML

#### TypeScript Types (`frontend/src/lib/types/index.ts`)

New types added:
- `ConditionEffect`, `ConditionTargetType`, `ConditionTarget`
- `FieldConditionRule`
- `ProcessFieldLibrary`, `TaskFieldReference`, `TaskGridReference`
- `TaskFormConfig`, `ProcessFormDefinition`
- `ComputedFieldState`, `ComputedGridState`
- `RuntimeFormField`, `RuntimeFormGrid`, `RuntimeGridColumn`

### How It Works

1. **Design Time**: In the process designer, users can:
   - Define fields and grids in the "Field Library" panel (once per process)
   - Create condition rules in the "Condition Rules" panel
   - Rules are stored as JSON in BPMN process element attributes

2. **Runtime**: When a form is rendered:
   - The `DynamicForm` component receives condition rules and context
   - `ConditionStateComputer` evaluates all rules against current form values
   - Fields/grids/columns are hidden or made readonly based on computed states
   - "Least access wins": if any rule hides a field, it stays hidden

3. **User Context**: Available in conditions:
   - `user.id`, `user.username`, `user.roles`, `user.groups`
   - Helper functions: `hasRole('admin')`, `hasAnyGroup(['managers', 'admins'])`

4. **Process Variables**: Available in conditions:
   - `process.initiator` and any other process variables
   - Form field values directly: `${amount > 1000}`
