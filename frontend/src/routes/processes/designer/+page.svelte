<script lang="ts">
  import { onMount, onDestroy, tick } from 'svelte';
  import { page } from '$app/stores';
  import { goto } from '$app/navigation';
  import { api } from '$lib/api/client';
  import { processStore } from '$lib/stores/processes.svelte';
  import BpmnModeler from 'bpmn-js/lib/Modeler';
  import 'bpmn-js/dist/assets/diagram-js.css';
  import 'bpmn-js/dist/assets/bpmn-font/css/bpmn-embedded.css';
  // @ts-ignore
  import TokenSimulationModule from 'bpmn-js-token-simulation';
  import 'bpmn-js-token-simulation/assets/css/bpmn-js-token-simulation.css';
  import { flowableModdle } from '$lib/utils/flowable-moddle';
  import {
    demoProcesses,
    getDemoProcessesByCategory,
    type DemoProcess
  } from '$lib/utils/demo-processes';
  import type { ProcessFieldLibrary, FieldConditionRule, FormField, FormGrid } from '$lib/types';
  import FieldLibraryPanel from '$lib/components/FieldLibraryPanel.svelte';
  import ConditionRuleList from '$lib/components/ConditionRuleList.svelte';

  // Core modeler state
  let modelerContainer: HTMLDivElement;
  let modeler: BpmnModeler | null = null;
  let modelerReady = $state(false);

  // Process metadata
  let processName = $state('my-process');
  let processDescription = $state('');
  let documentTypes = $state<any[]>([]);
  let selectedDocumentType = $state('');

  // UI state
  let isDeploying = $state(false);
  let isLoading = $state(false);
  let error = $state('');
  let success = $state('');
  let isEditMode = $state(false);
  let editProcessKey = $state('');
  let loadRetryCount = $state(0);
  const MAX_LOAD_RETRIES = 3;

  // Template selection modal state
  let showTemplateModal = $state(false);
  let selectedTemplate = $state<DemoProcess | null>(null);
  const groupedTemplates = getDemoProcessesByCategory();

  // Properties Panel State
  let selectedElement = $state<any>(null);
  let showPropertiesPanel = $state(true);
  let elementProperties = $state<{
    id: string;
    name: string;
    type: string;
    assignee: string;
    candidateGroups: string;
    candidateUsers: string;
    formKey: string;
    documentation: string;
    scriptFormat: string;
    script: string;
    conditionExpression: string;
    dueDate: string;
    priority: string;
    category: string;
    asyncBefore: boolean;
    asyncAfter: boolean;
    exclusive: boolean;
    skipExpression: string;
    implementation: string;
    expression: string;
    delegateExpression: string;
    resultVariable: string;
    class: string;
    multiInstanceType: string;
    loopCardinality: string;
    collection: string;
    elementVariable: string;

    completionCondition: string;
    documentType: string;
    timeDate: string;
    timeDuration: string;
    timeCycle: string;
  }>({
    id: '',
    name: '',
    type: '',
    assignee: '',
    candidateGroups: '',
    candidateUsers: '',
    formKey: '',
    documentation: '',
    scriptFormat: 'javascript',
    script: '',
    conditionExpression: '',
    dueDate: '',
    priority: '',
    category: '',
    asyncBefore: false,
    asyncAfter: false,
    exclusive: true,
    skipExpression: '',
    implementation: 'class',
    expression: '',
    delegateExpression: '',
    resultVariable: '',
    class: '',
    multiInstanceType: 'none',
    loopCardinality: '',
    collection: '',
    elementVariable: '',

    completionCondition: '',
    documentType: '',
    timeDate: '',
    timeDuration: '',
    timeCycle: ''
  });

  // Form Builder State - Enhanced with grid support
  let showFormBuilder = $state(false);
  let showGridBuilder = $state(false);

  // Grid (table) definitions - for adding multiple data grids to forms
  let formGrids = $state<
    Array<{
      id: string;
      name: string;
      label: string;
      description: string;
      minRows: number;
      maxRows: number;
      columns: Array<{
        id: string;
        name: string;
        label: string;
        type: 'text' | 'number' | 'date' | 'select' | 'textarea';
        required: boolean;
        placeholder: string;
        options: string[];
        min?: number;
        max?: number;
        step?: number;
        validation: {
          minLength?: number;
          maxLength?: number;
          min?: number;
          max?: number;
          pattern?: string;
          patternMessage?: string;
        };
        hiddenExpression?: string;
        readonlyExpression?: string;
        requiredExpression?: string;
        calculationExpression?: string;
      }>;
      gridColumn: number;
      gridRow: number;
      gridWidth: number;
      cssClass: string;
      visibilityExpression?: string;
    }>
  >([]);

  let expandedGridIndex = $state<number | null>(null);
  let expandedGridColumnIndex = $state<number | null>(null);

  let formFields = $state<
    Array<{
      id: string;
      name: string;
      label: string;
      type:
        | 'text'
        | 'number'
        | 'date'
        | 'datetime'
        | 'select'
        | 'multiselect'
        | 'textarea'
        | 'checkbox'
        | 'radio'
        | 'file'
        | 'email'
        | 'phone'
        | 'currency'
        | 'percentage'
        | 'expression';
      required: boolean;
      validation: {
        minLength?: number;
        maxLength?: number;
        min?: number;
        max?: number;
        pattern?: string;
        patternMessage?: string;
        customExpression?: string;
        customMessage?: string;
      };
      options: Array<{ value: string; label: string }>;
      placeholder: string;
      defaultValue: string;
      defaultExpression: string;
      tooltip: string;
      readonly: boolean;
      hidden: boolean;
      hiddenExpression: string;
      readonlyExpression: string;
      requiredExpression: string;
      calculationExpression: string;
      visibilityExpression?: string;
      gridColumn: number;
      gridRow: number;
      gridWidth: number;
      cssClass: string;
      onChange: string;
      onBlur: string;
    }>
  >([]);

  // Grid configuration
  let formGridColumns = $state(2);
  let formGridGap = $state(16);
  let showGridConfig = $state(false);

  // Process-Level Field Library and Condition Rules
  let showFieldLibrary = $state(false);
  let showConditionRules = $state(false);
  let fieldLibrary = $state<ProcessFieldLibrary>({ fields: [], grids: [] });
  let globalConditionRules = $state<FieldConditionRule[]>([]);


  const saveFieldLibrary = () => {
    if (!modeler) return;

    try {
      const elementRegistry = modeler.get('elementRegistry') as any;
      const modeling = modeler.get('modeling') as any;

      // Find the process element
      const processElement = elementRegistry.find(
        (element: any) => element.type === 'bpmn:Process'
      );
      if (!processElement) {
        error = 'Could not find process element';
        return;
      }

      // Convert field library to storable format
      const libraryData = {
        fields: fieldLibrary.fields.map((f: FormField) => ({
          ...f,
          options: f.options || [],
          validation: f.validation || null
        })),
        grids: fieldLibrary.grids.map((g: FormGrid) => ({
          ...g,
          columns: g.columns.map((c: any) => ({
            ...c,
            options: c.options || [],
            validation: c.validation || null
          }))
        }))
      };

      // Store as JSON attribute on process element
      modeling.updateProperties(processElement, {
        'flowable:fieldLibrary': JSON.stringify(libraryData)
      });

      // Update process variables
      fieldLibrary.fields.forEach((field: FormField) => {
        if (field.name && !processVariables.includes(field.name)) {
          processVariables = [...processVariables, field.name];
        }
      });
      fieldLibrary.grids.forEach((grid: FormGrid) => {
        if (grid.name && !processVariables.includes(grid.name)) {
          processVariables = [...processVariables, grid.name];
        }
      });

      success = 'Field library saved successfully';
      setTimeout(() => (success = ''), 3000);
    } catch (err) {
      console.error('Error saving field library:', err);
      error = 'Failed to save field library';
    }
  };

  const saveConditionRules = () => {
    if (!modeler) return;

    try {
      const elementRegistry = modeler.get('elementRegistry') as any;
      const modeling = modeler.get('modeling') as any;

      // Find the process element
      const processElement = elementRegistry.find(
        (element: any) => element.type === 'bpmn:Process'
      );
      if (!processElement) {
        error = 'Could not find process element';
        return;
      }

      // Store as JSON attribute on process element
      modeling.updateProperties(processElement, {
        'flowable:conditionRules': JSON.stringify(globalConditionRules)
      });

      success = 'Condition rules saved successfully';
      setTimeout(() => (success = ''), 3000);
    } catch (err) {
      console.error('Error saving condition rules:', err);
      error = 'Failed to save condition rules';
    }
  };

  // Script Editor State
  let showScriptEditor = $state(false);
  let scriptCode = $state('');
  let scriptFormat = $state('javascript');
  let scriptValidationError = $state('');

  // Expression Builder State
  let showExpressionBuilder = $state(false);
  let expressionTarget = $state('');
  let expressionValue = $state('');

  // Sync state
  let isDocTypeSynced = $state(true);
  let syncCheckLoading = $state(false);

  // Expression Tester State
  let showExpressionTester = $state(false);
  let testContextJson = $state('{\n  "initiator": "user1",\n  "amount": 1000\n}');
  let testExpression = $state('');
  let testResult = $state('');
  let testError = $state('');

  // Validation state
  let validationErrors = $state<string[]>([]);
  let validationWarnings = $state<string[]>([]);

  // Undo/Redo state
  let canUndo = $state(false);
  let canRedo = $state(false);

  function checkHistory() {
    if (!modeler) return;
    const commandStack = modeler.get('commandStack') as any;
    canUndo = commandStack.canUndo();
    canRedo = commandStack.canRedo();
  }

  function handleUndo() {
    if (modeler) {
      (modeler.get('commandStack') as any).undo();
    }
  }

  function handleRedo() {
    if (modeler) {
      (modeler.get('commandStack') as any).redo();
    }
  }

  // Available process variables for expression builder
  let processVariables = $state<string[]>([
    'initiator',
    'amount',
    'description',
    'decision',
    'status'
  ]);

  // Default BPMN template with a simple start -> user task -> end flow
  const defaultBpmn = `<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
                  xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xmlns:flowable="http://flowable.org/bpmn"
                  id="Definitions_1"
                  targetNamespace="http://bpmn.io/schema/bpmn">
  <bpmn:process id="process_1" name="New Process" isExecutable="true">
    <bpmn:startEvent id="startEvent" name="Start">
      <bpmn:outgoing>Flow_1</bpmn:outgoing>
    </bpmn:startEvent>
    <bpmn:userTask id="userTask1" name="Review Task" flowable:assignee="\${initiator}">
      <bpmn:incoming>Flow_1</bpmn:incoming>
      <bpmn:outgoing>Flow_2</bpmn:outgoing>
    </bpmn:userTask>
    <bpmn:endEvent id="endEvent" name="End">
      <bpmn:incoming>Flow_2</bpmn:incoming>
    </bpmn:endEvent>
    <bpmn:sequenceFlow id="Flow_1" sourceRef="startEvent" targetRef="userTask1"/>
    <bpmn:sequenceFlow id="Flow_2" sourceRef="userTask1" targetRef="endEvent"/>
  </bpmn:process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="process_1">
      <bpmndi:BPMNShape id="startEvent_di" bpmnElement="startEvent">
        <dc:Bounds x="152" y="102" width="36" height="36"/>
        <bpmndi:BPMNLabel>
          <dc:Bounds x="158" y="145" width="24" height="14"/>
        </bpmndi:BPMNLabel>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="userTask1_di" bpmnElement="userTask1">
        <dc:Bounds x="240" y="80" width="100" height="80"/>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="endEvent_di" bpmnElement="endEvent">
        <dc:Bounds x="392" y="102" width="36" height="36"/>
        <bpmndi:BPMNLabel>
          <dc:Bounds x="400" y="145" width="20" height="14"/>
        </bpmndi:BPMNLabel>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNEdge id="Flow_1_di" bpmnElement="Flow_1">
        <di:waypoint x="188" y="120"/>
        <di:waypoint x="240" y="120"/>
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_2_di" bpmnElement="Flow_2">
        <di:waypoint x="340" y="120"/>
        <di:waypoint x="392" y="120"/>
      </bpmndi:BPMNEdge>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</bpmn:definitions>`;

  // Helper function to delay execution (for retry logic)
  function delay(ms: number): Promise<void> {
    return new Promise((resolve) => setTimeout(resolve, ms));
  }

  // Retry function with exponential backoff
  async function retryWithBackoff<T>(
    fn: () => Promise<T>,
    maxRetries: number,
    baseDelay: number = 1000
  ): Promise<T> {
    let lastError: Error | null = null;

    for (let i = 0; i < maxRetries; i++) {
      try {
        loadRetryCount = i + 1;
        return await fn();
      } catch (err) {
        lastError = err instanceof Error ? err : new Error(String(err));
        if (i < maxRetries - 1) {
          const delayTime = baseDelay * Math.pow(2, i);
          // console.log(`Retry ${i + 1}/${maxRetries} failed, waiting ${delayTime}ms...`);
          await delay(delayTime);
        }
      }
    }

    throw lastError;
  }

  // Load process definition with retry logic
  async function loadProcessDefinition(processId: string): Promise<{ bpmn: string }> {
    return await retryWithBackoff(
      async () => {
        const response = await api.getProcessBpmn(processId);
        if (!response || !response.bpmn) {
          throw new Error('No BPMN XML returned from server');
        }
        return response;
      },
      MAX_LOAD_RETRIES,
      1000
    );
  }

  // Draft storage key for local storage
  const DRAFT_STORAGE_KEY = 'bpmn-designer-draft';
  let draftSaveTimeout: ReturnType<typeof setTimeout> | null = null;
  let _hasDraft = $state(false); // Prefixed: tracked for potential future UI usage

  interface DraftData {
    processName: string;
    processDescription: string;
    bpmnXml: string;
    savedAt: number;
  }

  // Save draft to local storage (debounced)
  function saveDraft() {
    if (draftSaveTimeout) {
      clearTimeout(draftSaveTimeout);
    }

    draftSaveTimeout = setTimeout(async () => {
      if (!modeler || isEditMode) return; // Don't save drafts for edit mode

      try {
        const result = await modeler.saveXML({ format: true });
        if (result.xml) {
          const draft: DraftData = {
            processName,
            processDescription,
            bpmnXml: result.xml,
            savedAt: Date.now()
          };
          localStorage.setItem(DRAFT_STORAGE_KEY, JSON.stringify(draft));
          _hasDraft = true;
        }
      } catch (err) {
        console.error('Failed to save draft:', err);
      }
    }, 2000); // Debounce: save 2 seconds after last change
  }

  // Load draft from local storage
  function loadDraft(): DraftData | null {
    try {
      const stored = localStorage.getItem(DRAFT_STORAGE_KEY);
      if (stored) {
        const draft = JSON.parse(stored) as DraftData;
        // Only restore drafts less than 24 hours old
        const MAX_DRAFT_AGE = 24 * 60 * 60 * 1000; // 24 hours
        if (Date.now() - draft.savedAt < MAX_DRAFT_AGE) {
          return draft;
        }
        // Draft too old, clear it
        clearDraft();
      }
    } catch (err) {
      console.error('Failed to load draft:', err);
    }
    return null;
  }

  // Clear draft from local storage
  function clearDraft() {
    localStorage.removeItem(DRAFT_STORAGE_KEY);
    _hasDraft = false;
  }

  // Load selected template into the modeler
  async function loadTemplate(template: DemoProcess) {
    if (!modeler) return;

    try {
      await modeler.importXML(template.bpmn);
      processName = template.id;
      processDescription = template.description;
      const canvas = modeler.get('canvas') as any;
      canvas.zoom('fit-viewport');
      showTemplateModal = false;
      selectedTemplate = null;
      success = `Loaded template: ${template.name}`;
      setTimeout(() => (success = ''), 3000);

      // Extract process variables from the loaded diagram
      extractProcessVariables();
    } catch (err) {
      console.error('Error loading template:', err);
      error = 'Failed to load template. Please try again.';
    }
  }

  onMount(async () => {
    // Check for edit mode from URL parameters
    const editParam = $page.url.searchParams.get('edit');
    const processDefId = $page.url.searchParams.get('processDefinitionId');

    // Initialize the BPMN modeler with enhanced configuration
    modeler = new BpmnModeler({
      container: modelerContainer,
      keyboard: {
        bindTo: document
      },
      additionalModules: [TokenSimulationModule],
      moddleExtensions: {
        flowable: flowableModdle
      }
    });

    // Set up selection change handler for properties panel
    const eventBus = modeler.get('eventBus') as any;

    eventBus.on('selection.changed', async (e: any) => {
      const selection = e.newSelection;
      if (selection && selection.length === 1) {
        selectedElement = selection[0];
        await tick(); // Ensure DOM is updated
        loadElementProperties(selection[0]);
      } else if (selection && selection.length === 0) {
        // When clicking on canvas (no selection), keep current element if any
        // This prevents losing selection on accidental clicks
      } else {
        selectedElement = null;
        resetElementProperties();
      }
    });

    // Set up element change handler - reloads properties when element changes
    eventBus.on('element.changed', (e: any) => {
      if (selectedElement && e.element && e.element.id === selectedElement.id) {
        // Only reload if the change wasn't triggered by our own updates
        // This prevents flickering while typing
        if (!isUpdatingProperty) {
          loadElementProperties(e.element);
        }
      }
    });

    // Set up shape added handler to track new elements
    eventBus.on('shape.added', (e: any) => {
      // Auto-select newly added elements
      if (e.element && e.element.type !== 'bpmn:Process' && e.element.type !== 'label') {
        const selection = modeler?.get('selection') as any;
        if (selection) {
          selection.select(e.element);
        }
      }
    });

    // Set up command stack change handler for auto-saving drafts and updating history state
    eventBus.on('commandStack.changed', () => {
      checkHistory();
      if (!isEditMode) {
        saveDraft();
      }
    });

    try {
      if (editParam || processDefId) {
        // Edit mode - load existing process with retry logic
        isEditMode = true;
        isLoading = true;
        editProcessKey = editParam || '';

        const processId = processDefId || editParam;
        if (processId) {
          try {
            const response = await loadProcessDefinition(processId);

            // Validate the BPMN XML before importing
            if (
              !response.bpmn.includes('bpmn:definitions') &&
              !response.bpmn.includes('definitions')
            ) {
              throw new Error('Invalid BPMN XML format');
            }

            await modeler.importXML(response.bpmn);
            processName = editParam || processId;
            success = `Loaded process: ${processName}`;
            setTimeout(() => (success = ''), 3000);

            // Extract process variables from the loaded diagram
            extractProcessVariables();

            // Extract document type from loaded process
            const rootElement = (modeler.get('canvas') as any).getRootElement();
            const businessObject = rootElement.businessObject;
            selectedDocumentType = businessObject.get('flowable:documentType') || '';
          } catch (loadErr) {
            console.error('Error loading existing process:', loadErr);
            const errorMsg = loadErr instanceof Error ? loadErr.message : 'Unknown error';
            error = `Failed to load process after ${loadRetryCount} attempts: ${errorMsg}. Starting with default template.`;

            // Reset for new process
            isEditMode = false;
            editProcessKey = '';
            await modeler.importXML(defaultBpmn);
          }
        }
        isLoading = false;
        loadRetryCount = 0;
      } else {
        // New process mode - check for existing draft
        const existingDraft = loadDraft();
        if (existingDraft) {
          _hasDraft = true;
          processName = existingDraft.processName;
          processDescription = existingDraft.processDescription;
          await modeler.importXML(existingDraft.bpmnXml);
          const savedDate = new Date(existingDraft.savedAt).toLocaleString();
          success = `Draft restored from ${savedDate}`;
          setTimeout(() => (success = ''), 4000);
        } else {
          // No draft - show template selection modal
          await modeler.importXML(defaultBpmn);
          showTemplateModal = true;
        }
      }

      const canvas = modeler.get('canvas') as any;
      canvas.zoom('fit-viewport');

      // Load process-level field library and condition rules
      loadProcessLevelData();

      // Load document types
      try {
        documentTypes = await api.getDocumentTypes();
      } catch (err) {
        console.error('Failed to load document types', err);
      }

      modelerReady = true;
    } catch (err) {
      console.error('Error loading BPMN diagram:', err);
      error = 'Failed to initialize the process modeler. Please refresh the page.';
    }

  });

  // Additional cleanup on component destroy
  onDestroy(() => {
    if (modeler) {
      modeler.destroy();
    }
    if (draftSaveTimeout) {
      clearTimeout(draftSaveTimeout);
    }
  });

  // Flag to prevent recursive updates
  let isUpdatingProperty = false;

  function loadElementProperties(element: any) {
    if (!element || !element.businessObject) {
      resetElementProperties();
      return;
    }

    const businessObject = element.businessObject;

    // Create a new properties object with all values from the business object
    const newProps = {
      id: businessObject.id || '',
      name: businessObject.name || '',
      type: element.type,
      assignee: businessObject.get('flowable:assignee') || '',
      candidateGroups: businessObject.get('flowable:candidateGroups') || '',
      candidateUsers: businessObject.get('flowable:candidateUsers') || '',
      formKey: businessObject.get('flowable:formKey') || '',
      documentation: businessObject.documentation?.[0]?.text || '',
      scriptFormat: businessObject.scriptFormat || 'javascript',
      script: businessObject.script?.body || businessObject.script || '',
      conditionExpression: businessObject.conditionExpression?.body || '',
      dueDate: businessObject.get('flowable:dueDate') || '',
      priority: businessObject.get('flowable:priority') || '',
      category: businessObject.get('flowable:category') || '',
      asyncBefore:
        businessObject.get('flowable:asyncBefore') === 'true' ||
        businessObject.get('flowable:asyncBefore') === true,
      asyncAfter:
        businessObject.get('flowable:asyncAfter') === 'true' ||
        businessObject.get('flowable:asyncAfter') === true,
      exclusive: businessObject.get('flowable:exclusive') !== 'false',
      skipExpression: businessObject.get('flowable:skipExpression') || '',
      implementation: businessObject.get('flowable:class')
        ? 'class'
        : businessObject.get('flowable:delegateExpression')
          ? 'delegateExpression'
          : businessObject.get('flowable:expression')
            ? 'expression'
            : 'class',
      expression: businessObject.get('flowable:expression') || '',
      delegateExpression: businessObject.get('flowable:delegateExpression') || '',
      resultVariable: businessObject.get('flowable:resultVariable') || '',
      class: businessObject.get('flowable:class') || '',
      multiInstanceType: getMultiInstanceType(businessObject),
      loopCardinality: getLoopCardinality(businessObject),
      collection: getCollection(businessObject),
      elementVariable: getElementVariable(businessObject),

      completionCondition: getCompletionCondition(businessObject),

      documentType: businessObject.get('flowable:documentType') || '',
      timeDate: getTimerDefinition(businessObject, 'timeDate'),
      timeDuration: getTimerDefinition(businessObject, 'timeDuration'),
      timeCycle: getTimerDefinition(businessObject, 'timeCycle')
    };

    // Update the state
    elementProperties = newProps;

    // Load form fields if present
    loadFormFields(businessObject);

    // Load script for script tasks
    if (element.type === 'bpmn:ScriptTask') {
      scriptCode = newProps.script;
      scriptFormat = newProps.scriptFormat;
    }
  }

  function getMultiInstanceType(businessObject: any): string {
    const loopCharacteristics = businessObject.loopCharacteristics;
    if (!loopCharacteristics) return 'none';
    if (loopCharacteristics.$type === 'bpmn:MultiInstanceLoopCharacteristics') {
      return loopCharacteristics.isSequential ? 'sequential' : 'parallel';
    }
    return 'none';
  }

  function getLoopCardinality(businessObject: any): string {
    const loopCharacteristics = businessObject.loopCharacteristics;
    if (!loopCharacteristics) return '';
    return loopCharacteristics.loopCardinality?.body || '';
  }

  function getCollection(businessObject: any): string {
    const loopCharacteristics = businessObject.loopCharacteristics;
    if (!loopCharacteristics) return '';
    return loopCharacteristics.get('flowable:collection') || '';
  }

  function getElementVariable(businessObject: any): string {
    const loopCharacteristics = businessObject.loopCharacteristics;
    if (!loopCharacteristics) return '';
    return loopCharacteristics.get('flowable:elementVariable') || '';
  }

  function getCompletionCondition(businessObject: any): string {
    const loopCharacteristics = businessObject.loopCharacteristics;
    if (!loopCharacteristics) return '';
    return loopCharacteristics.completionCondition?.body || '';
  }

  function getTimerDefinition(businessObject: any, type: string): string {
    const eventDefinitions = businessObject.eventDefinitions;
    if (eventDefinitions && eventDefinitions[0] && eventDefinitions[0].$type === 'bpmn:TimerEventDefinition') {
      const timerDef = eventDefinitions[0];
      return timerDef[type]?.body || '';
    }
    return '';
  }

  function loadFormFields(businessObject: any) {
    try {
      const formFieldsJson = businessObject.get('flowable:formFields');
      if (formFieldsJson && typeof formFieldsJson === 'string') {
        const parsed = JSON.parse(formFieldsJson);
        // Ensure all fields have the required structure
        formFields = parsed.map((field: any, index: number) => ({
          id: field.id || `field_${Date.now()}_${index}`,
          name: field.name || '',
          label: field.label || '',
          type: field.type || 'text',
          required: Boolean(field.required),
          validation: field.validation || {},
          options: Array.isArray(field.options)
            ? field.options
            : typeof field.options === 'string' && field.options
              ? field.options.split(',').map((o: string) => ({ value: o.trim(), label: o.trim() }))
              : [],
          placeholder: field.placeholder || '',
          defaultValue: field.defaultValue || '',
          defaultExpression: field.defaultExpression || '',
          tooltip: field.tooltip || '',
          readonly: Boolean(field.readonly),
          hidden: Boolean(field.hidden),
          hiddenExpression: field.hiddenExpression || '',
          readonlyExpression: field.readonlyExpression || '',
          requiredExpression: field.requiredExpression || '',
          calculationExpression: field.calculationExpression || '',
          gridColumn: field.gridColumn || 1,
          gridRow: field.gridRow || index + 1,
          gridWidth: field.gridWidth || 1,
          cssClass: field.cssClass || '',
          onChange: field.onChange || '',
          onBlur: field.onBlur || ''
        }));

        // Load grid configuration if present
        if (parsed.length > 0 && parsed[0]._gridConfig) {
          formGridColumns = parsed[0]._gridConfig.columns || 2;
          formGridGap = parsed[0]._gridConfig.gap || 16;
        }
      } else {
        formFields = [];
      }
    } catch (err) {
      console.error('Error parsing form fields:', err);
      formFields = [];
    }

    // Load form grids (data tables)
    try {
      const formGridsJson = businessObject.get('flowable:formGrids');
      if (formGridsJson && typeof formGridsJson === 'string') {
        const parsed = JSON.parse(formGridsJson);
        formGrids = parsed.map((grid: any, index: number) => ({
          id: grid.id || `grid_${Date.now()}_${index}`,
          name: grid.name || '',
          label: grid.label || '',
          description: grid.description || '',
          minRows: grid.minRows || 0,
          maxRows: grid.maxRows || 0,
          columns: Array.isArray(grid.columns)
            ? grid.columns.map((col: any, colIndex: number) => ({
                id: col.id || `col_${Date.now()}_${colIndex}`,
                name: col.name || '',
                label: col.label || '',
                type: col.type || 'text',
                required: Boolean(col.required),
                placeholder: col.placeholder || '',
                options: Array.isArray(col.options) ? col.options : [],
                min: col.min,
                max: col.max,
                step: col.step,
                validation: col.validation || {},
                hiddenExpression: col.hiddenExpression || '',
                readonlyExpression: col.readonlyExpression || '',
                requiredExpression: col.requiredExpression || '',
                calculationExpression: col.calculationExpression || ''
              }))
            : [],
          gridColumn: grid.gridColumn || 1,
          gridRow: grid.gridRow || index + 1,
          gridWidth: grid.gridWidth || formGridColumns,
          cssClass: grid.cssClass || '',
          visibilityExpression: grid.visibilityExpression || ''
        }));
      } else {
        formGrids = [];
      }
    } catch (err) {
      console.error('Error parsing form grids:', err);
      formGrids = [];
    }
  }

  function resetElementProperties() {
    elementProperties = {
      id: '',
      name: '',
      type: '',
      assignee: '',
      candidateGroups: '',
      candidateUsers: '',
      formKey: '',
      documentation: '',
      scriptFormat: 'javascript',
      script: '',
      conditionExpression: '',
      dueDate: '',
      priority: '',
      category: '',
      asyncBefore: false,
      asyncAfter: false,
      exclusive: true,
      skipExpression: '',
      implementation: 'class',
      expression: '',
      delegateExpression: '',
      resultVariable: '',
      class: '',
      multiInstanceType: 'none',
      loopCardinality: '',
      collection: '',
      elementVariable: '',
      completionCondition: '',
      documentType: '',
      timeDate: '',
      timeDuration: '',
      timeCycle: ''
    };
    formFields = [];
    formGrids = [];
    scriptCode = '';
  }

  async function handleDocumentTypeChange(newDocumentType: string) {
    if (!modeler || !selectedElement) return;

    // Update the property immediately for UI responsiveness
    elementProperties.documentType = newDocumentType;
    updateElementProperty('flowable:documentType', newDocumentType);

    if (!newDocumentType) {
      return;
    }

    try {
      // Find the document type definition
      const docType = documentTypes.find((dt) => dt.key === newDocumentType);
      if (!docType || !docType.schemaJson) return;

      const schema = JSON.parse(docType.schemaJson);

      // Populate fields
      if (schema.fields && Array.isArray(schema.fields)) {
        formFields = schema.fields.map((field: any, index: number) => ({
          id: field.id || `field_${Date.now()}_${index}`,
          name: field.name || '',
          label: field.label || '',
          type: field.type || 'text',
          required: Boolean(field.required),
          validation: field.validation || {},
          options: field.options || [],
          placeholder: field.placeholder || '',
          defaultValue: field.defaultValue || '',
          defaultExpression: field.defaultExpression || '',
          tooltip: field.tooltip || '',
          readonly: Boolean(field.readonly),
          hidden: Boolean(field.hidden),
          hiddenExpression: field.hiddenExpression || '',
          readonlyExpression: field.readonlyExpression || '',
          requiredExpression: field.requiredExpression || '',
          calculationExpression: field.calculationExpression || '',
          gridColumn: field.gridColumn || 1,
          gridRow: field.gridRow || index + 1,
          gridWidth: field.gridWidth || 1,
          cssClass: field.cssClass || '',
          onChange: field.onChange || '',
          onBlur: field.onBlur || ''
        }));

        // Save form fields to BPMN
        const fieldsToSave = formFields.map((field, index) => ({
          ...field,
          ...(index === 0 ? { _gridConfig: { columns: formGridColumns, gap: formGridGap } } : {})
        }));

        updateElementProperty('flowable:formFields', JSON.stringify(fieldsToSave));

        // Update process variables
        formFields.forEach((field) => {
          if (field.name && !processVariables.includes(field.name)) {
            processVariables = [...processVariables, field.name];
          }
        });
      }

      // Populate grids
      if (schema.grids && Array.isArray(schema.grids)) {
        formGrids = schema.grids.map((grid: any, index: number) => ({
          id: grid.id || `grid_${Date.now()}_${index}`,
          name: grid.name || '',
          label: grid.label || '',
          description: grid.description || '',
          minRows: grid.minRows || 0,
          maxRows: grid.maxRows || 0,
          columns: Array.isArray(grid.columns)
            ? grid.columns.map((col: any, colIndex: number) => ({
                id: col.id || `col_${Date.now()}_${colIndex}`,
                name: col.name || '',
                label: col.label || '',
                type: col.type || 'text',
                required: Boolean(col.required),
                placeholder: col.placeholder || '',
                options: Array.isArray(col.options) ? col.options : [],
                min: col.min,
                max: col.max,
                step: col.step,
                validation: col.validation || {},
                hiddenExpression: col.hiddenExpression || '',
                readonlyExpression: col.readonlyExpression || '',
                requiredExpression: col.requiredExpression || '',
                calculationExpression: col.calculationExpression || ''
              }))
            : [],
          gridColumn: grid.gridColumn || 1,
          gridRow: grid.gridRow || index + 1,
          gridWidth: grid.gridWidth || formGridColumns,
          cssClass: grid.cssClass || '',
          visibilityExpression: grid.visibilityExpression || ''
        }));

        // Save form grids to BPMN
        updateElementProperty('flowable:formGrids', JSON.stringify(formGrids));

        // Update process variables
        formGrids.forEach((grid) => {
          if (grid.name && !processVariables.includes(grid.name)) {
            processVariables = [...processVariables, grid.name];
          }
          grid.columns.forEach((column) => {
            const fullName = `${grid.name}_${column.name}`;
            if (column.name && !processVariables.includes(fullName)) {
              processVariables = [...processVariables, fullName];
            }
          });
        });
      }
    } catch (err) {
      console.error('Error applying document type schema:', err);
    }
  }

  async function checkSyncStatus() {
    if (!elementProperties.documentType) {
        isDocTypeSynced = true;
        return;
    }

    syncCheckLoading = true;
    try {
        const docType = await api.getDocumentType(elementProperties.documentType);
        if (!docType || !docType.schemaJson) {
            isDocTypeSynced = true;
            return;
        }

        const schema = JSON.parse(docType.schemaJson);
        const schemaFields = schema.fields || [];
        const schemaGrids = schema.grids || [];

        let synced = true;

        for (const sf of schemaFields) {
            const ff = formFields.find(f => f.name === sf.name);
            if (!ff || ff.type !== sf.type || ff.required !== sf.required) {
                synced = false;
                break;
            }
        }

        if (synced) {
            for (const sg of schemaGrids) {
                const fg = formGrids.find(g => g.name === sg.name);
                if (!fg || fg.columns.length !== sg.columns.length) {
                    synced = false;
                    break;
                }
            }
        }

        isDocTypeSynced = synced;
    } catch (e) {
        console.error('Failed to check sync status', e);
    } finally {
        syncCheckLoading = false;
    }
  }

  $effect(() => {
      if (showPropertiesPanel && selectedElement && elementProperties.documentType) {
          checkSyncStatus();
      }
  });

  async function handleSyncDocumentType() {
    if (!modeler || !selectedElement || !elementProperties.documentType) return;

    const docTypeKey = elementProperties.documentType;
    try {
      const latestDocType = await api.getDocumentType(docTypeKey);
      if (!latestDocType || !latestDocType.schemaJson) return;

      const schema = JSON.parse(latestDocType.schemaJson);

      if (schema.fields && Array.isArray(schema.fields)) {
        const newFields = schema.fields.map((schemaField: any, index: number) => {
          const existingField = formFields.find((f) => f.name === schemaField.name);

          if (existingField) {
            return {
              ...schemaField,
              id: existingField.id,
              gridColumn: existingField.gridColumn,
              gridRow: existingField.gridRow,
              gridWidth: existingField.gridWidth,
              cssClass: existingField.cssClass || schemaField.cssClass || ''
            };
          } else {
            return {
              id: schemaField.id || `field_${Date.now()}_${index}`,
              name: schemaField.name || '',
              label: schemaField.label || '',
              type: schemaField.type || 'text',
              required: Boolean(schemaField.required),
              validation: schemaField.validation || {},
              options: schemaField.options || [],
              placeholder: schemaField.placeholder || '',
              defaultValue: schemaField.defaultValue || '',
              defaultExpression: schemaField.defaultExpression || '',
              tooltip: schemaField.tooltip || '',
              readonly: Boolean(schemaField.readonly),
              hidden: Boolean(schemaField.hidden),
              hiddenExpression: schemaField.hiddenExpression || '',
              readonlyExpression: schemaField.readonlyExpression || '',
              requiredExpression: schemaField.requiredExpression || '',
              calculationExpression: schemaField.calculationExpression || '',
              gridColumn: schemaField.gridColumn || 1,
              gridRow: schemaField.gridRow || index + 1,
              gridWidth: schemaField.gridWidth || 1,
              cssClass: schemaField.cssClass || '',
              onChange: schemaField.onChange || '',
              onBlur: schemaField.onBlur || ''
            };
          }
        });

        const schemaFieldNames = schema.fields.map((f: any) => f.name);
        const preservedManualFields = formFields.filter((f) => !schemaFieldNames.includes(f.name));

        formFields = [...newFields, ...preservedManualFields];

        const fieldsToSave = formFields.map((field, index) => ({
          ...field,
          ...(index === 0 ? { _gridConfig: { columns: formGridColumns, gap: formGridGap } } : {})
        }));

        updateElementProperty('flowable:formFields', JSON.stringify(fieldsToSave));

        formFields.forEach((field) => {
          if (field.name && !processVariables.includes(field.name)) {
            processVariables = [...processVariables, field.name];
          }
        });
      }

      if (schema.grids && Array.isArray(schema.grids)) {
        const newGrids = schema.grids.map((schemaGrid: any, index: number) => {
          const existingGrid = formGrids.find((g) => g.name === schemaGrid.name);

          if (existingGrid) {
            return {
              ...schemaGrid,
              id: existingGrid.id,
              gridColumn: existingGrid.gridColumn,
              gridRow: existingGrid.gridRow,
              gridWidth: existingGrid.gridWidth,
              cssClass: existingGrid.cssClass || schemaGrid.cssClass || ''
            };
          } else {
            return {
              id: schemaGrid.id || `grid_${Date.now()}_${index}`,
              name: schemaGrid.name || '',
              label: schemaGrid.label || '',
              description: schemaGrid.description || '',
              minRows: schemaGrid.minRows || 0,
              maxRows: schemaGrid.maxRows || 0,
              columns: Array.isArray(schemaGrid.columns) ? schemaGrid.columns : [],
              gridColumn: schemaGrid.gridColumn || 1,
              gridRow: schemaGrid.gridRow || index + 1,
              gridWidth: schemaGrid.gridWidth || formGridColumns,
              cssClass: schemaGrid.cssClass || '',
              visibilityExpression: schemaGrid.visibilityExpression || ''
            };
          }
        });

        const schemaGridNames = schema.grids.map((g: any) => g.name);
        const preservedManualGrids = formGrids.filter((g) => !schemaGridNames.includes(g.name));

        formGrids = [...newGrids, ...preservedManualGrids];

        updateElementProperty('flowable:formGrids', JSON.stringify(formGrids));
      }

      success = 'Synchronized with Document Type definition';
      isDocTypeSynced = true;
      setTimeout(() => (success = ''), 2000);
    } catch (err) {
      console.error('Error syncing document type:', err);
      error = 'Failed to sync with document type';
    }
  }

  async function handleSaveAsDocumentType() {
    const name = prompt('Enter a name for the new Document Type:');
    if (!name) return;

    const key = name.toLowerCase().replace(/[^a-z0-9]/g, '_');
    // Simple check to ensure key is valid and not empty
    if (!key) {
      error = 'Invalid name. Key generation failed.';
      return;
    }

    if (!confirm(`Create Document Type "${name}" with key "${key}" from current form fields?`))
      return;

    try {
      const payload = {
        key,
        name,
        description: 'Created from Process Designer',
        schemaJson: JSON.stringify({
          fields: formFields,
          grids: formGrids
        })
      };

      await api.createDocumentType(payload);

      // Refresh list
      documentTypes = await api.getDocumentTypes();

      // Select it
      elementProperties.documentType = key;
      updateElementProperty('flowable:documentType', key);

      success = `Document Type "${name}" created and applied.`;
      setTimeout(() => (success = ''), 2000);
    } catch (err: any) {
      console.error('Failed to create document type', err);
      error = err.message || 'Failed to create document type';
    }
  }

  function updateProcessDocumentType() {
    if (!modeler) return;
    const rootElement = (modeler.get('canvas') as any).getRootElement();
    const modeling = modeler.get('modeling') as any;

    // We need to update the 'flowable:documentType' property on the business object
    modeling.updateProperties(rootElement, {
      'flowable:documentType': selectedDocumentType
    });
  }

  function updateElementProperty(property: string, value: string | boolean) {
    if (!modeler || !selectedElement) return;

    isUpdatingProperty = true;

    try {
      const modeling = modeler.get('modeling') as any;
      const moddle = modeler.get('moddle') as any;
      const businessObject = selectedElement.businessObject;

      // Update local state immediately for responsive UI
      if (property in elementProperties) {
        (elementProperties as any)[property] = value;
      }

      if (property === 'name' || property === 'id') {
        modeling.updateProperties(selectedElement, { [property]: value });
      } else if (property === 'documentation') {
        // Update documentation element
        let documentation = businessObject.documentation;
        if (!value) {
          // Remove documentation if empty
          modeling.updateProperties(selectedElement, { documentation: undefined });
        } else if (!documentation || documentation.length === 0) {
          documentation = [moddle.create('bpmn:Documentation', { text: value as string })];
          modeling.updateProperties(selectedElement, { documentation });
        } else {
          documentation[0].text = value as string;
          modeling.updateProperties(selectedElement, { documentation: [...documentation] });
        }
      } else if (property === 'conditionExpression') {
        // Update sequence flow condition
        if (!value) {
          modeling.updateProperties(selectedElement, { conditionExpression: undefined });
        } else {
          const conditionExpression = moddle.create('bpmn:FormalExpression', {
            body: value as string
          });
          modeling.updateProperties(selectedElement, { conditionExpression });
        }
      } else if (property === 'script') {
        // Update script task script
        modeling.updateProperties(selectedElement, {
          script: value as string,
          scriptFormat: scriptFormat
        });
      } else if (property === 'scriptFormat') {
        modeling.updateProperties(selectedElement, {
          scriptFormat: value as string,
          script: elementProperties.script
        });
      } else if (
        property === 'asyncBefore' ||
        property === 'asyncAfter' ||
        property === 'exclusive'
      ) {
        // Boolean properties
        modeling.updateProperties(selectedElement, {
          [`flowable:${property}`]: value ? 'true' : 'false'
        });
      } else if (property === 'multiInstanceType') {
        updateMultiInstanceType(value as string);
      } else if (
        property === 'loopCardinality' ||
        property === 'collection' ||
        property === 'elementVariable' ||
        property === 'completionCondition'
      ) {
        updateMultiInstanceProperty(property, value as string);
      } else if (property === 'timeDate' || property === 'timeDuration' || property === 'timeCycle') {
        updateTimerDefinition(property, value as string);
      } else if (property.startsWith('flowable:')) {
        // Update Flowable extension attributes
        modeling.updateProperties(selectedElement, { [property]: value });
      } else {
        // Generic property update with flowable prefix
        modeling.updateProperties(selectedElement, { [`flowable:${property}`]: value });
      }
    } finally {
      // Reset the flag after a short delay to allow DOM to update
      setTimeout(() => {
        isUpdatingProperty = false;
      }, 100);
    }
  }

  function updateMultiInstanceType(type: string) {
    if (!modeler || !selectedElement) return;

    const modeling = modeler.get('modeling') as any;
    const moddle = modeler.get('moddle') as any;
    const businessObject = selectedElement.businessObject;

    if (type === 'none') {
      modeling.updateProperties(selectedElement, { loopCharacteristics: undefined });
    } else {
      const loopCharacteristics = moddle.create('bpmn:MultiInstanceLoopCharacteristics', {
        isSequential: type === 'sequential'
      });
      modeling.updateProperties(selectedElement, { loopCharacteristics });
    }
  }

  function updateMultiInstanceProperty(property: string, value: string) {
    if (!modeler || !selectedElement) return;

    const modeling = modeler.get('modeling') as any;
    const moddle = modeler.get('moddle') as any;
    const businessObject = selectedElement.businessObject;
    let loopCharacteristics = businessObject.loopCharacteristics;

    if (!loopCharacteristics) return;

    if (property === 'loopCardinality') {
      if (!value) {
        loopCharacteristics.loopCardinality = undefined;
      } else {
        loopCharacteristics.loopCardinality = moddle.create('bpmn:FormalExpression', {
          body: value
        });
      }
    } else if (property === 'completionCondition') {
      if (!value) {
        loopCharacteristics.completionCondition = undefined;
      } else {
        loopCharacteristics.completionCondition = moddle.create('bpmn:FormalExpression', {
          body: value
        });
      }
    } else if (property === 'collection') {
      loopCharacteristics.set('flowable:collection', value || undefined);
    } else if (property === 'elementVariable') {
      loopCharacteristics.set('flowable:elementVariable', value || undefined);
    }

    modeling.updateProperties(selectedElement, { loopCharacteristics });
  }

  function updateTimerDefinition(property: string, value: string) {
    if (!modeler || !selectedElement) return;

    const modeling = modeler.get('modeling') as any;
    const moddle = modeler.get('moddle') as any;
    const businessObject = selectedElement.businessObject;
    const eventDefinitions = businessObject.eventDefinitions;

    if (!eventDefinitions || !eventDefinitions[0] || eventDefinitions[0].$type !== 'bpmn:TimerEventDefinition') {
      return;
    }

    const timerDef = eventDefinitions[0];
    const newProps: any = {};
    
    if (value) {
      newProps[property] = moddle.create('bpmn:FormalExpression', { body: value });
    } else {
      newProps[property] = undefined;
    }
    
    modeling.updateModdleProperties(selectedElement, timerDef, newProps);
  }

  function extractProcessVariables() {
    if (!modeler) return;

    const elementRegistry = modeler.get('elementRegistry') as any;
    const variables = new Set<string>(['initiator']);

    elementRegistry.forEach((element: any) => {
      const bo = element.businessObject;
      if (!bo) return;

      // Extract from form fields
      const formFieldsJson = bo.get && bo.get('flowable:formFields');
      if (formFieldsJson) {
        try {
          const fields = JSON.parse(formFieldsJson);
          fields.forEach((field: any) => {
            if (field.name) variables.add(field.name);
          });
        } catch (e) {
          // Ignore parsing errors
        }
      }

      // Extract from form grids (data tables)
      const formGridsJson = bo.get && bo.get('flowable:formGrids');
      if (formGridsJson) {
        try {
          const grids = JSON.parse(formGridsJson);
          grids.forEach((grid: any) => {
            if (grid.name) {
              variables.add(grid.name);
              // Also add column names prefixed with grid name
              if (Array.isArray(grid.columns)) {
                grid.columns.forEach((col: any) => {
                  if (col.name) {
                    variables.add(`${grid.name}_${col.name}`);
                  }
                });
              }
            }
          });
        } catch (e) {
          // Ignore parsing errors
        }
      }

      // Extract from scripts
      const script = bo.script;
      if (script) {
        const matches = script.match(/execution\.(?:get|set)Variable\(['"]([^'"]+)['"]/g);
        if (matches) {
          matches.forEach((match: string) => {
            const varMatch = match.match(/\(['"]([^'"]+)['"]/);
            if (varMatch) variables.add(varMatch[1]);
          });
        }
      }
    });

    processVariables = Array.from(variables).sort();
  }

  function addFormField() {
    const newField = {
      id: `field_${Date.now()}`,
      name: '',
      label: '',
      type: 'text' as const,
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
      gridRow: formFields.length + 1,
      gridWidth: 1,
      cssClass: '',
      onChange: '',
      onBlur: ''
    };
    formFields = [...formFields, newField];
  }

  function removeFormField(index: number) {
    formFields = formFields.filter((_, i) => i !== index);
  }

  function duplicateFormField(index: number) {
    const field = formFields[index];
    const newField = {
      ...JSON.parse(JSON.stringify(field)),
      id: `field_${Date.now()}`,
      name: field.name ? `${field.name}_copy` : '',
      gridRow: formFields.length + 1
    };
    formFields = [...formFields, newField];
  }

  function moveFormField(index: number, direction: 'up' | 'down') {
    if (direction === 'up' && index > 0) {
      const newFields = [...formFields];
      [newFields[index - 1], newFields[index]] = [newFields[index], newFields[index - 1]];
      formFields = newFields;
    } else if (direction === 'down' && index < formFields.length - 1) {
      const newFields = [...formFields];
      [newFields[index], newFields[index + 1]] = [newFields[index + 1], newFields[index]];
      formFields = newFields;
    }
  }

  function addFieldOption(fieldIndex: number) {
    const field = formFields[fieldIndex];
    field.options = [...field.options, { value: '', label: '' }];
    formFields = [...formFields];
  }

  function removeFieldOption(fieldIndex: number, optionIndex: number) {
    const field = formFields[fieldIndex];
    field.options = field.options.filter((_, i) => i !== optionIndex);
    formFields = [...formFields];
  }

  // ====== Grid (Data Table) Management Functions ======

  function addGrid() {
    const newGrid = {
      id: `grid_${Date.now()}`,
      name: '',
      label: '',
      description: '',
      minRows: 0,
      maxRows: 0,
      columns: [],
      gridColumn: 1,
      gridRow: formGrids.length + formFields.length + 1,
      gridWidth: formGridColumns,
      cssClass: '',
      visibilityExpression: ''
    };
    formGrids = [...formGrids, newGrid];
    expandedGridIndex = formGrids.length - 1;
  }

  function removeGrid(index: number) {
    formGrids = formGrids.filter((_, i) => i !== index);
    if (expandedGridIndex === index) {
      expandedGridIndex = null;
    }
  }

  function duplicateGrid(index: number) {
    const grid = formGrids[index];
    const newGrid = {
      ...JSON.parse(JSON.stringify(grid)),
      id: `grid_${Date.now()}`,
      name: grid.name ? `${grid.name}_copy` : '',
      gridRow: formGrids.length + formFields.length + 1
    };
    formGrids = [...formGrids, newGrid];
  }

  function moveGrid(index: number, direction: 'up' | 'down') {
    if (direction === 'up' && index > 0) {
      const newGrids = [...formGrids];
      [newGrids[index - 1], newGrids[index]] = [newGrids[index], newGrids[index - 1]];
      formGrids = newGrids;
    } else if (direction === 'down' && index < formGrids.length - 1) {
      const newGrids = [...formGrids];
      [newGrids[index], newGrids[index + 1]] = [newGrids[index + 1], newGrids[index]];
      formGrids = newGrids;
    }
  }

  function addGridColumn(gridIndex: number) {
    const grid = formGrids[gridIndex];
    const newColumn = {
      id: `col_${Date.now()}`,
      name: '',
      label: '',
      type: 'text' as const,
      required: false,
      placeholder: '',
      options: [],
      validation: {},
      hiddenExpression: '',
      readonlyExpression: '',
      requiredExpression: '',
      calculationExpression: ''
    };
    grid.columns = [...grid.columns, newColumn];
    formGrids = [...formGrids];
    expandedGridColumnIndex = grid.columns.length - 1;
  }

  function removeGridColumn(gridIndex: number, columnIndex: number) {
    const grid = formGrids[gridIndex];
    grid.columns = grid.columns.filter((_, i) => i !== columnIndex);
    formGrids = [...formGrids];
    if (expandedGridColumnIndex === columnIndex) {
      expandedGridColumnIndex = null;
    }
  }

  function moveGridColumn(gridIndex: number, columnIndex: number, direction: 'up' | 'down') {
    const grid = formGrids[gridIndex];
    if (direction === 'up' && columnIndex > 0) {
      [grid.columns[columnIndex - 1], grid.columns[columnIndex]] = [
        grid.columns[columnIndex],
        grid.columns[columnIndex - 1]
      ];
      formGrids = [...formGrids];
    } else if (direction === 'down' && columnIndex < grid.columns.length - 1) {
      [grid.columns[columnIndex], grid.columns[columnIndex + 1]] = [
        grid.columns[columnIndex + 1],
        grid.columns[columnIndex]
      ];
      formGrids = [...formGrids];
    }
  }

  function addGridColumnOption(gridIndex: number, columnIndex: number) {
    const grid = formGrids[gridIndex];
    const column = grid.columns[columnIndex];
    column.options = [...column.options, ''];
    formGrids = [...formGrids];
  }

  function removeGridColumnOption(gridIndex: number, columnIndex: number, optionIndex: number) {
    const grid = formGrids[gridIndex];
    const column = grid.columns[columnIndex];
    column.options = column.options.filter((_, i) => i !== optionIndex);
    formGrids = [...formGrids];
  }

  function validateFormGrids(): string[] {
    const errors: string[] = [];
    const names = new Set<string>();

    formGrids.forEach((grid, gridIndex) => {
      if (!grid.name) {
        errors.push(`Grid ${gridIndex + 1}: Name is required`);
      } else if (!/^[a-zA-Z_][a-zA-Z0-9_]*$/.test(grid.name)) {
        errors.push(`Grid ${gridIndex + 1}: Name must be a valid identifier`);
      } else if (names.has(grid.name)) {
        errors.push(`Grid ${gridIndex + 1}: Duplicate name '${grid.name}'`);
      } else {
        names.add(grid.name);
      }

      if (!grid.label) {
        errors.push(`Grid ${gridIndex + 1}: Label is required`);
      }

      if (grid.columns.length === 0) {
        errors.push(`Grid ${gridIndex + 1}: At least one column is required`);
      }

      const columnNames = new Set<string>();
      grid.columns.forEach((column, colIndex) => {
        if (!column.name) {
          errors.push(`Grid ${gridIndex + 1}, Column ${colIndex + 1}: Name is required`);
        } else if (!/^[a-zA-Z_][a-zA-Z0-9_]*$/.test(column.name)) {
          errors.push(
            `Grid ${gridIndex + 1}, Column ${colIndex + 1}: Name must be a valid identifier`
          );
        } else if (columnNames.has(column.name)) {
          errors.push(
            `Grid ${gridIndex + 1}, Column ${colIndex + 1}: Duplicate column name '${column.name}'`
          );
        } else {
          columnNames.add(column.name);
        }

        if (!column.label) {
          errors.push(`Grid ${gridIndex + 1}, Column ${colIndex + 1}: Label is required`);
        }

        if (column.type === 'select' && column.options.length === 0) {
          errors.push(
            `Grid ${gridIndex + 1}, Column ${colIndex + 1}: Options are required for select type`
          );
        }
      });
    });

    return errors;
  }

  function saveFormGrids() {
    if (!modeler || !selectedElement) return;

    // Validate grids
    const errors = validateFormGrids();
    if (errors.length > 0) {
      error = `Grid validation errors: ${errors.join(', ')}`;
      setTimeout(() => (error = ''), 5000);
      return;
    }

    const modeling = modeler.get('modeling') as any;
    const formGridsJson = JSON.stringify(formGrids);

    // Store form grids as a custom attribute
    modeling.updateProperties(selectedElement, {
      'flowable:formGrids': formGridsJson
    });

    // Update process variables with new grid names and column names
    formGrids.forEach((grid) => {
      if (grid.name && !processVariables.includes(grid.name)) {
        processVariables = [...processVariables, grid.name];
      }
      grid.columns.forEach((column) => {
        const fullName = `${grid.name}_${column.name}`;
        if (column.name && !processVariables.includes(fullName)) {
          processVariables = [...processVariables, fullName];
        }
      });
    });

    success = 'Data grids saved successfully';
    setTimeout(() => (success = ''), 3000);
    showGridBuilder = false;
  }

  // Grid column type options
  const gridColumnTypeOptions = [
    { value: 'text', label: 'Text' },
    { value: 'number', label: 'Number' },
    { value: 'date', label: 'Date' },
    { value: 'select', label: 'Dropdown' },
    { value: 'textarea', label: 'Text Area' }
  ];

  function saveFormFields() {
    if (!modeler || !selectedElement) return;

    // Validate form fields
    const errors = validateFormFields();
    if (errors.length > 0) {
      error = `Form validation errors: ${errors.join(', ')}`;
      setTimeout(() => (error = ''), 5000);
      return;
    }

    const modeling = modeler.get('modeling') as any;

    // Include grid configuration in the saved data
    const fieldsToSave = formFields.map((field, index) => ({
      ...field,
      ...(index === 0 ? { _gridConfig: { columns: formGridColumns, gap: formGridGap } } : {})
    }));

    const formFieldsJson = JSON.stringify(fieldsToSave);

    // Store form fields as a custom attribute
    modeling.updateProperties(selectedElement, {
      'flowable:formFields': formFieldsJson
    });

    // Update process variables with new field names
    formFields.forEach((field) => {
      if (field.name && !processVariables.includes(field.name)) {
        processVariables = [...processVariables, field.name];
      }
    });

    success = 'Form fields saved successfully';
    setTimeout(() => (success = ''), 3000);
    showFormBuilder = false;
  }



  // Load field library and condition rules from process element
  function loadProcessLevelData() {
    if (!modeler) return;

    try {
      const elementRegistry = modeler.get('elementRegistry') as any;
      const processElement = elementRegistry.find(
        (element: any) => element.type === 'bpmn:Process'
      );

      if (!processElement) return;

      const businessObject = processElement.businessObject;

      // Load field library
      const fieldLibraryJson = businessObject.get && businessObject.get('flowable:fieldLibrary');
      if (fieldLibraryJson && typeof fieldLibraryJson === 'string') {
        try {
          const parsed = JSON.parse(fieldLibraryJson);
          fieldLibrary = {
            fields: (parsed.fields || []).map((f: any) => ({
              ...f,
              options: f.options || [],
              validation: f.validation || null
            })),
            grids: (parsed.grids || []).map((g: any) => ({
              ...g,
              columns: (g.columns || []).map((c: any) => ({
                ...c,
                options: c.options || [],
                validation: c.validation || null
              }))
            }))
          };
        } catch (e) {
          console.error('Error parsing field library:', e);
          fieldLibrary = { fields: [], grids: [] };
        }
      }

      // Load condition rules
      const conditionRulesJson =
        businessObject.get && businessObject.get('flowable:conditionRules');
      if (conditionRulesJson && typeof conditionRulesJson === 'string') {
        try {
          globalConditionRules = JSON.parse(conditionRulesJson);
        } catch (e) {
          console.error('Error parsing condition rules:', e);
          globalConditionRules = [];
        }
      }
    } catch (err) {
      console.error('Error loading process-level data:', err);
    }
  }

  // Handle field library changes from the panel
  function handleFieldLibraryChange(newLibrary: ProcessFieldLibrary) {
    fieldLibrary = newLibrary;
    saveFieldLibrary();
  }

  // Handle condition rules changes from the list
  function handleConditionRulesChange(newRules: FieldConditionRule[]) {
    globalConditionRules = newRules;
    saveConditionRules();
  }



  function validateFormFields(): string[] {
    const errors: string[] = [];
    const names = new Set<string>();

    formFields.forEach((field, index) => {
      if (!field.name) {
        errors.push(`Field ${index + 1}: Name is required`);
      } else if (!/^[a-zA-Z_][a-zA-Z0-9_]*$/.test(field.name)) {
        errors.push(`Field ${index + 1}: Name must be a valid identifier`);
      } else if (names.has(field.name)) {
        errors.push(`Field ${index + 1}: Duplicate name '${field.name}'`);
      } else {
        names.add(field.name);
      }

      if (!field.label) {
        errors.push(`Field ${index + 1}: Label is required`);
      }

      if (
        (field.type === 'select' || field.type === 'multiselect' || field.type === 'radio') &&
        field.options.length === 0
      ) {
        errors.push(`Field ${index + 1}: Options are required for ${field.type} type`);
      }
    });

    return errors;
  }

  function validateScript(code: string, format: string): string | null {
    if (!code.trim()) return null;

    // Basic syntax validation
    if (format === 'javascript') {
      try {
        // Check for common issues
        if (code.includes('eval(')) {
          return 'Warning: eval() is not recommended for security reasons';
        }
        // Basic bracket matching
        const brackets = { '(': 0, '[': 0, '{': 0 };
        for (const char of code) {
          if (char === '(') brackets['(']++;
          if (char === ')') brackets['(']--;
          if (char === '[') brackets['[']++;
          if (char === ']') brackets['[']--;
          if (char === '{') brackets['{']++;
          if (char === '}') brackets['{']--;
        }
        if (brackets['('] !== 0) return 'Unmatched parentheses';
        if (brackets['['] !== 0) return 'Unmatched square brackets';
        if (brackets['{'] !== 0) return 'Unmatched curly braces';
      } catch (e) {
        return e instanceof Error ? e.message : 'Syntax error';
      }
    }

    return null;
  }

  function saveScript() {
    if (!modeler || !selectedElement) return;

    // Validate script
    const validationError = validateScript(scriptCode, scriptFormat);
    if (validationError && !validationError.startsWith('Warning')) {
      scriptValidationError = validationError;
      return;
    }
    scriptValidationError = '';

    updateElementProperty('script', scriptCode);
    updateElementProperty('scriptFormat', scriptFormat);

    // Update local state
    elementProperties.script = scriptCode;
    elementProperties.scriptFormat = scriptFormat;

    success = 'Script saved successfully';
    setTimeout(() => (success = ''), 3000);
    showScriptEditor = false;
  }

  function openExpressionBuilder(target: string, currentValue: string) {
    expressionTarget = target;
    expressionValue = currentValue;
    showExpressionBuilder = true;
  }

  function insertVariable(variable: string) {
    expressionValue = `${expressionValue}\${${variable}}`;
  }

  function saveExpression() {
    if (expressionTarget) {
      updateElementProperty(expressionTarget, expressionValue);
    }
    showExpressionBuilder = false;
  }

  function handleTestExpression() {
      if (!testExpression) return;

      try {
          // Basic JS evaluation for testing
          let context = {};
          try {
              context = JSON.parse(testContextJson);
          } catch (e) {
              testError = "Invalid JSON Context";
              return;
          }

          let expr = testExpression.trim();
          if (expr.startsWith('${') && expr.endsWith('}')) {
              expr = expr.substring(2, expr.length - 1);
          }

          const varNames = Object.keys(context);
          const varValues = Object.values(context);

          const executionMock = {
              getVariable: (name: string) => (context as any)[name],
              getProcessInstanceId: () => 'mock-instance-id'
          };

          const func = new Function(...varNames, 'execution', `return ${expr};`);
          const result = func(...varValues, executionMock);

          testResult = String(result);
          testError = '';
      } catch (err: any) {
          testResult = '';
          testError = err.message;
      }
  }

  // BPMN Diagram Validation
  function validateBpmnDiagram(): { valid: boolean; errors: string[]; warnings: string[] } {
    if (!modeler) return { valid: false, errors: ['Modeler not initialized'], warnings: [] };

    const errors: string[] = [];
    const warnings: string[] = [];
    const elementRegistry = modeler.get('elementRegistry') as any;

    // Refresh variables list
    extractProcessVariables();

    let hasStartEvent = false;
    let hasEndEvent = false;
    const disconnectedElements: string[] = [];

    // Element types that don't participate in sequence flows
    const nonFlowElementTypes = new Set([
      'bpmn:Process',
      'bpmn:Collaboration',
      'bpmn:Participant',
      'bpmn:Lane',
      'bpmn:LaneSet',
      'bpmn:SequenceFlow',
      'bpmn:MessageFlow',
      'bpmn:Association',
      'bpmn:DataObject',
      'bpmn:DataObjectReference',
      'bpmn:DataStoreReference',
      'bpmn:DataInput',
      'bpmn:DataOutput',
      'bpmn:TextAnnotation',
      'bpmn:Group',
      'bpmn:Category',
      'bpmn:CategoryValue',
      'label'
    ]);

    // Element types that don't require incoming connections
    const noIncomingRequired = new Set([
      'bpmn:StartEvent',
      'bpmn:BoundaryEvent', // Attached to host element, triggered by events
      'bpmn:EventBasedGateway' // Can be used as a start pattern
    ]);

    // Element types that don't require outgoing connections
    const noOutgoingRequired = new Set([
      'bpmn:EndEvent',
      'bpmn:IntermediateThrowEvent', // Can be terminal (e.g., terminate, escalation, compensation)
      'bpmn:CompensateEventDefinition'
    ]);

    // Check if an element is a terminating throw event
    function isTerminatingThrowEvent(bo: any): boolean {
      if (!bo.eventDefinitions || bo.eventDefinitions.length === 0) return false;
      return bo.eventDefinitions.some(
        (ed: any) =>
          ed.$type === 'bpmn:TerminateEventDefinition' ||
          ed.$type === 'bpmn:EscalationEventDefinition' ||
          ed.$type === 'bpmn:CompensateEventDefinition' ||
          ed.$type === 'bpmn:SignalEventDefinition' ||
          ed.$type === 'bpmn:MessageEventDefinition'
      );
    }

    // Check if element is inside a subprocess (internal flows handled separately)
    function isInsideSubProcess(element: any): boolean {
      let parent = element.parent;
      while (parent) {
        if (
          parent.type === 'bpmn:SubProcess' ||
          parent.type === 'bpmn:Transaction' ||
          parent.type === 'bpmn:AdHocSubProcess'
        ) {
          return true;
        }
        parent = parent.parent;
      }
      return false;
    }

    // Check if a subprocess is collapsed (visual only, no internal elements shown)
    function isCollapsedSubProcess(element: any): boolean {
      if (
        element.type !== 'bpmn:SubProcess' &&
        element.type !== 'bpmn:Transaction' &&
        element.type !== 'bpmn:AdHocSubProcess'
      ) {
        return false;
      }
      // Check if it's collapsed (di:isExpanded attribute)
      return element.collapsed === true || element.businessObject?.di?.isExpanded === false;
    }

    // Collect all elements for validation
    const allElements: any[] = [];
    elementRegistry.forEach((element: any) => {
      allElements.push(element);
    });

    // First pass: identify start/end events and build element map
    for (const element of allElements) {
      const type = element.type;

      // Track start and end events (only at process level, not inside subprocesses)
      if (type === 'bpmn:StartEvent' && !isInsideSubProcess(element)) {
        hasStartEvent = true;
      }
      if (type === 'bpmn:EndEvent' && !isInsideSubProcess(element)) {
        hasEndEvent = true;
      }
    }

    // Second pass: validate connections and element-specific rules
    for (const element of allElements) {
      const type = element.type;
      const bo = element.businessObject;

      // Skip non-flow elements
      if (nonFlowElementTypes.has(type)) continue;

      // Skip collapsed subprocesses for internal validation
      if (isCollapsedSubProcess(element)) continue;

      // Get connections from diagram element (not business object)
      // element.incoming/outgoing contains actual visual connections in the diagram
      // bo.incoming/outgoing may be out of sync with visual state
      const incoming = element.incoming || [];
      const outgoing = element.outgoing || [];

      // Connection validation for flow elements
      const requiresIncoming = !noIncomingRequired.has(type);
      const requiresOutgoing = !noOutgoingRequired.has(type);

      // Special handling for intermediate throw events
      if (type === 'bpmn:IntermediateThrowEvent') {
        // Check if it's a terminating type that doesn't need outgoing
        if (isTerminatingThrowEvent(bo)) {
          // These can be terminal, don't require outgoing
        } else if (outgoing.length === 0) {
          // Non-terminating throw events should have outgoing
          disconnectedElements.push(
            `${type.replace('bpmn:', '')} "${bo?.name || bo?.id}" has no outgoing connections`
          );
        }
        // Always check incoming for intermediate events
        if (incoming.length === 0) {
          disconnectedElements.push(
            `${type.replace('bpmn:', '')} "${bo?.name || bo?.id}" has no incoming connections`
          );
        }
        continue;
      }

      // Check incoming connections
      if (requiresIncoming && incoming.length === 0) {
        // Don't flag boundary events - they're attached to their host
        if (type !== 'bpmn:BoundaryEvent') {
          disconnectedElements.push(
            `${type.replace('bpmn:', '')} "${bo?.name || bo?.id}" has no incoming connections`
          );
        }
      }

      // Check outgoing connections
      if (requiresOutgoing && outgoing.length === 0) {
        disconnectedElements.push(
          `${type.replace('bpmn:', '')} "${bo?.name || bo?.id}" has no outgoing connections`
        );
      }

      // Boundary events must have outgoing (they trigger a flow when event occurs)
      if (type === 'bpmn:BoundaryEvent' && outgoing.length === 0) {
        disconnectedElements.push(
          `Boundary Event "${bo?.name || bo?.id}" has no outgoing connections`
        );
      }

      // Check for user tasks without assignee (warning, not error)
      if (type === 'bpmn:UserTask') {
        const assignee = bo?.get?.('flowable:assignee');
        const candidateGroups = bo?.get?.('flowable:candidateGroups');
        const candidateUsers = bo?.get?.('flowable:candidateUsers');

        if (!assignee && !candidateGroups && !candidateUsers) {
          warnings.push(`User Task "${bo?.name || bo?.id}" has no assignee or candidates defined`);
        }
      }

      // Check for script tasks without scripts (warning, not error)
      if (type === 'bpmn:ScriptTask') {
        if (!bo?.script) {
          warnings.push(`Script Task "${bo?.name || bo?.id}" has no script defined`);
        }
      }

      // Check exclusive gateways for conditions (warning, not error)
      if (type === 'bpmn:ExclusiveGateway') {
        // Use visual outgoing connections from element
        const gatewayOutgoing = element.outgoing || [];
        if (gatewayOutgoing.length > 1) {
          const hasDefault = bo?.default;
          const flowsWithConditions = gatewayOutgoing.filter((conn: any) => {
            const flowBo = conn.businessObject;
            return flowBo?.conditionExpression || flowBo === hasDefault;
          });
          if (flowsWithConditions.length !== gatewayOutgoing.length) {
            warnings.push(`Exclusive Gateway "${bo?.name || bo?.id}" has flows without conditions`);
          }
        }
      }

      // Check for undefined variables in expressions
      if (bo) {
          const propsToCheck = ['conditionExpression', 'flowable:assignee', 'flowable:skipExpression', 'flowable:collection', 'flowable:elementVariable'];
          propsToCheck.forEach(prop => {
              let val = '';
              if (prop === 'conditionExpression') {
                  val = bo.conditionExpression?.body || '';
              } else {
                  val = bo.get(prop) || '';
              }

              if (val) {
                  // Extract variables pattern: ${varName}
                  const matches = val.match(/\$\{([a-zA-Z0-9_]+)\}/g);
                  if (matches) {
                      matches.forEach((m: string) => {
                          const varName = m.substring(2, m.length - 1);
                          // Skip standard/implicit variables
                          if (!processVariables.includes(varName) && !['initiator', 'execution', 'task', 'authenticatedUserId'].includes(varName)) {
                              warnings.push(`Element "${bo?.name || bo?.id}": Variable '${varName}' used in ${prop} but not defined in process`);
                          }
                      });
                  }
              }
          });
      }

      // Check inclusive gateways for conditions (warning, not error)
      if (type === 'bpmn:InclusiveGateway') {
        // Use visual outgoing connections from element
        const gatewayOutgoing = element.outgoing || [];
        if (gatewayOutgoing.length > 1) {
          const hasDefault = bo?.default;
          const flowsWithConditions = gatewayOutgoing.filter((conn: any) => {
            const flowBo = conn.businessObject;
            return flowBo?.conditionExpression || flowBo === hasDefault;
          });
          if (flowsWithConditions.length !== gatewayOutgoing.length) {
            warnings.push(`Inclusive Gateway "${bo?.name || bo?.id}" has flows without conditions`);
          }
        }
      }
    }

    // Critical errors: missing start/end events
    if (!hasStartEvent) {
      errors.push('Process must have at least one Start Event');
    }
    if (!hasEndEvent) {
      errors.push('Process must have at least one End Event');
    }

    // Connection issues are errors (prevent deployment)
    if (disconnectedElements.length > 0) {
      if (disconnectedElements.length <= 3) {
        errors.push(...disconnectedElements);
      } else {
        // Provide a summary with the first few issues
        errors.push(`${disconnectedElements.length} elements have connection issues:`);
        errors.push(...disconnectedElements.slice(0, 3));
        errors.push(`...and ${disconnectedElements.length - 3} more`);
      }
    }

    return { valid: errors.length === 0, errors, warnings };
  }

  async function handleDeploy() {
    if (!modeler) {
      error = 'Modeler not initialized';
      return;
    }

    if (!processName || processName.trim() === '') {
      error = 'Please enter a process name';
      return;
    }

    // Validate process name format
    if (!/^[a-zA-Z][a-zA-Z0-9_-]*$/.test(processName)) {
      error =
        'Process name must start with a letter and contain only letters, numbers, hyphens, and underscores';
      return;
    }

    // Validate BPMN diagram
    const validation = validateBpmnDiagram();
    validationWarnings = validation.warnings;
    if (!validation.valid) {
      validationErrors = validation.errors;
      error = `Diagram validation failed: ${validation.errors[0]}`;
      return;
    }
    validationErrors = [];

    isDeploying = true;
    error = '';
    success = '';

    try {
      // Get the BPMN XML from the modeler
      const result = await modeler.saveXML({ format: true });
      const bpmnXml = result.xml;

      if (!bpmnXml) {
        throw new Error('Failed to generate BPMN XML');
      }

      // Deploy the process with retry logic
      const response = await retryWithBackoff(
        () => api.deployProcess(processName, bpmnXml),
        3,
        1000
      );

      success = response.message || 'Process deployed successfully!';

      // Update the process store with the new process and invalidate cache
      // This ensures other components see the new process immediately
      if (response.process) {
        processStore.addDeployedProcess(response.process);
      } else {
        // If no process returned, just invalidate cache
        processStore.invalidateDefinitions();
      }

      // Clear draft from local storage after successful deployment
      clearDraft();

      // Navigate to processes page after brief success message display
      setTimeout(() => {
        goto('/processes/manage');
      }, 1500);
    } catch (err) {
      console.error('Deployment error:', err);
      error = err instanceof Error ? err.message : 'Failed to deploy process';
    } finally {
      isDeploying = false;
    }
  }

  function handleCancel() {
    goto('/processes');
  }

  async function handleExport() {
    if (!modeler) return;

    try {
      const result = await modeler.saveXML({ format: true });
      const bpmnXml = result.xml;

      if (!bpmnXml) {
        throw new Error('Failed to generate BPMN XML');
      }

      // Create a download link
      const blob = new Blob([bpmnXml], { type: 'application/xml' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `${processName || 'process'}.bpmn20.xml`;
      a.click();
      URL.revokeObjectURL(url);

      success = 'BPMN diagram exported successfully';
      setTimeout(() => (success = ''), 3000);
    } catch (err) {
      console.error('Export error:', err);
      error = 'Failed to export BPMN diagram';
    }
  }

  async function handleImport(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];

    if (!file || !modeler) return;

    try {
      const text = await file.text();

      // Validate the XML before importing
      if (!text.includes('bpmn:definitions') && !text.includes('definitions')) {
        throw new Error('Invalid BPMN XML format');
      }

      await modeler.importXML(text);
      const canvas = modeler.get('canvas') as any;
      canvas.zoom('fit-viewport');

      // Extract process name from imported file
      const nameMatch = text.match(/name="([^"]+)"/);
      if (nameMatch) {
        processName = nameMatch[1].replace(/\s+/g, '-').toLowerCase();
      }

      // Extract process variables
      extractProcessVariables();

      success = 'BPMN diagram imported successfully';
      setTimeout(() => (success = ''), 3000);
    } catch (err) {
      console.error('Import error:', err);
      error = 'Failed to import BPMN diagram. Please ensure the file is a valid BPMN 2.0 XML file.';
    }

    // Reset input
    input.value = '';
  }

  function getElementTypeLabel(type: string): string {
    const typeMap: Record<string, string> = {
      'bpmn:StartEvent': 'Start Event',
      'bpmn:EndEvent': 'End Event',
      'bpmn:UserTask': 'User Task',
      'bpmn:ServiceTask': 'Service Task',
      'bpmn:ScriptTask': 'Script Task',
      'bpmn:ExclusiveGateway': 'Exclusive Gateway (XOR)',
      'bpmn:ParallelGateway': 'Parallel Gateway (AND)',
      'bpmn:InclusiveGateway': 'Inclusive Gateway (OR)',
      'bpmn:SequenceFlow': 'Sequence Flow',
      'bpmn:Process': 'Process',
      'bpmn:SubProcess': 'Sub Process',
      'bpmn:CallActivity': 'Call Activity',
      'bpmn:BoundaryEvent': 'Boundary Event',
      'bpmn:IntermediateCatchEvent': 'Intermediate Catch Event',
      'bpmn:IntermediateThrowEvent': 'Intermediate Throw Event',
      'bpmn:Task': 'Task',
      'bpmn:SendTask': 'Send Task',
      'bpmn:ReceiveTask': 'Receive Task',
      'bpmn:ManualTask': 'Manual Task',
      'bpmn:BusinessRuleTask': 'Business Rule Task'
    };
    return typeMap[type] || type.replace('bpmn:', '');
  }

  function getElementTypeIcon(type: string): string {
    const iconMap: Record<string, string> = {
      'bpmn:StartEvent': '▶',
      'bpmn:EndEvent': '⬛',
      'bpmn:UserTask': '👤',
      'bpmn:ServiceTask': '⚙️',
      'bpmn:ScriptTask': '📜',
      'bpmn:ExclusiveGateway': '◇',
      'bpmn:ParallelGateway': '⊕',
      'bpmn:InclusiveGateway': '◎',
      'bpmn:SequenceFlow': '→',
      'bpmn:SubProcess': '▣',
      'bpmn:CallActivity': '↗'
    };
    return iconMap[type] || '○';
  }

  // Form field type options
  const fieldTypeOptions = [
    { value: 'text', label: 'Text' },
    { value: 'number', label: 'Number' },
    { value: 'date', label: 'Date' },
    { value: 'datetime', label: 'Date & Time' },
    { value: 'select', label: 'Dropdown' },
    { value: 'multiselect', label: 'Multi-Select' },
    { value: 'textarea', label: 'Text Area' },
    { value: 'checkbox', label: 'Checkbox' },
    { value: 'radio', label: 'Radio Buttons' },
    { value: 'file', label: 'File Upload' },
    { value: 'email', label: 'Email' },
    { value: 'phone', label: 'Phone' },
    { value: 'currency', label: 'Currency' },
    { value: 'percentage', label: 'Percentage' },
    { value: 'expression', label: 'Expression (Read-only)' }
  ];

  // Expanded field index for detailed editing
  let expandedFieldIndex = $state<number | null>(null);


</script>

<div class="min-h-screen bg-gray-50">
  <div class="flex h-screen">
    <!-- Main Content Area -->
    <div class="flex flex-1 flex-col overflow-hidden">
      <!-- Header -->
      <div class="border-b border-gray-200 bg-white p-4">
        <div class="flex items-center justify-between">
          <div>
            <h1 class="text-2xl font-bold text-gray-900">
              {isEditMode ? 'Edit Process' : 'Process Designer'}
            </h1>
            <p class="text-sm text-gray-600">
              {isEditMode
                ? `Editing: ${editProcessKey}`
                : 'Create and deploy custom BPMN processes'}
            </p>
          </div>
          <div class="flex items-center gap-2">
            {#if !isEditMode}
              <button
                onclick={() => (showTemplateModal = true)}
                class="rounded-md bg-green-100 px-3 py-2 text-sm font-medium text-green-700 hover:bg-green-200"
              >
                Load Template
              </button>
            {/if}
            <!-- Undo/Redo Controls -->
            <div class="mr-2 flex items-center gap-1 border-r border-gray-200 pr-2">
              <button
                onclick={handleUndo}
                disabled={!canUndo}
                class="rounded p-1 text-gray-600 hover:bg-gray-100 disabled:opacity-30"
                title="Undo (Ctrl+Z)"
              >
                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M3 7v6h6"></path>
                  <path d="M21 17a9 9 0 0 0-9-9 9 9 0 0 0-6 2.3L3 13"></path>
                </svg>
              </button>
              <button
                onclick={handleRedo}
                disabled={!canRedo}
                class="rounded p-1 text-gray-600 hover:bg-gray-100 disabled:opacity-30"
                title="Redo (Ctrl+Y)"
              >
                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M21 7v6h-6"></path>
                  <path d="M3 17a9 9 0 0 1 9-9 9 9 0 0 1 6 2.3L21 13"></path>
                </svg>
              </button>
            </div>

            <button
              onclick={() => (showPropertiesPanel = !showPropertiesPanel)}
              class="rounded-md bg-gray-100 px-3 py-2 text-sm font-medium text-gray-700 hover:bg-gray-200"
            >
              {showPropertiesPanel ? 'Hide' : 'Show'} Properties
            </button>
            <button
              onclick={() => (showFieldLibrary = !showFieldLibrary)}
              class="rounded-md px-3 py-2 text-sm font-medium {showFieldLibrary
                ? 'bg-purple-600 text-white'
                : 'bg-purple-100 text-purple-700 hover:bg-purple-200'}"
            >
              Field Library ({fieldLibrary.fields.length + fieldLibrary.grids.length})
            </button>
            <button
              onclick={() => (showConditionRules = !showConditionRules)}
              class="rounded-md px-3 py-2 text-sm font-medium {showConditionRules
                ? 'bg-orange-600 text-white'
                : 'bg-orange-100 text-orange-700 hover:bg-orange-200'}"
            >
              Condition Rules ({globalConditionRules.length})
            </button>
          </div>
        </div>
      </div>

      <!-- Process Metadata Form -->
      <div class="border-b border-gray-200 bg-white p-4">
        <div class="grid gap-4 md:grid-cols-2">
          <div>
            <label for="processName" class="mb-1 block text-sm font-medium text-gray-700">
              Process Key <span class="text-red-500">*</span>
            </label>
            <input
              type="text"
              id="processName"
              bind:value={processName}
              class="w-full rounded-md border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
              placeholder="e.g., my-custom-process"
              required
              disabled={isEditMode}
            />
            <p class="mt-1 text-xs text-gray-500">
              Must start with a letter, can contain letters, numbers, hyphens, underscores
            </p>
          </div>
          <div>
            <label for="processDescription" class="mb-1 block text-sm font-medium text-gray-700">
              Description
            </label>
            <input
              type="text"
              id="processDescription"
              bind:value={processDescription}
              class="w-full rounded-md border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
              placeholder="Brief description of the process"
            />
          </div>
          <div>
            <label for="processDocumentType" class="mb-1 block text-sm font-medium text-gray-700">
              Document Type
            </label>
            <select
              id="processDocumentType"
              bind:value={selectedDocumentType}
              onchange={updateProcessDocumentType}
              class="w-full rounded-md border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            >
              <option value="">-- None --</option>
              {#each documentTypes as docType}
                <option value={docType.key}>{docType.name}</option>
              {/each}
            </select>
            <p class="mt-1 text-xs text-gray-500">
              Select a document structure to use with this process
            </p>
          </div>
        </div>
      </div>

      <!-- Alerts -->
      {#if error}
        <div class="border-b border-red-200 bg-red-50 p-3">
          <div class="flex items-center">
            <svg class="mr-2 h-5 w-5 text-red-400" viewBox="0 0 20 20" fill="currentColor">
              <path
                fill-rule="evenodd"
                d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z"
                clip-rule="evenodd"
              />
            </svg>
            <p class="text-sm text-red-800">{error}</p>
            <button onclick={() => (error = '')} class="ml-auto text-red-600 hover:text-red-800" aria-label="Close error">
              <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M6 18L18 6M6 6l12 12"
                />
              </svg>
            </button>
          </div>
        </div>
      {/if}

      {#if validationErrors.length > 0}
        <div class="border-b border-red-200 bg-red-50 p-3">
          <div class="flex items-start">
            <svg
              class="mr-2 h-5 w-5 flex-shrink-0 text-red-400"
              viewBox="0 0 20 20"
              fill="currentColor"
            >
              <path
                fill-rule="evenodd"
                d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z"
                clip-rule="evenodd"
              />
            </svg>
            <div class="flex-1">
              <p class="text-sm font-medium text-red-800">
                Validation Errors (must fix before deploying):
              </p>
              <ul class="mt-1 list-inside list-disc text-sm text-red-700">
                {#each validationErrors as validationError}
                  <li>{validationError}</li>
                {/each}
              </ul>
            </div>
            <button
              onclick={() => (validationErrors = [])}
              class="ml-auto flex-shrink-0 text-red-600 hover:text-red-800"
              aria-label="Clear validation errors"
            >
              <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M6 18L18 6M6 6l12 12"
                />
              </svg>
            </button>
          </div>
        </div>
      {/if}

      {#if validationWarnings.length > 0}
        <div class="border-b border-yellow-200 bg-yellow-50 p-3">
          <div class="flex items-start">
            <svg
              class="mr-2 h-5 w-5 flex-shrink-0 text-yellow-400"
              viewBox="0 0 20 20"
              fill="currentColor"
            >
              <path
                fill-rule="evenodd"
                d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z"
                clip-rule="evenodd"
              />
            </svg>
            <div class="flex-1">
              <p class="text-sm font-medium text-yellow-800">Warnings (can still deploy):</p>
              <ul class="mt-1 list-inside list-disc text-sm text-yellow-700">
                {#each validationWarnings as warning}
                  <li>{warning}</li>
                {/each}
              </ul>
            </div>
            <button
              onclick={() => (validationWarnings = [])}
              class="ml-auto flex-shrink-0 text-yellow-600 hover:text-yellow-800"
              aria-label="Clear warnings"
            >
              <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M6 18L18 6M6 6l12 12"
                />
              </svg>
            </button>
          </div>
        </div>
      {/if}

      {#if success}
        <div class="border-b border-green-200 bg-green-50 p-3">
          <div class="flex items-center">
            <svg class="mr-2 h-5 w-5 text-green-400" viewBox="0 0 20 20" fill="currentColor">
              <path
                fill-rule="evenodd"
                d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z"
                clip-rule="evenodd"
              />
            </svg>
            <p class="text-sm text-green-800">{success}</p>
          </div>
        </div>
      {/if}

      {#if isLoading}
        <div class="border-b border-blue-200 bg-blue-50 p-3">
          <div class="flex items-center">
            <svg class="mr-2 h-5 w-5 animate-spin text-blue-600" fill="none" viewBox="0 0 24 24">
              <circle
                class="opacity-25"
                cx="12"
                cy="12"
                r="10"
                stroke="currentColor"
                stroke-width="4"
              ></circle>
              <path
                class="opacity-75"
                fill="currentColor"
                d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
              ></path>
            </svg>
            <p class="text-sm text-blue-800">
              Loading process...{loadRetryCount > 1
                ? ` (Attempt ${loadRetryCount}/${MAX_LOAD_RETRIES})`
                : ''}
            </p>
          </div>
        </div>
      {/if}

      <!-- Main Designer Area -->
      <div class="flex flex-1 overflow-hidden">
        <!-- BPMN Modeler -->
        <div class="flex-1">
          <div class="border-b border-gray-200 bg-white p-2">
            <div class="flex items-center justify-between">
              <h2 class="text-sm font-semibold text-gray-700">BPMN Diagram</h2>
              <div class="flex gap-2">
                <label
                  class="cursor-pointer rounded bg-gray-100 px-3 py-1 text-xs font-medium text-gray-700 hover:bg-gray-200"
                >
                  Import
                  <input
                    type="file"
                    accept=".bpmn,.bpmn20.xml,.xml"
                    class="hidden"
                    onchange={handleImport}
                  />
                </label>
                <button
                  onclick={handleExport}
                  class="rounded bg-gray-100 px-3 py-1 text-xs font-medium text-gray-700 hover:bg-gray-200"
                >
                  Export
                </button>
                <button
                  onclick={() => {
                    const validation = validateBpmnDiagram();
                    validationErrors = validation.errors;
                    validationWarnings = validation.warnings;
                  }}
                  class="rounded bg-blue-100 px-3 py-1 text-xs font-medium text-blue-700 hover:bg-blue-200"
                >
                  Validate
                </button>
              </div>
            </div>
          </div>
          <div bind:this={modelerContainer} class="h-full w-full bg-white"></div>
        </div>

        <!-- Properties Panel -->
        {#if showPropertiesPanel}
          <div class="w-96 flex-shrink-0 overflow-y-auto border-l border-gray-200 bg-white">
            <div class="border-b border-gray-200 bg-gray-50 p-3">
              <h3 class="text-sm font-semibold text-gray-900">Properties</h3>
              {#if selectedElement}
                <p class="flex items-center gap-2 text-xs text-gray-500">
                  <span>{getElementTypeIcon(elementProperties.type)}</span>
                  <span>{getElementTypeLabel(elementProperties.type)}</span>
                </p>
              {:else}
                <p class="text-xs text-gray-500">Select an element to edit</p>
              {/if}
            </div>

            {#if selectedElement && elementProperties.type}
              <div class="space-y-4 p-4">
                <!-- Basic Properties -->
                <div class="rounded-lg bg-gray-50 p-3">
                  <h4 class="mb-2 text-xs font-semibold text-gray-700 uppercase tracking-wide">
                    Basic
                  </h4>

                  <div class="mb-3">
                    <label for="prop-id" class="mb-1 block text-xs font-medium text-gray-700">ID</label>
                    <input
                      id="prop-id"
                      type="text"
                      value={elementProperties.id}
                      oninput={(e) => updateElementProperty('id', e.currentTarget.value)}
                      class="w-full rounded border border-gray-300 px-2 py-1 text-sm focus:border-blue-500 focus:outline-none"
                    />
                  </div>

                  <div>
                    <label for="prop-name" class="mb-1 block text-xs font-medium text-gray-700">Name</label>
                    <input
                      id="prop-name"
                      type="text"
                      value={elementProperties.name}
                      oninput={(e) => updateElementProperty('name', e.currentTarget.value)}
                      class="w-full rounded border border-gray-300 px-2 py-1 text-sm focus:border-blue-500 focus:outline-none"
                    />
                  </div>
                </div>

                <!-- User Task Properties -->
                {#if elementProperties.type === 'bpmn:UserTask'}
                  <div class="rounded-lg bg-blue-50 p-3">
                    <h4 class="mb-2 text-xs font-semibold text-blue-900 uppercase tracking-wide">
                      Assignment
                    </h4>

                    <div class="mb-3">
                      <label for="prop-assignee" class="mb-1 block text-xs font-medium text-gray-700">Assignee</label>
                      <div class="flex gap-1">
                        <input
                          id="prop-assignee"
                          type="text"
                          value={elementProperties.assignee}
                          oninput={(e) =>
                            updateElementProperty('flowable:assignee', e.currentTarget.value)}
                          placeholder={'${initiator}'}
                          class="flex-1 rounded border border-gray-300 px-2 py-1 text-sm focus:border-blue-500 focus:outline-none"
                        />
                        <button
                          onclick={() =>
                            openExpressionBuilder('flowable:assignee', elementProperties.assignee)}
                          class="rounded bg-blue-100 px-2 py-1 text-xs text-blue-700 hover:bg-blue-200"
                          title="Expression Builder"
                        >
                          {'${..}'}
                        </button>
                      </div>
                      <p class="mt-1 text-xs text-gray-500">
                        User ID or ${'{'}variable{'}'} expression
                      </p>
                    </div>

                    <div class="mb-3">
                      <label for="prop-candidateGroups" class="mb-1 block text-xs font-medium text-gray-700"
                        >Candidate Groups</label
                      >
                      <input
                        id="prop-candidateGroups"
                        type="text"
                        value={elementProperties.candidateGroups}
                        oninput={(e) =>
                          updateElementProperty('flowable:candidateGroups', e.currentTarget.value)}
                        placeholder="supervisors,managers"
                        class="w-full rounded border border-gray-300 px-2 py-1 text-sm focus:border-blue-500 focus:outline-none"
                      />
                      <p class="mt-1 text-xs text-gray-500">Comma-separated group names</p>
                    </div>

                    <div class="mb-3">
                      <label for="prop-candidateUsers" class="mb-1 block text-xs font-medium text-gray-700"
                        >Candidate Users</label
                      >
                      <input
                        id="prop-candidateUsers"
                        type="text"
                        value={elementProperties.candidateUsers}
                        oninput={(e) =>
                          updateElementProperty('flowable:candidateUsers', e.currentTarget.value)}
                        placeholder="user1,user2"
                        class="w-full rounded border border-gray-300 px-2 py-1 text-sm focus:border-blue-500 focus:outline-none"
                      />
                    </div>

                    <div class="mb-3">
                      <label for="prop-dueDate" class="mb-1 block text-xs font-medium text-gray-700">Due Date</label>
                      <input
                        id="prop-dueDate"
                        type="text"
                        value={elementProperties.dueDate}
                        oninput={(e) =>
                          updateElementProperty('flowable:dueDate', e.currentTarget.value)}
                        placeholder={'${dueDate} or ISO date'}
                        class="w-full rounded border border-gray-300 px-2 py-1 text-sm focus:border-blue-500 focus:outline-none"
                      />
                    </div>

                    <div class="mb-3">
                      <label for="prop-priority" class="mb-1 block text-xs font-medium text-gray-700">Priority</label>
                      <input
                        id="prop-priority"
                        type="text"
                        value={elementProperties.priority}
                        oninput={(e) =>
                          updateElementProperty('flowable:priority', e.currentTarget.value)}
                        placeholder="50"
                        class="w-full rounded border border-gray-300 px-2 py-1 text-sm focus:border-blue-500 focus:outline-none"
                      />
                    </div>
                  </div>

                  <div class="rounded-lg bg-indigo-50 p-3">
                    <h4 class="mb-2 text-xs font-semibold text-indigo-900 uppercase tracking-wide">
                      Form
                    </h4>

                    <div class="mb-3">
                      <label for="prop-documentType" class="mb-1 block text-xs font-medium text-gray-700"
                        >Document Type</label
                      >
                      <div class="flex gap-1">
                        <select
                          id="prop-documentType"
                          value={elementProperties.documentType}
                          onchange={(e) => handleDocumentTypeChange(e.currentTarget.value)}
                          class="w-full rounded border border-gray-300 px-2 py-1 text-sm focus:border-blue-500 focus:outline-none"
                        >
                          <option value="">-- Select Document Type --</option>
                          {#each documentTypes as docType}
                            <option value={docType.key}>{docType.name}</option>
                          {/each}
                        </select>
                        <button
                          onclick={handleSyncDocumentType}
                          disabled={!elementProperties.documentType}
                          class="p-1 text-gray-500 hover:text-blue-600 disabled:opacity-30 disabled:hover:text-gray-500 {isDocTypeSynced ? '' : 'text-amber-500 hover:text-amber-600 animate-pulse'}"
                          title={isDocTypeSynced ? "Synced with definition" : "Out of sync! Click to update."}
                        >
                          {#if !isDocTypeSynced}
                             <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                             </svg>
                          {:else}
                             <svg
                                class="w-5 h-5"
                                fill="none"
                                viewBox="0 0 24 24"
                                stroke="currentColor"
                              >
                                <path
                                  stroke-linecap="round"
                                  stroke-linejoin="round"
                                  stroke-width="2"
                                  d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"
                                />
                              </svg>
                          {/if}
                        </button>
                      </div>
                      <p class="mt-1 text-[10px] text-gray-500">
                        Selecting a document type will overwrite existing form fields.
                      </p>
                    </div>

                    <div class="mb-3">
                      <label for="prop-formKey" class="mb-1 block text-xs font-medium text-gray-700">Form Key</label>
                      <input
                        id="prop-formKey"
                        type="text"
                        value={elementProperties.formKey}
                        oninput={(e) =>
                          updateElementProperty('flowable:formKey', e.currentTarget.value)}
                        placeholder="approval-form"
                        class="w-full rounded border border-gray-300 px-2 py-1 text-sm focus:border-blue-500 focus:outline-none"
                      />
                    </div>

                    <div class="space-y-2">
                      <button
                        onclick={() => (showFormBuilder = true)}
                        class="w-full rounded bg-indigo-600 py-1.5 text-xs font-medium text-white hover:bg-indigo-700"
                      >
                        Configure Form Fields ({formFields.length})
                      </button>
            <button
              onclick={() => (showExpressionTester = true)}
              class="rounded-md bg-teal-100 px-3 py-2 text-sm font-medium text-teal-700 hover:bg-teal-200"
              title="Test Expressions"
            >
                <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
                </svg>
            </button>

                      <button
                        onclick={() => (showGridBuilder = true)}
                        class="w-full rounded bg-emerald-600 py-1.5 text-xs font-medium text-white hover:bg-emerald-700"
                      >
                        Configure Data Grids ({formGrids.length})
                      </button>
                    </div>

                    {#if formFields.length > 0 || formGrids.length > 0}
                      <div class="mt-2 rounded bg-gray-100 p-2 text-xs text-gray-600">
                        <span class="font-medium">Form elements:</span>
                        {formFields.length} field{formFields.length !== 1 ? 's' : ''}, {formGrids.length}
                        grid{formGrids.length !== 1 ? 's' : ''}
                      </div>
                    {/if}
                  </div>

                  <!-- Multi-Instance Configuration -->
                  <div class="rounded-lg bg-teal-50 p-3">
                    <h4 class="mb-2 text-xs font-semibold text-teal-900 uppercase tracking-wide">
                      Multi-Instance
                    </h4>

                    <div class="mb-3">
                      <label for="prop-multiInstanceType" class="mb-1 block text-xs font-medium text-gray-700">Type</label>
                      <select
                        id="prop-multiInstanceType"
                        value={elementProperties.multiInstanceType}
                        onchange={(e) =>
                          updateElementProperty('multiInstanceType', e.currentTarget.value)}
                        class="w-full rounded border border-gray-300 px-2 py-1 text-sm focus:border-blue-500 focus:outline-none"
                      >
                        <option value="none">None</option>
                        <option value="parallel">Parallel</option>
                        <option value="sequential">Sequential</option>
                      </select>
                    </div>

                    {#if elementProperties.multiInstanceType !== 'none'}
                      <div class="mb-3">
                        <label for="prop-loopCardinality" class="mb-1 block text-xs font-medium text-gray-700"
                          >Loop Cardinality</label
                        >
                        <input
                          id="prop-loopCardinality"
                          type="text"
                          value={elementProperties.loopCardinality}
                          oninput={(e) =>
                            updateElementProperty('loopCardinality', e.currentTarget.value)}
                          placeholder={'3 or ${count}'}
                          class="w-full rounded border border-gray-300 px-2 py-1 text-sm focus:border-blue-500 focus:outline-none"
                        />
                      </div>

                      <div class="mb-3">
                        <label for="prop-collection" class="mb-1 block text-xs font-medium text-gray-700"
                          >Collection</label
                        >
                        <input
                          id="prop-collection"
                          type="text"
                          value={elementProperties.collection}
                          oninput={(e) =>
                            updateElementProperty('collection', e.currentTarget.value)}
                          placeholder={'\${assigneeList}'}
                          class="w-full rounded border border-gray-300 px-2 py-1 text-sm focus:border-blue-500 focus:outline-none"
                        />
                      </div>

                      <div class="mb-3">
                        <label for="prop-elementVariable" class="mb-1 block text-xs font-medium text-gray-700"
                          >Element Variable</label
                        >
                        <input
                          id="prop-elementVariable"
                          type="text"
                          value={elementProperties.elementVariable}
                          oninput={(e) =>
                            updateElementProperty('elementVariable', e.currentTarget.value)}
                          placeholder="assignee"
                          class="w-full rounded border border-gray-300 px-2 py-1 text-sm focus:border-blue-500 focus:outline-none"
                        />
                      </div>

                      <div class="mb-3">
                        <label for="prop-completionCondition" class="mb-1 block text-xs font-medium text-gray-700"
                          >Completion Condition</label
                        >
                        <input
                          id="prop-completionCondition"
                          type="text"
                          value={elementProperties.completionCondition}
                          oninput={(e) =>
                            updateElementProperty('completionCondition', e.currentTarget.value)}
                          placeholder={'\${nrOfCompletedInstances >= 2}'}
                          class="w-full rounded border border-gray-300 px-2 py-1 text-sm focus:border-blue-500 focus:outline-none"
                        />
                      </div>
                    {/if}
                  </div>
                {/if}

                <!-- Service Task Properties -->
                {#if elementProperties.type === 'bpmn:ServiceTask'}
                  <div class="rounded-lg bg-green-50 p-3">
                    <h4 class="mb-2 text-xs font-semibold text-green-900 uppercase tracking-wide">
                      Implementation
                    </h4>

                    <div class="mb-3">
                      <label for="prop-implementation" class="mb-1 block text-xs font-medium text-gray-700">Type</label>
                      <select
                        id="prop-implementation"
                        value={elementProperties.implementation}
                        onchange={(e) => {
                          elementProperties.implementation = e.currentTarget.value;
                        }}
                        class="w-full rounded border border-gray-300 px-2 py-1 text-sm focus:border-blue-500 focus:outline-none"
                      >
                        <option value="class">Java Class</option>
                        <option value="expression">Expression</option>
                        <option value="delegateExpression">Delegate Expression</option>
                      </select>
                    </div>

                    {#if elementProperties.implementation === 'class'}
                      <div class="mb-3">
                        <label for="prop-class" class="mb-1 block text-xs font-medium text-gray-700">Class</label>
                        <input
                          id="prop-class"
                          type="text"
                          value={elementProperties.class}
                          oninput={(e) =>
                            updateElementProperty('flowable:class', e.currentTarget.value)}
                          placeholder="com.example.MyDelegate"
                          class="w-full rounded border border-gray-300 px-2 py-1 text-sm focus:border-blue-500 focus:outline-none"
                        />
                      </div>
                    {:else if elementProperties.implementation === 'expression'}
                      <div class="mb-3">
                        <label for="prop-expression" class="mb-1 block text-xs font-medium text-gray-700"
                          >Expression</label
                        >
                        <input
                          id="prop-expression"
                          type="text"
                          value={elementProperties.expression}
                          oninput={(e) =>
                            updateElementProperty('flowable:expression', e.currentTarget.value)}
                          placeholder={'${myBean.execute()}'}
                          class="w-full rounded border border-gray-300 px-2 py-1 text-sm focus:border-blue-500 focus:outline-none"
                        />
                      </div>
                    {:else}
                      <div class="mb-3">
                        <label for="delegateExpression" class="mb-1 block text-xs font-medium text-gray-700"
                          >Delegate Expression</label
                        >
                        <input
                          id="delegateExpression"
                          type="text"
                          value={elementProperties.delegateExpression}
                          oninput={(e) =>
                            updateElementProperty(
                              'flowable:delegateExpression',
                              e.currentTarget.value
                            )}
                          placeholder={'${myDelegate}'}
                          class="w-full rounded border border-gray-300 px-2 py-1 text-sm focus:border-blue-500 focus:outline-none"
                        />
                      </div>
                    {/if}

                    <div class="mb-3">
                      <label for="prop-resultVariable" class="mb-1 block text-xs font-medium text-gray-700"
                        >Result Variable</label
                      >
                      <input
                        id="prop-resultVariable"
                        type="text"
                        value={elementProperties.resultVariable}
                        oninput={(e) =>
                          updateElementProperty('flowable:resultVariable', e.currentTarget.value)}
                        placeholder="result"
                        class="w-full rounded border border-gray-300 px-2 py-1 text-sm focus:border-blue-500 focus:outline-none"
                      />
                    </div>
                  </div>
                {/if}

                <!-- Timer Event Properties -->
                {#if (elementProperties.type === 'bpmn:BoundaryEvent' || elementProperties.type === 'bpmn:IntermediateCatchEvent' || elementProperties.type === 'bpmn:StartEvent') && selectedElement?.businessObject?.eventDefinitions?.[0]?.$type === 'bpmn:TimerEventDefinition'}
                  <div class="rounded-lg bg-blue-50 p-3 mb-3">
                    <h4 class="mb-2 text-xs font-semibold text-blue-900 uppercase tracking-wide">
                      Timer Configuration
                    </h4>
                    
                    <div class="mb-3">
                      <label for="prop-timeDuration" class="mb-1 block text-xs font-medium text-gray-700">Time Duration (ISO 8601)</label>
                      <input
                        id="prop-timeDuration"
                        type="text"
                        value={elementProperties.timeDuration || ''}
                        oninput={(e) => updateElementProperty('timeDuration', e.currentTarget.value)}
                        placeholder="PT1H (1 Hour)"
                        class="w-full rounded border border-gray-300 px-2 py-1 text-sm focus:border-blue-500 focus:outline-none"
                      />
                      <p class="mt-1 text-xs text-gray-500">e.g., PT15M (15 min), P2D (2 days)</p>
                    </div>

                    <div class="mb-3">
                      <label for="prop-timeDate" class="mb-1 block text-xs font-medium text-gray-700">Time Date (ISO 8601)</label>
                      <input
                        id="prop-timeDate"
                        type="text"
                        value={elementProperties.timeDate || ''}
                        oninput={(e) => updateElementProperty('timeDate', e.currentTarget.value)}
                        placeholder="2023-01-01T12:00:00Z"
                        class="w-full rounded border border-gray-300 px-2 py-1 text-sm focus:border-blue-500 focus:outline-none"
                      />
                    </div>

                    <div class="mb-3">
                      <label for="prop-timeCycle" class="mb-1 block text-xs font-medium text-gray-700">Time Cycle (Cron/ISO)</label>
                      <input
                        id="prop-timeCycle"
                        type="text"
                        value={elementProperties.timeCycle || ''}
                        oninput={(e) => updateElementProperty('timeCycle', e.currentTarget.value)}
                        placeholder="R3/PT10H (3 times, every 10 hours)"
                        class="w-full rounded border border-gray-300 px-2 py-1 text-sm focus:border-blue-500 focus:outline-none"
                      />
                    </div>
                  </div>
                {/if}

                <!-- Script Task Properties -->
                {#if elementProperties.type === 'bpmn:ScriptTask'}
                  <div class="rounded-lg bg-purple-50 p-3">
                    <h4 class="mb-2 text-xs font-semibold text-purple-900 uppercase tracking-wide">
                      Script
                    </h4>

                    <div class="mb-3">
                      <label for="prop-scriptFormat" class="mb-1 block text-xs font-medium text-gray-700"
                        >Script Format</label
                      >
                      <select
                        id="prop-scriptFormat"
                        bind:value={scriptFormat}
                        onchange={(e) =>
                          updateElementProperty('scriptFormat', e.currentTarget.value)}
                        class="w-full rounded border border-gray-300 px-2 py-1 text-sm focus:border-blue-500 focus:outline-none"
                      >
                        <option value="javascript">JavaScript (Nashorn)</option>
                        <option value="groovy">Groovy</option>
                        <option value="juel">JUEL Expression</option>
                      </select>
                    </div>

                    <div class="mb-3">
                      <label for="prop-scriptResultVariable" class="mb-1 block text-xs font-medium text-gray-700"
                        >Result Variable</label
                      >
                      <input
                        id="prop-scriptResultVariable"
                        type="text"
                        value={elementProperties.resultVariable}
                        oninput={(e) =>
                          updateElementProperty('flowable:resultVariable', e.currentTarget.value)}
                        placeholder="scriptResult"
                        class="w-full rounded border border-gray-300 px-2 py-1 text-sm focus:border-blue-500 focus:outline-none"
                      />
                    </div>

                    <button
                      onclick={() => {
                        scriptCode = elementProperties.script || '';
                        scriptValidationError = '';
                        showScriptEditor = true;
                      }}
                      class="w-full rounded bg-purple-600 py-1.5 text-xs font-medium text-white hover:bg-purple-700"
                    >
                      {elementProperties.script ? 'Edit Script Code' : 'Add Script Code'}
                    </button>

                    {#if elementProperties.script}
                      <p class="mt-2 text-xs text-green-600">
                        Script configured ({elementProperties.script.length} chars)
                      </p>
                    {/if}
                  </div>
                {/if}

                <!-- Sequence Flow Properties -->
                {#if elementProperties.type === 'bpmn:SequenceFlow'}
                  <div class="rounded-lg bg-orange-50 p-3">
                    <h4 class="mb-2 text-xs font-semibold text-orange-900 uppercase tracking-wide">
                      Condition
                    </h4>
                    <div class="flex gap-1">
                      <textarea
                        value={elementProperties.conditionExpression}
                        oninput={(e) =>
                          updateElementProperty('conditionExpression', e.currentTarget.value)}
                        placeholder={"${decision == 'approved'}"}
                        rows="3"
                        class="flex-1 rounded border border-gray-300 px-2 py-1 font-mono text-xs focus:border-blue-500 focus:outline-none"
                      ></textarea>
                    </div>
                    <p class="mt-1 text-xs text-gray-500">
                      Use ${'{'}variable{'}'} syntax for conditions
                    </p>

                    <div class="mt-2">
                      <p class="text-xs text-gray-600">Quick expressions:</p>
                      <div class="mt-1 flex flex-wrap gap-1">
                        <button
                          onclick={() =>
                            updateElementProperty(
                              'conditionExpression',
                              "${decision == 'approved'}"
                            )}
                          class="rounded bg-orange-100 px-2 py-0.5 text-xs text-orange-700 hover:bg-orange-200"
                        >
                          Approved
                        </button>
                        <button
                          onclick={() =>
                            updateElementProperty(
                              'conditionExpression',
                              "${decision == 'rejected'}"
                            )}
                          class="rounded bg-orange-100 px-2 py-0.5 text-xs text-orange-700 hover:bg-orange-200"
                        >
                          Rejected
                        </button>
                        <button
                          onclick={() =>
                            updateElementProperty('conditionExpression', '${amount > 10000}')}
                          class="rounded bg-orange-100 px-2 py-0.5 text-xs text-orange-700 hover:bg-orange-200"
                        >
                          Amount > 10000
                        </button>
                      </div>
                    </div>
                  </div>
                {/if}

                <!-- Advanced Properties (for tasks) -->
                {#if elementProperties.type === 'bpmn:UserTask' || elementProperties.type === 'bpmn:ServiceTask' || elementProperties.type === 'bpmn:ScriptTask'}
                  <div class="rounded-lg bg-gray-100 p-3">
                    <h4 class="mb-2 text-xs font-semibold text-gray-700 uppercase tracking-wide">
                      Execution
                    </h4>

                    <div class="space-y-2">
                      <label class="flex items-center text-xs text-gray-700">
                        <input
                          type="checkbox"
                          checked={elementProperties.asyncBefore}
                          onchange={(e) =>
                            updateElementProperty('asyncBefore', e.currentTarget.checked)}
                          class="mr-2 rounded border-gray-300"
                        />
                        Async Before
                      </label>

                      <label class="flex items-center text-xs text-gray-700">
                        <input
                          type="checkbox"
                          checked={elementProperties.asyncAfter}
                          onchange={(e) =>
                            updateElementProperty('asyncAfter', e.currentTarget.checked)}
                          class="mr-2 rounded border-gray-300"
                        />
                        Async After
                      </label>

                      <label class="flex items-center text-xs text-gray-700">
                        <input
                          type="checkbox"
                          checked={elementProperties.exclusive}
                          onchange={(e) =>
                            updateElementProperty('exclusive', e.currentTarget.checked)}
                          class="mr-2 rounded border-gray-300"
                        />
                        Exclusive
                      </label>
                    </div>

                    <div class="mt-3">
                      <label for="prop-skipExpression" class="mb-1 block text-xs font-medium text-gray-700"
                        >Skip Expression</label
                      >
                      <input
                        id="prop-skipExpression"
                        type="text"
                        value={elementProperties.skipExpression}
                        oninput={(e) =>
                          updateElementProperty('flowable:skipExpression', e.currentTarget.value)}
                        placeholder={'${skipTask}'}
                        class="w-full rounded border border-gray-300 px-2 py-1 text-sm focus:border-blue-500 focus:outline-none"
                      />
                    </div>
                  </div>
                {/if}

                <!-- Documentation -->
                <div class="rounded-lg bg-gray-50 p-3">
                  <h4 class="mb-2 text-xs font-semibold text-gray-700 uppercase tracking-wide">
                    Documentation
                  </h4>
                  <label for="prop-documentation" class="sr-only">Documentation</label>
                  <textarea
                    id="prop-documentation"
                    value={elementProperties.documentation}
                    oninput={(e) => updateElementProperty('documentation', e.currentTarget.value)}
                    placeholder="Describe this element..."
                    rows="3"
                    class="w-full rounded border border-gray-300 px-2 py-1 text-sm focus:border-blue-500 focus:outline-none"
                  ></textarea>
                </div>
              </div>
            {:else}
              <div class="p-4">
                <div class="rounded-lg bg-gray-50 p-4 text-center">
                  <svg
                    class="mx-auto h-10 w-10 text-gray-400"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke="currentColor"
                  >
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      stroke-width="2"
                      d="M13 10V3L4 14h7v7l9-11h-7z"
                    />
                  </svg>
                  <p class="mt-2 text-sm text-gray-600">
                    Click on an element in the diagram to edit its properties
                  </p>
                  <p class="mt-1 text-xs text-gray-500">
                    Use the palette on the left to add new elements
                  </p>
                </div>
              </div>
            {/if}
          </div>
        {/if}

        <!-- Field Library Panel -->
        {#if showFieldLibrary}
          <div class="w-[500px] flex-shrink-0 overflow-y-auto border-l border-gray-200 bg-white">
            <div class="p-4">
              <div class="flex items-center justify-between mb-4">
                <h3 class="text-lg font-semibold text-gray-900">Process Field Library</h3>
                <button
                  onclick={() => (showFieldLibrary = false)}
                  class="text-gray-400 hover:text-gray-600"
                  aria-label="Close field library"
                >
                  <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      stroke-width="2"
                      d="M6 18L18 6M6 6l12 12"
                    />
                  </svg>
                </button>
              </div>
              <FieldLibraryPanel library={fieldLibrary} onChange={handleFieldLibraryChange} />
            </div>
          </div>
        {/if}

        <!-- Condition Rules Panel -->
        {#if showConditionRules}
          <div class="w-[550px] flex-shrink-0 overflow-y-auto border-l border-gray-200 bg-white">
            <div class="p-4">
              <div class="flex items-center justify-between mb-4">
                <h3 class="text-lg font-semibold text-gray-900">Global Condition Rules</h3>
                <button
                  onclick={() => (showConditionRules = false)}
                  class="text-gray-400 hover:text-gray-600"
                  aria-label="Close condition rules"
                >
                  <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      stroke-width="2"
                      d="M6 18L18 6M6 6l12 12"
                    />
                  </svg>
                </button>
              </div>
              <ConditionRuleList
                rules={globalConditionRules}
                availableFields={fieldLibrary.fields}
                availableGrids={fieldLibrary.grids}
                onChange={handleConditionRulesChange}
                title="Condition Rules"
                description="Rules are evaluated in priority order. 'Least access wins' - the most restrictive rule takes effect."
              />
            </div>
          </div>
        {/if}
      </div>

      <!-- Action Buttons -->
      <div class="border-t border-gray-200 bg-white p-4">
        <div class="flex justify-end gap-4">
          <button
            onclick={handleCancel}
            class="rounded-md border border-gray-300 bg-white px-6 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
            disabled={isDeploying}
          >
            Cancel
          </button>
          <button
            onclick={handleDeploy}
            class="rounded-md bg-blue-600 px-6 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:bg-blue-300"
            disabled={isDeploying || !modelerReady}
          >
            {isDeploying ? 'Deploying...' : isEditMode ? 'Update Process' : 'Deploy Process'}
          </button>
        </div>
      </div>
    </div>
  </div>
</div>

<!-- Form Builder Modal - Enhanced with Grid Support -->
{#if showFormBuilder}
  <div class="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50">
    <div class="max-h-[90vh] w-full max-w-5xl overflow-hidden rounded-lg bg-white shadow-xl">
      <div class="border-b border-gray-200 p-4">
        <div class="flex items-center justify-between">
          <div>
            <h3 class="text-lg font-semibold text-gray-900">Form Builder</h3>
            <p class="mt-1 text-sm text-gray-500">
              Define form fields with validation rules and grid layout
            </p>
          </div>
          <div class="flex items-center gap-2">
            <button
              onclick={() => (showGridConfig = !showGridConfig)}
              class="rounded bg-gray-100 px-3 py-1 text-xs font-medium text-gray-700 hover:bg-gray-200"
            >
              Grid Config
            </button>
            <button
              onclick={handleSaveAsDocumentType}
              class="rounded bg-indigo-100 px-3 py-1 text-xs font-medium text-indigo-700 hover:bg-indigo-200"
              title="Save current fields as a reusable Document Type"
            >
              Save as Document Type
            </button>
            <button
              onclick={() => (showFormBuilder = false)}
              class="text-gray-400 hover:text-gray-600"
              aria-label="Close form builder"
            >
              <svg class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M6 18L18 6M6 6l12 12"
                />
              </svg>
            </button>
          </div>
        </div>

        {#if showGridConfig}
          <div class="mt-3 flex items-center gap-4 rounded bg-gray-50 p-3">
            <div>
              <label for="form-grid-columns" class="block text-xs font-medium text-gray-700">Grid Columns</label>
              <select
                id="form-grid-columns"
                bind:value={formGridColumns}
                class="mt-1 rounded border border-gray-300 px-2 py-1 text-sm"
              >
                <option value={1}>1 Column</option>
                <option value={2}>2 Columns</option>
                <option value={3}>3 Columns</option>
                <option value={4}>4 Columns</option>
              </select>
            </div>
            <div>
              <label for="form-grid-gap" class="block text-xs font-medium text-gray-700">Gap (px)</label>
              <input
                id="form-grid-gap"
                type="number"
                bind:value={formGridGap}
                min="0"
                max="48"
                class="mt-1 w-20 rounded border border-gray-300 px-2 py-1 text-sm"
              />
            </div>
          </div>
        {/if}
      </div>

      <div class="max-h-[60vh] overflow-y-auto p-4">
        {#if formFields.length === 0}
          <div class="rounded-lg bg-gray-50 p-8 text-center">
            <svg
              class="mx-auto h-12 w-12 text-gray-400"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
              />
            </svg>
            <p class="mt-2 text-gray-600">No form fields defined yet</p>
            <button
              onclick={addFormField}
              class="mt-4 rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
            >
              Add First Field
            </button>
          </div>
        {:else}
          <div class="space-y-3">
            {#each formFields as field, index}
              <div class="rounded-lg border border-gray-200 bg-white shadow-sm">
                <!-- Field Header -->
                <div
                  role="button"
                  tabindex="0"
                  class="flex w-full cursor-pointer items-center justify-between rounded-t-lg bg-gray-50 p-3 text-left"
                  onclick={() => (expandedFieldIndex = expandedFieldIndex === index ? null : index)}
                  onkeydown={(e) => {
                    if (e.key === 'Enter' || e.key === ' ') {
                      e.preventDefault();
                      expandedFieldIndex = expandedFieldIndex === index ? null : index;
                    }
                  }}
                  aria-expanded={expandedFieldIndex === index}
                >
                  <div class="flex items-center gap-3">
                    <span
                      class="flex h-6 w-6 items-center justify-center rounded-full bg-blue-100 text-xs font-medium text-blue-700"
                    >
                      {index + 1}
                    </span>
                    <div>
                      <span class="font-medium text-gray-900"
                        >{field.label || field.name || 'Untitled Field'}</span
                      >
                      <span class="ml-2 text-xs text-gray-500">({field.type})</span>
                      {#if field.required}
                        <span class="ml-1 text-xs text-red-500">*required</span>
                      {/if}
                    </div>
                  </div>
                  <div class="flex items-center gap-1">
                    <button
                      onclick={(e) => {
                        e.stopPropagation();
                        moveFormField(index, 'up');
                      }}
                      class="rounded p-1 text-gray-400 hover:bg-gray-200 hover:text-gray-600"
                      disabled={index === 0}
                      aria-label="Move field up"
                    >
                      <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path
                          stroke-linecap="round"
                          stroke-linejoin="round"
                          stroke-width="2"
                          d="M5 15l7-7 7 7"
                        />
                      </svg>
                    </button>
                    <button
                      onclick={(e) => {
                        e.stopPropagation();
                        moveFormField(index, 'down');
                      }}
                      class="rounded p-1 text-gray-400 hover:bg-gray-200 hover:text-gray-600"
                      disabled={index === formFields.length - 1}
                      aria-label="Move field down"
                    >
                      <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path
                          stroke-linecap="round"
                          stroke-linejoin="round"
                          stroke-width="2"
                          d="M19 9l-7 7-7-7"
                        />
                      </svg>
                    </button>
                    <button
                      onclick={(e) => {
                        e.stopPropagation();
                        duplicateFormField(index);
                      }}
                      class="rounded p-1 text-gray-400 hover:bg-gray-200 hover:text-gray-600"
                      title="Duplicate"
                        aria-label="Duplicate field"
                    >
                      <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path
                          stroke-linecap="round"
                          stroke-linejoin="round"
                          stroke-width="2"
                          d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z"
                        />
                      </svg>
                    </button>
                    <button
                      onclick={(e) => {
                        e.stopPropagation();
                        removeFormField(index);
                      }}
                      class="rounded p-1 text-red-400 hover:bg-red-100 hover:text-red-600"
                      title="Delete"
                        aria-label="Delete field"
                    >
                      <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path
                          stroke-linecap="round"
                          stroke-linejoin="round"
                          stroke-width="2"
                          d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"
                        />
                      </svg>
                    </button>
                    <svg
                      class="ml-2 h-4 w-4 text-gray-400 transition-transform {expandedFieldIndex ===
                      index
                        ? 'rotate-180'
                        : ''}"
                      fill="none"
                      viewBox="0 0 24 24"
                      stroke="currentColor"
                    >
                      <path
                        stroke-linecap="round"
                        stroke-linejoin="round"
                        stroke-width="2"
                        d="M19 9l-7 7-7-7"
                      />
                    </svg>
                  </div>
                </div>

                <!-- Field Configuration (Expanded) -->
                {#if expandedFieldIndex === index}
                  <div class="border-t border-gray-200 p-4">
                    <!-- Basic Settings -->
                    <div class="grid gap-4 md:grid-cols-4">
                      <div>
                        <label for="field-name-{index}" class="mb-1 block text-xs font-medium text-gray-700"
                          >Variable Name <span class="text-red-500">*</span></label
                        >
                        <input
                          id="field-name-{index}"
                          type="text"
                          bind:value={field.name}
                          placeholder="fieldName"
                          class="w-full rounded border border-gray-300 px-2 py-1.5 text-sm focus:border-blue-500 focus:outline-none"
                        />
                      </div>
                      <div>
                        <label for="field-label-{index}" class="mb-1 block text-xs font-medium text-gray-700"
                          >Label <span class="text-red-500">*</span></label
                        >
                        <input
                          id="field-label-{index}"
                          type="text"
                          bind:value={field.label}
                          placeholder="Display Label"
                          class="w-full rounded border border-gray-300 px-2 py-1.5 text-sm focus:border-blue-500 focus:outline-none"
                        />
                      </div>
                      <div>
                        <label for="field-type-{index}" class="mb-1 block text-xs font-medium text-gray-700">Type</label>
                        <select
                          id="field-type-{index}"
                          bind:value={field.type}
                          class="w-full rounded border border-gray-300 px-2 py-1.5 text-sm focus:border-blue-500 focus:outline-none"
                        >
                          {#each fieldTypeOptions as option}
                            <option value={option.value}>{option.label}</option>
                          {/each}
                        </select>
                      </div>
                      <div class="flex items-end gap-4">
                        <label class="flex items-center text-sm text-gray-700">
                          <input
                            type="checkbox"
                            bind:checked={field.required}
                            class="mr-2 rounded border-gray-300"
                          />
                          Required
                        </label>
                        <label class="flex items-center text-sm text-gray-700">
                          <input
                            type="checkbox"
                            bind:checked={field.readonly}
                            class="mr-2 rounded border-gray-300"
                          />
                          Read-only
                        </label>
                      </div>
                    </div>

                    <!-- Additional Settings -->
                    <div class="mt-4 grid gap-4 md:grid-cols-3">
                      <div>
                        <label for="field-placeholder-{index}" class="mb-1 block text-xs font-medium text-gray-700"
                          >Placeholder</label
                        >
                        <input
                          id="field-placeholder-{index}"
                          type="text"
                          bind:value={field.placeholder}
                          placeholder="Enter placeholder text"
                          class="w-full rounded border border-gray-300 px-2 py-1.5 text-sm focus:border-blue-500 focus:outline-none"
                        />
                      </div>
                      <div>
                        <label for="field-default-{index}" class="mb-1 block text-xs font-medium text-gray-700"
                          >Default Value</label
                        >
                        <input
                          id="field-default-{index}"
                          type="text"
                          bind:value={field.defaultValue}
                          placeholder="Default value"
                          class="w-full rounded border border-gray-300 px-2 py-1.5 text-sm focus:border-blue-500 focus:outline-none"
                        />
                      </div>
                      <div>
                        <label for="field-tooltip-{index}" class="mb-1 block text-xs font-medium text-gray-700">Tooltip</label>
                        <input
                          id="field-tooltip-{index}"
                          type="text"
                          bind:value={field.tooltip}
                          placeholder="Help text"
                          class="w-full rounded border border-gray-300 px-2 py-1.5 text-sm focus:border-blue-500 focus:outline-none"
                        />
                      </div>
                    </div>

                    <div class="mt-3">
                      <label for="field-visibility-{index}" class="mb-1 block text-xs font-medium text-gray-700">Visibility Expression</label>
                      <input
                        id="field-visibility-{index}"
                        type="text"
                        bind:value={field.visibilityExpression}
                        placeholder={'\${showField == true}'}
                        class="w-full rounded border border-gray-300 px-2 py-1.5 text-sm font-mono focus:border-emerald-500 focus:outline-none"
                      />
                    </div>

                    <!-- Grid Layout -->
                    <div class="mt-4">
                      <h5 class="mb-2 block text-xs font-medium text-gray-700">Grid Layout</h5
                      >
                      <div class="grid gap-4 md:grid-cols-3">
                        <div>
                          <label for="field-gridcol-{index}" class="mb-1 block text-xs text-gray-600">Column</label>
                          <select
                            id="field-gridcol-{index}"
                            bind:value={field.gridColumn}
                            class="w-full rounded border border-gray-300 px-2 py-1.5 text-sm"
                          >
                            {#each Array(formGridColumns) as _, i}
                              <option value={i + 1}>Column {i + 1}</option>
                            {/each}
                          </select>
                        </div>
                        <div>
                          <label for="field-gridwidth-{index}" class="mb-1 block text-xs text-gray-600">Width (columns)</label>
                          <select
                            id="field-gridwidth-{index}"
                            bind:value={field.gridWidth}
                            class="w-full rounded border border-gray-300 px-2 py-1.5 text-sm"
                          >
                            {#each Array(formGridColumns) as _, i}
                              <option value={i + 1}>{i + 1}</option>
                            {/each}
                          </select>
                        </div>
                        <div>
                          <label for="field-css-{index}" class="mb-1 block text-xs text-gray-600">CSS Class</label>
                          <input
                            id="field-css-{index}"
                            type="text"
                            bind:value={field.cssClass}
                            placeholder="custom-class"
                            class="w-full rounded border border-gray-300 px-2 py-1.5 text-sm"
                          />
                        </div>
                      </div>
                    </div>

                    <!-- Options for Select/Radio/Multiselect -->
                    {#if field.type === 'select' || field.type === 'multiselect' || field.type === 'radio'}
                      <div class="mt-4">
                        <h5 class="mb-2 block text-xs font-medium text-gray-700">Options</h5>
                        <div class="space-y-2">
                          {#each field.options as option, optIndex}
                            <div class="flex gap-2">
                              <input
                                type="text"
                                bind:value={option.value}
                                placeholder="Value"
                                class="flex-1 rounded border border-gray-300 px-2 py-1 text-sm"
                              />
                              <input
                                type="text"
                                bind:value={option.label}
                                placeholder="Label"
                                class="flex-1 rounded border border-gray-300 px-2 py-1 text-sm"
                              />
                              <button
                                onclick={() => removeFieldOption(index, optIndex)}
                                class="rounded bg-red-100 px-2 py-1 text-red-600 hover:bg-red-200"
                                aria-label="Remove option"
                              >
                                <svg
                                  class="h-4 w-4"
                                  fill="none"
                                  viewBox="0 0 24 24"
                                  stroke="currentColor"
                                >
                                  <path
                                    stroke-linecap="round"
                                    stroke-linejoin="round"
                                    stroke-width="2"
                                    d="M6 18L18 6M6 6l12 12"
                                  />
                                </svg>
                              </button>
                            </div>
                          {/each}
                          <button
                            onclick={() => addFieldOption(index)}
                            class="rounded border border-dashed border-gray-300 px-3 py-1 text-xs text-gray-600 hover:border-blue-500 hover:text-blue-600"
                          >
                            + Add Option
                          </button>
                        </div>
                      </div>
                    {/if}

                    <!-- Validation Rules -->
                    <div class="mt-4">
                      <h5 class="mb-2 block text-xs font-medium text-gray-700">Validation</h5>
                      <div class="grid gap-4 md:grid-cols-4">
                        {#if field.type === 'text' || field.type === 'textarea' || field.type === 'email' || field.type === 'phone'}
                          <div>
                            <label for="field-minlen-{index}" class="mb-1 block text-xs text-gray-600">Min Length</label>
                            <input
                              id="field-minlen-{index}"
                              type="number"
                              bind:value={field.validation.minLength}
                              min="0"
                              class="w-full rounded border border-gray-300 px-2 py-1 text-sm"
                            />
                          </div>
                          <div>
                            <label for="field-maxlen-{index}" class="mb-1 block text-xs text-gray-600">Max Length</label>
                            <input
                              id="field-maxlen-{index}"
                              type="number"
                              bind:value={field.validation.maxLength}
                              min="0"
                              class="w-full rounded border border-gray-300 px-2 py-1 text-sm"
                            />
                          </div>
                        {/if}
                        {#if field.type === 'number' || field.type === 'currency' || field.type === 'percentage'}
                          <div>
                            <label for="field-minval-{index}" class="mb-1 block text-xs text-gray-600">Min Value</label>
                            <input
                              id="field-minval-{index}"
                              type="number"
                              bind:value={field.validation.min}
                              class="w-full rounded border border-gray-300 px-2 py-1 text-sm"
                            />
                          </div>
                          <div>
                            <label for="field-maxval-{index}" class="mb-1 block text-xs text-gray-600">Max Value</label>
                            <input
                              id="field-maxval-{index}"
                              type="number"
                              bind:value={field.validation.max}
                              class="w-full rounded border border-gray-300 px-2 py-1 text-sm"
                            />
                          </div>
                        {/if}
                        <div class="md:col-span-2">
                          <label for="field-pattern-{index}" class="mb-1 block text-xs text-gray-600">Pattern (Regex)</label>
                          <input
                            id="field-pattern-{index}"
                            type="text"
                            bind:value={field.validation.pattern}
                            placeholder="^[A-Za-z]+$"
                            class="w-full rounded border border-gray-300 px-2 py-1 text-sm"
                          />
                        </div>
                      </div>
                      <div class="mt-2">
                        <label for="field-customval-{index}" class="mb-1 block text-xs text-gray-600"
                          >Custom Validation Expression</label
                        >
                        <input
                          id="field-customval-{index}"
                          type="text"
                          bind:value={field.validation.customExpression}
                          placeholder={'${value > 0 && value < 1000}'}
                          class="w-full rounded border border-gray-300 px-2 py-1 text-sm font-mono"
                        />
                      </div>
                    </div>

                    <!-- Dynamic Behavior -->
                    <div class="mt-4">
                      <h5 class="mb-2 block text-xs font-medium text-gray-700"
                        >Dynamic Behavior</h5
                      >
                      <div class="grid gap-4 md:grid-cols-2">
                        <div>
                          <label for="field-hidden-{index}" class="mb-1 block text-xs text-gray-600">Hidden Expression</label>
                          <input
                            id="field-hidden-{index}"
                            type="text"
                            bind:value={field.hiddenExpression}
                            placeholder={'${showField == false}'}
                            class="w-full rounded border border-gray-300 px-2 py-1 text-sm font-mono"
                          />
                        </div>
                        <div>
                          <label for="field-readonly-{index}" class="mb-1 block text-xs text-gray-600">Readonly Expression</label
                          >
                          <input
                            id="field-readonly-{index}"
                            type="text"
                            bind:value={field.readonlyExpression}
                            placeholder={"${status == 'approved'}"}
                            class="w-full rounded border border-gray-300 px-2 py-1 text-sm font-mono"
                          />
                        </div>
                      </div>
                      <div class="mt-2 grid gap-4 md:grid-cols-2">
                        <div>
                          <label for="field-required-{index}" class="mb-1 block text-xs text-gray-600">Required Expression</label>
                          <input
                            id="field-required-{index}"
                            type="text"
                            bind:value={field.requiredExpression}
                            placeholder={'${amount > 1000}'}
                            class="w-full rounded border border-gray-300 px-2 py-1 text-sm font-mono"
                          />
                        </div>
                        <div>
                          <label for="field-calc-{index}" class="mb-1 block text-xs text-gray-600">Calculation Expression</label>
                          <input
                            id="field-calc-{index}"
                            type="text"
                            bind:value={field.calculationExpression}
                            placeholder={'${price * quantity}'}
                            class="w-full rounded border border-gray-300 px-2 py-1 text-sm font-mono"
                          />
                        </div>
                      </div>
                    </div>

                    <!-- JavaScript Event Handlers -->
                    <div class="mt-4">
                      <h5 class="mb-2 block text-xs font-medium text-gray-700"
                        >Event Handlers (JavaScript)</h5
                      >
                      <div class="grid gap-4 md:grid-cols-2">
                        <div>
                          <label for="field-onchange-{index}" class="mb-1 block text-xs text-gray-600">On Change</label>
                          <textarea
                            id="field-onchange-{index}"
                            bind:value={field.onChange}
                            placeholder="// value contains the new value&#10;console.log('Changed:', value);"
                            rows="2"
                            class="w-full rounded border border-gray-300 px-2 py-1 font-mono text-xs"
                          ></textarea>
                        </div>
                        <div>
                          <label for="field-onblur-{index}" class="mb-1 block text-xs text-gray-600">On Blur</label>
                          <textarea
                            id="field-onblur-{index}"
                            bind:value={field.onBlur}
                            placeholder="// Validate or transform value&#10;return value.trim();"
                            rows="2"
                            class="w-full rounded border border-gray-300 px-2 py-1 font-mono text-xs"
                          ></textarea>
                        </div>
                      </div>
                    </div>
                  </div>
                {/if}
              </div>
            {/each}

            <button
              onclick={addFormField}
              class="w-full rounded-md border-2 border-dashed border-gray-300 py-4 text-sm text-gray-600 hover:border-blue-500 hover:text-blue-600"
            >
              + Add Another Field
            </button>
          </div>
        {/if}
      </div>

      <div class="border-t border-gray-200 bg-gray-50 p-4">
        <div class="flex justify-between">
          <div class="text-sm text-gray-500">
            {formFields.length} field{formFields.length !== 1 ? 's' : ''} configured
          </div>
          <div class="flex gap-3">
            <button
              onclick={() => (showFormBuilder = false)}
              class="rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
            >
              Cancel
            </button>
            <button
              onclick={saveFormFields}
              class="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
            >
              Save Form Fields
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
{/if}

<!-- Grid Builder Modal - For adding multiple data grids -->
{#if showGridBuilder}
  <div class="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50">
    <div class="max-h-[90vh] w-full max-w-6xl overflow-hidden rounded-lg bg-white shadow-xl">
      <div class="border-b border-gray-200 p-4">
        <div class="flex items-center justify-between">
          <div>
            <h3 class="text-lg font-semibold text-gray-900">Data Grid Builder</h3>
            <p class="mt-1 text-sm text-gray-500">
              Define data grids (tables) for collecting structured data
            </p>
          </div>
          <button
            onclick={() => (showGridBuilder = false)}
            class="text-gray-400 hover:text-gray-600"
            aria-label="Close grid builder"
          >
            <svg class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </button>
        </div>
      </div>

      <div class="max-h-[60vh] overflow-y-auto p-4">
        {#if formGrids.length === 0}
          <div class="rounded-lg bg-gray-50 p-8 text-center">
            <svg
              class="mx-auto h-12 w-12 text-gray-400"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
              aria-hidden="true"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M3 10h18M3 14h18m-9-4v8m-7 0h14a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z"
              />
            </svg>
            <p class="mt-2 text-gray-600">No data grids defined yet</p>
            <p class="mt-1 text-sm text-gray-500">
              Data grids allow users to enter multiple rows of structured data
            </p>
            <button
              onclick={addGrid}
              class="mt-4 rounded-md bg-emerald-600 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-700"
            >
              Add First Grid
            </button>
          </div>
        {:else}
          <div class="space-y-4">
            {#each formGrids as grid, gridIndex}
              <div class="rounded-lg border border-gray-200 bg-white shadow-sm">
                <!-- Grid Header -->
                <button
                  type="button"
                  class="flex w-full cursor-pointer items-center justify-between rounded-t-lg bg-emerald-50 p-3 text-left"
                  onclick={() =>
                    (expandedGridIndex = expandedGridIndex === gridIndex ? null : gridIndex)}
                  aria-expanded={expandedGridIndex === gridIndex}
                >
                  <div class="flex items-center gap-3">
                    <span
                      class="flex h-6 w-6 items-center justify-center rounded-full bg-emerald-100 text-xs font-medium text-emerald-700"
                    >
                      {gridIndex + 1}
                    </span>
                    <div>
                      <span class="font-medium text-gray-900"
                        >{grid.label || grid.name || 'Untitled Grid'}</span
                      >
                      <span class="ml-2 text-xs text-gray-500"
                        >({grid.columns.length} column{grid.columns.length !== 1 ? 's' : ''})</span
                      >
                    </div>
                  </div>
                  <div class="flex items-center gap-1">
                    <span
                      role="button"
                      tabindex="0"
                      onclick={(e) => {
                        e.stopPropagation();
                        moveGrid(gridIndex, 'up');
                      }}
                      onkeydown={(e) => {
                        if (e.key === 'Enter' || e.key === ' ') {
                          e.stopPropagation();
                          moveGrid(gridIndex, 'up');
                        }
                      }}
                      class="rounded p-1 text-gray-400 hover:bg-emerald-100 hover:text-gray-600 {gridIndex ===
                      0
                        ? 'opacity-50'
                        : ''}"
                      aria-label="Move grid up"
                      aria-disabled={gridIndex === 0}
                    >
                      <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path
                          stroke-linecap="round"
                          stroke-linejoin="round"
                          stroke-width="2"
                          d="M5 15l7-7 7 7"
                        />
                      </svg>
                    </span>
                    <span
                      role="button"
                      tabindex="0"
                      onclick={(e) => {
                        e.stopPropagation();
                        moveGrid(gridIndex, 'down');
                      }}
                      onkeydown={(e) => {
                        if (e.key === 'Enter' || e.key === ' ') {
                          e.stopPropagation();
                          moveGrid(gridIndex, 'down');
                        }
                      }}
                      class="rounded p-1 text-gray-400 hover:bg-emerald-100 hover:text-gray-600 {gridIndex ===
                      formGrids.length - 1
                        ? 'opacity-50'
                        : ''}"
                      aria-label="Move grid down"
                      aria-disabled={gridIndex === formGrids.length - 1}
                    >
                      <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path
                          stroke-linecap="round"
                          stroke-linejoin="round"
                          stroke-width="2"
                          d="M19 9l-7 7-7-7"
                        />
                      </svg>
                    </span>
                    <span
                      role="button"
                      tabindex="0"
                      onclick={(e) => {
                        e.stopPropagation();
                        duplicateGrid(gridIndex);
                      }}
                      onkeydown={(e) => {
                        if (e.key === 'Enter' || e.key === ' ') {
                          e.stopPropagation();
                          duplicateGrid(gridIndex);
                        }
                      }}
                      class="rounded p-1 text-gray-400 hover:bg-emerald-100 hover:text-gray-600"
                      aria-label="Duplicate grid"
                    >
                      <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path
                          stroke-linecap="round"
                          stroke-linejoin="round"
                          stroke-width="2"
                          d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z"
                        />
                      </svg>
                    </span>
                    <span
                      role="button"
                      tabindex="0"
                      onclick={(e) => {
                        e.stopPropagation();
                        removeGrid(gridIndex);
                      }}
                      onkeydown={(e) => {
                        if (e.key === 'Enter' || e.key === ' ') {
                          e.stopPropagation();
                          removeGrid(gridIndex);
                        }
                      }}
                      class="rounded p-1 text-red-400 hover:bg-red-100 hover:text-red-600"
                      aria-label="Delete grid"
                    >
                      <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path
                          stroke-linecap="round"
                          stroke-linejoin="round"
                          stroke-width="2"
                          d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"
                        />
                      </svg>
                    </span>
                    <svg
                      class="ml-2 h-4 w-4 text-gray-400 transition-transform {expandedGridIndex ===
                      gridIndex
                        ? 'rotate-180'
                        : ''}"
                      fill="none"
                      viewBox="0 0 24 24"
                      stroke="currentColor"
                      aria-hidden="true"
                    >
                      <path
                        stroke-linecap="round"
                        stroke-linejoin="round"
                        stroke-width="2"
                        d="M19 9l-7 7-7-7"
                      />
                    </svg>
                  </div>
                </button>

                <!-- Grid Configuration (Expanded) -->
                {#if expandedGridIndex === gridIndex}
                  <div class="border-t border-gray-200 p-4">
                    <!-- Grid Basic Settings -->
                    <div class="grid gap-4 md:grid-cols-4">
                      <div>
                        <label
                          for="grid-{gridIndex}-name"
                          class="mb-1 block text-xs font-medium text-gray-700"
                          >Variable Name <span class="text-red-500">*</span></label
                        >
                        <input
                          id="grid-{gridIndex}-name"
                          type="text"
                          bind:value={grid.name}
                          placeholder="gridName"
                          class="w-full rounded border border-gray-300 px-2 py-1.5 text-sm focus:border-emerald-500 focus:outline-none"
                        />
                      </div>
                      <div>
                        <label
                          for="grid-{gridIndex}-label"
                          class="mb-1 block text-xs font-medium text-gray-700"
                          >Label <span class="text-red-500">*</span></label
                        >
                        <input
                          id="grid-{gridIndex}-label"
                          type="text"
                          bind:value={grid.label}
                          placeholder="Display Label"
                          class="w-full rounded border border-gray-300 px-2 py-1.5 text-sm focus:border-emerald-500 focus:outline-none"
                        />
                      </div>
                      <div>
                        <label
                          for="grid-{gridIndex}-minrows"
                          class="mb-1 block text-xs font-medium text-gray-700">Min Rows</label
                        >
                        <input
                          id="grid-{gridIndex}-minrows"
                          type="number"
                          bind:value={grid.minRows}
                          min="0"
                          class="w-full rounded border border-gray-300 px-2 py-1.5 text-sm focus:border-emerald-500 focus:outline-none"
                        />
                      </div>
                      <div>
                        <label
                          for="grid-{gridIndex}-maxrows"
                          class="mb-1 block text-xs font-medium text-gray-700">Max Rows</label
                        >
                        <input
                          id="grid-{gridIndex}-maxrows"
                          type="number"
                          bind:value={grid.maxRows}
                          min="0"
                          placeholder="0 = unlimited"
                          class="w-full rounded border border-gray-300 px-2 py-1.5 text-sm focus:border-emerald-500 focus:outline-none"
                        />
                      </div>
                    </div>

                    <div class="mt-3">
                      <label
                        for="grid-{gridIndex}-desc"
                        class="mb-1 block text-xs font-medium text-gray-700">Description</label
                      >
                      <input
                        id="grid-{gridIndex}-desc"
                        type="text"
                        bind:value={grid.description}
                        placeholder="Optional description"
                        class="w-full rounded border border-gray-300 px-2 py-1.5 text-sm focus:border-emerald-500 focus:outline-none"
                      />
                    </div>

                    <!-- Grid Layout -->
                    <div class="mt-3 grid gap-4 md:grid-cols-3">
                      <div>
                        <label
                          for="grid-{gridIndex}-column"
                          class="mb-1 block text-xs text-gray-600">Column</label
                        >
                        <select
                          id="grid-{gridIndex}-column"
                          bind:value={grid.gridColumn}
                          class="w-full rounded border border-gray-300 px-2 py-1.5 text-sm"
                        >
                          {#each Array(formGridColumns) as _, i}
                            <option value={i + 1}>Column {i + 1}</option>
                          {/each}
                        </select>
                      </div>
                      <div>
                        <label for="grid-{gridIndex}-width" class="mb-1 block text-xs text-gray-600"
                          >Width (columns)</label
                        >
                        <select
                          id="grid-{gridIndex}-width"
                          bind:value={grid.gridWidth}
                          class="w-full rounded border border-gray-300 px-2 py-1.5 text-sm"
                        >
                          {#each Array(formGridColumns) as _, i}
                            <option value={i + 1}>{i + 1}</option>
                          {/each}
                        </select>
                      </div>
                      <div>
                        <label
                          for="grid-{gridIndex}-cssclass"
                          class="mb-1 block text-xs text-gray-600">CSS Class</label
                        >
                        <input
                          id="grid-{gridIndex}-cssclass"
                          type="text"
                          bind:value={grid.cssClass}
                          placeholder="custom-class"
                          class="w-full rounded border border-gray-300 px-2 py-1.5 text-sm"
                        />
                      </div>
                    </div>

                    <!-- Grid Columns -->
                    <div class="mt-4">
                      <div class="mb-2 flex items-center justify-between">
                        <h5 class="text-sm font-medium text-gray-700">Grid Columns</h5>
                        <button
                          onclick={() => addGridColumn(gridIndex)}
                          class="rounded bg-emerald-100 px-2 py-1 text-xs font-medium text-emerald-700 hover:bg-emerald-200"
                        >
                          + Add Column
                        </button>
                      </div>

                      {#if grid.columns.length === 0}
                        <div class="rounded bg-gray-50 p-4 text-center text-sm text-gray-500">
                          No columns defined. Add columns to define the grid structure.
                        </div>
                      {:else}
                        <div class="space-y-2">
                          {#each grid.columns as column, colIndex}
                            <div class="rounded border border-gray-200 bg-gray-50 p-3">
                              <div class="flex items-center justify-between">
                                <div class="flex items-center gap-2">
                                  <span class="text-xs font-medium text-gray-500"
                                    >Col {colIndex + 1}:</span
                                  >
                                  <span class="text-sm font-medium"
                                    >{column.label || column.name || 'Untitled'}</span
                                  >
                                  <span class="text-xs text-gray-400">({column.type})</span>
                                  {#if column.required}
                                    <span class="text-xs text-red-500">*</span>
                                  {/if}
                                </div>
                                <div class="flex items-center gap-1">
                                  <button
                                    onclick={() => moveGridColumn(gridIndex, colIndex, 'up')}
                                    class="rounded p-1 text-gray-400 hover:bg-gray-200"
                                    disabled={colIndex === 0}
                                    aria-label="Move column up"
                                  >
                                    <svg
                                      class="h-3 w-3"
                                      fill="none"
                                      viewBox="0 0 24 24"
                                      stroke="currentColor"
                                    >
                                      <path
                                        stroke-linecap="round"
                                        stroke-linejoin="round"
                                        stroke-width="2"
                                        d="M5 15l7-7 7 7"
                                      />
                                    </svg>
                                  </button>
                                  <button
                                    onclick={() => moveGridColumn(gridIndex, colIndex, 'down')}
                                    class="rounded p-1 text-gray-400 hover:bg-gray-200"
                                    disabled={colIndex === grid.columns.length - 1}
                                    aria-label="Move column down"
                                  >
                                    <svg
                                      class="h-3 w-3"
                                      fill="none"
                                      viewBox="0 0 24 24"
                                      stroke="currentColor"
                                    >
                                      <path
                                        stroke-linecap="round"
                                        stroke-linejoin="round"
                                        stroke-width="2"
                                        d="M19 9l-7 7-7-7"
                                      />
                                    </svg>
                                  </button>
                                  <button
                                    onclick={() =>
                                      (expandedGridColumnIndex =
                                        expandedGridColumnIndex === colIndex ? null : colIndex)}
                                    class="rounded p-1 text-blue-400 hover:bg-blue-100"
                                    aria-label="Edit column"
                                  >
                                    <svg
                                      class="h-3 w-3"
                                      fill="none"
                                      viewBox="0 0 24 24"
                                      stroke="currentColor"
                                    >
                                      <path
                                        stroke-linecap="round"
                                        stroke-linejoin="round"
                                        stroke-width="2"
                                        d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"
                                      />
                                    </svg>
                                  </button>
                                  <button
                                    onclick={() => removeGridColumn(gridIndex, colIndex)}
                                    class="rounded p-1 text-red-400 hover:bg-red-100"
                                    aria-label="Remove column"
                                  >
                                    <svg
                                      class="h-3 w-3"
                                      fill="none"
                                      viewBox="0 0 24 24"
                                      stroke="currentColor"
                                    >
                                      <path
                                        stroke-linecap="round"
                                        stroke-linejoin="round"
                                        stroke-width="2"
                                        d="M6 18L18 6M6 6l12 12"
                                      />
                                    </svg>
                                  </button>
                                </div>
                              </div>

                              <!-- Column Details (Expanded) -->
                              {#if expandedGridColumnIndex === colIndex}
                                <div
                                  class="mt-3 grid gap-3 border-t border-gray-200 pt-3 md:grid-cols-4"
                                >
                                  <div>
                                    <label
                                      for="grid-{gridIndex}-col-{colIndex}-name"
                                      class="mb-1 block text-xs text-gray-600"
                                      >Name <span class="text-red-500">*</span></label
                                    >
                                    <input
                                      id="grid-{gridIndex}-col-{colIndex}-name"
                                      type="text"
                                      bind:value={column.name}
                                      placeholder="columnName"
                                      class="w-full rounded border border-gray-300 px-2 py-1 text-xs"
                                    />
                                  </div>
                                  <div>
                                    <label
                                      for="grid-{gridIndex}-col-{colIndex}-label"
                                      class="mb-1 block text-xs text-gray-600"
                                      >Label <span class="text-red-500">*</span></label
                                    >
                                    <input
                                      id="grid-{gridIndex}-col-{colIndex}-label"
                                      type="text"
                                      bind:value={column.label}
                                      placeholder="Column Label"
                                      class="w-full rounded border border-gray-300 px-2 py-1 text-xs"
                                    />
                                  </div>
                                  <div>
                                    <label
                                      for="grid-{gridIndex}-col-{colIndex}-type"
                                      class="mb-1 block text-xs text-gray-600">Type</label
                                    >
                                    <select
                                      id="grid-{gridIndex}-col-{colIndex}-type"
                                      bind:value={column.type}
                                      class="w-full rounded border border-gray-300 px-2 py-1 text-xs"
                                    >
                                      {#each gridColumnTypeOptions as typeOpt}
                                        <option value={typeOpt.value}>{typeOpt.label}</option>
                                      {/each}
                                    </select>
                                  </div>
                                  <div class="flex items-end">
                                    <label class="flex items-center text-xs text-gray-700">
                                      <input
                                        type="checkbox"
                                        bind:checked={column.required}
                                        class="mr-1.5 rounded border-gray-300"
                                      />
                                      Required
                                    </label>
                                  </div>

                                  <div class="md:col-span-2">
                                    <label
                                      for="grid-{gridIndex}-col-{colIndex}-placeholder"
                                      class="mb-1 block text-xs text-gray-600">Placeholder</label
                                    >
                                    <input
                                      id="grid-{gridIndex}-col-{colIndex}-placeholder"
                                      type="text"
                                      bind:value={column.placeholder}
                                      placeholder="Placeholder text"
                                      class="w-full rounded border border-gray-300 px-2 py-1 text-xs"
                                    />
                                  </div>

                                  {#if column.type === 'number'}
                                    <div>
                                      <label
                                        for="grid-{gridIndex}-col-{colIndex}-min"
                                        class="mb-1 block text-xs text-gray-600">Min</label
                                      >
                                      <input
                                        id="grid-{gridIndex}-col-{colIndex}-min"
                                        type="number"
                                        bind:value={column.min}
                                        class="w-full rounded border border-gray-300 px-2 py-1 text-xs"
                                      />
                                    </div>
                                    <div>
                                      <label
                                        for="grid-{gridIndex}-col-{colIndex}-max"
                                        class="mb-1 block text-xs text-gray-600">Max</label
                                      >
                                      <input
                                        id="grid-{gridIndex}-col-{colIndex}-max"
                                        type="number"
                                        bind:value={column.max}
                                        class="w-full rounded border border-gray-300 px-2 py-1 text-xs"
                                      />
                                    </div>
                                  {/if}

                                  {#if column.type === 'select'}
                                    <div
                                      class="md:col-span-4"
                                      role="group"
                                      aria-labelledby="grid-{gridIndex}-col-{colIndex}-options-label"
                                    >
                                      <span
                                        class="mb-1 block text-xs text-gray-600"
                                        id="grid-{gridIndex}-col-{colIndex}-options-label"
                                        >Options</span
                                      >
                                      <div class="space-y-1">
                                        {#each column.options as _option, optIndex}
                                          <div class="flex gap-1">
                                            <input
                                              type="text"
                                              bind:value={column.options[optIndex]}
                                              placeholder="Option value"
                                              class="flex-1 rounded border border-gray-300 px-2 py-1 text-xs"
                                              aria-label="Option {optIndex + 1} value"
                                            />
                                            <button
                                              onclick={() =>
                                                removeGridColumnOption(
                                                  gridIndex,
                                                  colIndex,
                                                  optIndex
                                                )}
                                              class="rounded bg-red-100 px-2 py-1 text-red-600 hover:bg-red-200"
                                              aria-label="Remove option {optIndex + 1}"
                                            >
                                              <svg
                                                class="h-3 w-3"
                                                fill="none"
                                                viewBox="0 0 24 24"
                                                stroke="currentColor"
                                              >
                                                <path
                                                  stroke-linecap="round"
                                                  stroke-linejoin="round"
                                                  stroke-width="2"
                                                  d="M6 18L18 6M6 6l12 12"
                                                />
                                              </svg>
                                            </button>
                                          </div>
                                        {/each}
                                        <button
                                          onclick={() => addGridColumnOption(gridIndex, colIndex)}
                                          class="rounded border border-dashed border-gray-300 px-2 py-1 text-xs text-gray-600 hover:border-emerald-500 hover:text-emerald-600"
                                        >
                                          + Add Option
                                        </button>
                                      </div>
                                    </div>
                                  {/if}

                                  <!-- Validation -->
                                  {#if column.type === 'text' || column.type === 'textarea'}
                                    <div>
                                      <label
                                        for="grid-{gridIndex}-col-{colIndex}-minlen"
                                        class="mb-1 block text-xs text-gray-600">Min Length</label
                                      >
                                      <input
                                        id="grid-{gridIndex}-col-{colIndex}-minlen"
                                        type="number"
                                        bind:value={column.validation.minLength}
                                        min="0"
                                        class="w-full rounded border border-gray-300 px-2 py-1 text-xs"
                                      />
                                    </div>
                                    <div>
                                      <label
                                        for="grid-{gridIndex}-col-{colIndex}-maxlen"
                                        class="mb-1 block text-xs text-gray-600">Max Length</label
                                      >
                                      <input
                                        id="grid-{gridIndex}-col-{colIndex}-maxlen"
                                        type="number"
                                        bind:value={column.validation.maxLength}
                                        min="0"
                                        class="w-full rounded border border-gray-300 px-2 py-1 text-xs"
                                      />
                                    </div>
                                  {/if}

                                  <div class="md:col-span-2">
                                    <label
                                      for="grid-{gridIndex}-col-{colIndex}-pattern"
                                      class="mb-1 block text-xs text-gray-600"
                                      >Pattern (Regex)</label
                                    >
                                    <input
                                      id="grid-{gridIndex}-col-{colIndex}-pattern"
                                      type="text"
                                      bind:value={column.validation.pattern}
                                      placeholder="^[A-Za-z]+$"
                                      class="w-full rounded border border-gray-300 px-2 py-1 text-xs"
                                    />
                                  </div>

                                  <div class="md:col-span-4 mt-2 pt-2 border-t border-gray-100">
                                    <h6 class="text-xs font-medium text-gray-700 mb-2">Dynamic Behavior</h6>
                                    <div class="grid gap-3 md:grid-cols-2">
                                      <div>
                                        <label for="col-hidden-{gridIndex}-{colIndex}" class="mb-1 block text-xs text-gray-600">Hidden Expr.</label>
                                        <input
                                          id="col-hidden-{gridIndex}-{colIndex}"
                                          type="text"
                                          bind:value={column.hiddenExpression}
                                          placeholder={'${hideCol}'}
                                          class="w-full rounded border border-gray-300 px-2 py-1 text-xs font-mono"
                                        />
                                      </div>
                                      <div>
                                        <label for="col-readonly-{gridIndex}-{colIndex}" class="mb-1 block text-xs text-gray-600">Readonly Expr.</label>
                                        <input
                                          id="col-readonly-{gridIndex}-{colIndex}"
                                          type="text"
                                          bind:value={column.readonlyExpression}
                                          placeholder={'${readonly}'}
                                          class="w-full rounded border border-gray-300 px-2 py-1 text-xs font-mono"
                                        />
                                      </div>
                                      <div>
                                        <label for="col-required-{gridIndex}-{colIndex}" class="mb-1 block text-xs text-gray-600">Required Expr.</label>
                                        <input
                                          id="col-required-{gridIndex}-{colIndex}"
                                          type="text"
                                          bind:value={column.requiredExpression}
                                          placeholder={'${required}'}
                                          class="w-full rounded border border-gray-300 px-2 py-1 text-xs font-mono"
                                        />
                                      </div>
                                      <div>
                                        <label for="col-calc-{gridIndex}-{colIndex}" class="mb-1 block text-xs text-gray-600">Calculation Expr.</label>
                                        <input
                                          id="col-calc-{gridIndex}-{colIndex}"
                                          type="text"
                                          bind:value={column.calculationExpression}
                                          placeholder={'${row.price * row.qty}'}
                                          class="w-full rounded border border-gray-300 px-2 py-1 text-xs font-mono"
                                        />
                                      </div>
                                    </div>
                                  </div>
                                </div>
                              {/if}
                            </div>
                          {/each}
                        </div>
                      {/if}
                    </div>
                  </div>
                {/if}
              </div>
            {/each}

            <button
              onclick={addGrid}
              class="w-full rounded-md border-2 border-dashed border-gray-300 py-4 text-sm text-gray-600 hover:border-emerald-500 hover:text-emerald-600"
            >
              + Add Another Grid
            </button>
          </div>
        {/if}
      </div>

      <div class="border-t border-gray-200 bg-gray-50 p-4">
        <div class="flex justify-between">
          <div class="text-sm text-gray-500">
            {formGrids.length} grid{formGrids.length !== 1 ? 's' : ''} configured
            {#if formGrids.length > 0}
              ({formGrids.reduce((sum, g) => sum + g.columns.length, 0)} total columns)
            {/if}
          </div>
          <div class="flex gap-3">
            <button
              onclick={() => (showGridBuilder = false)}
              class="rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
            >
              Cancel
            </button>
            <button
              onclick={saveFormGrids}
              class="rounded-md bg-emerald-600 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-700"
            >
              Save Data Grids
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
{/if}

<!-- Script Editor Modal -->
{#if showScriptEditor}
  <div class="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50">
    <div class="max-h-[90vh] w-full max-w-5xl overflow-hidden rounded-lg bg-white shadow-xl">
      <div class="border-b border-gray-200 p-4">
        <div class="flex items-center justify-between">
          <div>
            <h3 class="text-lg font-semibold text-gray-900">Script Editor</h3>
            <p class="text-sm text-gray-500">Format: {scriptFormat}</p>
          </div>
          <button
            onclick={() => (showScriptEditor = false)}
            class="text-gray-400 hover:text-gray-600"
            aria-label="Close script editor"
          >
            <svg class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </button>
        </div>
      </div>

      <div class="p-4">
        <div class="mb-3 flex gap-4">
          <div class="flex-1">
            <label for="script-format-select" class="mb-1 block text-sm font-medium text-gray-700">Script Format</label>
            <select
              id="script-format-select"
              bind:value={scriptFormat}
              class="w-full rounded border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
            >
              <option value="javascript">JavaScript (Nashorn/GraalJS)</option>
              <option value="groovy">Groovy</option>
              <option value="juel">JUEL Expression</option>
            </select>
          </div>
          <div class="flex-1">
            <label for="insert-variable-select" class="mb-1 block text-sm font-medium text-gray-700">Insert Variable</label>
            <select
              id="insert-variable-select"
              onchange={(e) => {
                if (e.currentTarget.value) {
                  scriptCode += `execution.getVariable('${e.currentTarget.value}')`;
                  e.currentTarget.value = '';
                }
              }}
              class="w-full rounded border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
            >
              <option value="">-- Select Variable --</option>
              {#each processVariables as variable}
                <option value={variable}>{variable}</option>
              {/each}
            </select>
          </div>
        </div>

        {#if scriptValidationError}
          <div class="mb-3 rounded bg-red-50 p-3 text-sm text-red-700">
            {scriptValidationError}
          </div>
        {/if}

        <div class="mb-3">
          <label for="script-code-editor" class="mb-1 block text-sm font-medium text-gray-700">Script Code</label>
          <textarea
            id="script-code-editor"
            bind:value={scriptCode}
            rows="18"
            class="w-full rounded border border-gray-300 p-3 font-mono text-sm focus:border-blue-500 focus:outline-none"
            placeholder={scriptFormat === 'javascript'
              ? `// JavaScript example (Nashorn compatible)
var amount = execution.getVariable('amount') || 0;
var approvalLevel = 'SUPERVISOR';

if (amount > 50000) {
    approvalLevel = 'EXECUTIVE';
} else if (amount > 10000) {
    approvalLevel = 'MANAGER';
}

execution.setVariable('approvalLevel', approvalLevel);

// You can also access Java classes
var LocalDate = Java.type('java.time.LocalDate');
var today = LocalDate.now();
execution.setVariable('processDate', today.toString());`
              : scriptFormat === 'groovy'
                ? `// Groovy example
def amount = execution.getVariable('amount') ?: 0
def approvalLevel = 'SUPERVISOR'

if (amount > 50000) {
    approvalLevel = 'EXECUTIVE'
} else if (amount > 10000) {
    approvalLevel = 'MANAGER'
}

execution.setVariable('approvalLevel', approvalLevel)

// Groovy closures and collections
def items = execution.getVariable('items') ?: []
def total = items.collect { it.price * it.quantity }.sum() ?: 0
execution.setVariable('total', total)`
                : `// JUEL Expression
\${amount > 50000 ? 'EXECUTIVE' : amount > 10000 ? 'MANAGER' : 'SUPERVISOR'}`}
          ></textarea>
        </div>

        <div class="grid gap-4 md:grid-cols-2">
          <div class="rounded-lg bg-gray-50 p-3">
            <h4 class="mb-2 text-sm font-medium text-gray-700">Available APIs</h4>
            <ul class="space-y-1 text-xs text-gray-600">
              <li>
                <code class="rounded bg-gray-200 px-1">execution</code> - Process execution context
              </li>
              <li>
                <code class="rounded bg-gray-200 px-1">execution.getVariable('name')</code> - Get variable
              </li>
              <li>
                <code class="rounded bg-gray-200 px-1">execution.setVariable('name', value)</code> -
                Set variable
              </li>
              <li>
                <code class="rounded bg-gray-200 px-1">execution.getProcessInstanceId()</code> - Process
                instance ID
              </li>
              <li>
                <code class="rounded bg-gray-200 px-1">execution.getBusinessKey()</code> - Business key
              </li>
            </ul>
          </div>
          <div class="rounded-lg bg-blue-50 p-3">
            <h4 class="mb-2 text-sm font-medium text-blue-700">Tips</h4>
            <ul class="space-y-1 text-xs text-blue-600">
              <li>
                Use <code class="rounded bg-blue-100 px-1">var</code> instead of
                <code class="rounded bg-blue-100 px-1">let</code>/<code
                  class="rounded bg-blue-100 px-1">const</code
                > for Nashorn
              </li>
              <li>
                Access Java types with <code class="rounded bg-blue-100 px-1"
                  >Java.type('className')</code
                >
              </li>
              <li>Always handle null/undefined values gracefully</li>
              <li>Test scripts with simple logging first</li>
            </ul>
          </div>
        </div>
      </div>

      <div class="border-t border-gray-200 bg-gray-50 p-4">
        <div class="flex justify-between">
          <button
            onclick={() => {
              scriptCode = '';
              scriptValidationError = '';
            }}
            class="rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
          >
            Clear
          </button>
          <div class="flex gap-3">
            <button
              onclick={() => (showScriptEditor = false)}
              class="rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
            >
              Cancel
            </button>
            <button
              onclick={saveScript}
              class="rounded-md bg-purple-600 px-4 py-2 text-sm font-medium text-white hover:bg-purple-700"
            >
              Save Script
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
{/if}

<!-- Expression Tester Modal -->
{#if showExpressionTester}
  <div class="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50">
    <div class="w-full max-w-2xl rounded-lg bg-white shadow-xl">
      <div class="border-b border-gray-200 p-4">
        <div class="flex items-center justify-between">
          <h3 class="text-lg font-semibold text-gray-900">Expression Tester</h3>
          <button
            onclick={() => (showExpressionTester = false)}
            class="text-gray-400 hover:text-gray-600"
            aria-label="Close expression tester"
          >
            <svg class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
      </div>

      <div class="p-4 grid gap-4">
          <div class="grid grid-cols-2 gap-4">
              <div>
                  <label for="test-context-json" class="block text-sm font-medium text-gray-700 mb-1">Test Context (JSON)</label>
                  <textarea
                    id="test-context-json"
                    bind:value={testContextJson}
                    rows="8"
                    class="w-full rounded border border-gray-300 p-2 font-mono text-xs focus:border-blue-500 focus:outline-none"
                  ></textarea>
              </div>
              <div class="flex flex-col">
                  <div class="mb-4">
                      <label for="expression-to-test" class="block text-sm font-medium text-gray-700 mb-1">Expression to Test</label>
                      <textarea
                        id="expression-to-test"
                        bind:value={testExpression}
                        placeholder={'\${amount > 500}'}
                        rows="3"
                        class="w-full rounded border border-gray-300 p-2 font-mono text-sm focus:border-blue-500 focus:outline-none"
                      ></textarea>
                  </div>

                  <button
                    onclick={handleTestExpression}
                    class="mb-4 w-full rounded bg-teal-600 py-2 text-sm font-medium text-white hover:bg-teal-700"
                  >
                    Evaluate
                  </button>

                  <div>
                      <label for="test-result-output" class="block text-sm font-medium text-gray-700 mb-1">Result</label>
                      <div id="test-result-output" class="w-full h-24 rounded border border-gray-300 p-2 bg-gray-50 overflow-auto">
                          {#if testError}
                             <span class="text-red-600 font-mono text-sm">{testError}</span>
                          {:else if testResult}
                             <span class="text-green-700 font-mono text-sm font-bold">{testResult}</span>
                          {:else}
                             <span class="text-gray-400 text-xs italic">Result will appear here...</span>
                          {/if}
                      </div>
                  </div>
              </div>
          </div>
      </div>
    </div>
  </div>
{/if}

<!-- Expression Builder Modal -->
{#if showExpressionBuilder}
  <div class="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50">
    <div class="w-full max-w-lg rounded-lg bg-white shadow-xl">
      <div class="border-b border-gray-200 p-4">
        <div class="flex items-center justify-between">
          <h3 class="text-lg font-semibold text-gray-900">Expression Builder</h3>
          <button
            onclick={() => (showExpressionBuilder = false)}
            class="text-gray-400 hover:text-gray-600"
            aria-label="Close expression builder"
          >
            <svg class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </button>
        </div>
      </div>

      <div class="p-4">
        <div class="mb-4">
          <label for="expression-builder-input" class="mb-1 block text-sm font-medium text-gray-700">Expression</label>
          <input
            id="expression-builder-input"
            type="text"
            bind:value={expressionValue}
            placeholder={'${variable}'}
            class="w-full rounded border border-gray-300 px-3 py-2 font-mono text-sm focus:border-blue-500 focus:outline-none"
          />
        </div>

        <div class="mb-4">
          <span class="mb-2 block text-sm font-medium text-gray-700">Insert Variable</span>
          <div class="flex flex-wrap gap-2">
            {#each processVariables as variable}
              <button
                onclick={() => insertVariable(variable)}
                class="rounded bg-blue-100 px-2 py-1 text-xs text-blue-700 hover:bg-blue-200"
              >
                {variable}
              </button>
            {/each}
          </div>
        </div>

        <div class="rounded bg-gray-50 p-3">
          <h4 class="mb-2 text-xs font-medium text-gray-700">Common Patterns</h4>
          <div class="space-y-1 text-xs text-gray-600">
            <button
              onclick={() => (expressionValue = '${initiator}')}
              class="block w-full rounded px-2 py-1 text-left hover:bg-gray-100"
            >
              <code>${'{'}initiator{'}'}</code> - Process initiator
            </button>
            <button
              onclick={() => (expressionValue = '${execution.getVariable("user")}')}
              class="block w-full rounded px-2 py-1 text-left hover:bg-gray-100"
            >
              <code>${'{'}execution.getVariable("user"){'}'}</code> - Get variable
            </button>
          </div>
        </div>
      </div>

      <div class="border-t border-gray-200 bg-gray-50 p-4">
        <div class="flex justify-end gap-3">
          <button
            onclick={() => (showExpressionBuilder = false)}
            class="rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
          >
            Cancel
          </button>
          <button
            onclick={saveExpression}
            class="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
          >
            Apply
          </button>
        </div>
      </div>
    </div>
  </div>
{/if}

<!-- Template Selection Modal -->
{#if showTemplateModal}
  <div class="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50">
    <div class="max-h-[85vh] w-full max-w-4xl overflow-hidden rounded-lg bg-white shadow-xl">
      <div class="border-b border-gray-200 p-6">
        <div class="flex items-center justify-between">
          <div>
            <h3 class="text-xl font-semibold text-gray-900">Choose a Process Template</h3>
            <p class="mt-1 text-sm text-gray-500">
              Select a template to get started quickly, or start from scratch
            </p>
          </div>
          <button
            onclick={() => (showTemplateModal = false)}
            class="text-gray-400 hover:text-gray-600"
            aria-label="Close template modal"
          >
            <svg class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </button>
        </div>
      </div>

      <div class="max-h-[calc(85vh-180px)] overflow-y-auto p-6">
        {#each Array.from(groupedTemplates.entries()) as [category, templates]}
          <div class="mb-6">
            <h4 class="mb-3 text-sm font-semibold uppercase tracking-wide text-gray-500">
              {category}
            </h4>
            <div class="grid gap-4 md:grid-cols-2">
              {#each templates as template}
                <button
                  onclick={() => {
                    selectedTemplate = template;
                  }}
                  class="flex flex-col items-start rounded-lg border-2 p-4 text-left transition-colors {selectedTemplate?.id ===
                  template.id
                    ? 'border-blue-500 bg-blue-50'
                    : 'border-gray-200 hover:border-blue-300 hover:bg-gray-50'}"
                >
                  <div class="flex w-full items-center justify-between">
                    <span class="font-medium text-gray-900">{template.name}</span>
                    {#if selectedTemplate?.id === template.id}
                      <svg class="h-5 w-5 text-blue-600" fill="currentColor" viewBox="0 0 20 20">
                        <path
                          fill-rule="evenodd"
                          d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z"
                          clip-rule="evenodd"
                        />
                      </svg>
                    {/if}
                  </div>
                  <p class="mt-1 text-sm text-gray-600">{template.description}</p>
                </button>
              {/each}
            </div>
          </div>
        {/each}
      </div>

      <div class="border-t border-gray-200 bg-gray-50 p-4">
        <div class="flex justify-end gap-3">
          <button
            onclick={() => (showTemplateModal = false)}
            class="rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
          >
            Skip (Start Blank)
          </button>
          <button
            onclick={() => selectedTemplate && loadTemplate(selectedTemplate)}
            disabled={!selectedTemplate}
            class="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:bg-gray-300"
          >
            Use Template
          </button>
        </div>
      </div>
    </div>
  </div>
{/if}

<style>
  /* Override bpmn-js default styles for better integration */
  :global(.djs-palette) {
    background-color: white;
    border: 1px solid #e5e7eb;
  }

  :global(.djs-context-pad) {
    background-color: white;
  }

  :global(.bjs-powered-by) {
    display: none;
  }

  :global(.djs-palette-entries) {
    padding: 8px 0;
  }

  :global(.entry) {
    margin: 2px 4px !important;
  }

  :global(.djs-direct-editing-parent) {
    z-index: 100;
  }

  :global(.djs-overlay-container) {
    z-index: 50;
  }
</style>
