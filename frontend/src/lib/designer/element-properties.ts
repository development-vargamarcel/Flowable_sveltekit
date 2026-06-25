export type ElementProperties = {
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
};

export const defaultElementProperties = (): ElementProperties => ({
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

export function getMultiInstanceType(businessObject: any): string {
  const loopCharacteristics = businessObject.loopCharacteristics;
  if (!loopCharacteristics) return 'none';
  if (loopCharacteristics.$type === 'bpmn:MultiInstanceLoopCharacteristics')
    return loopCharacteristics.isSequential ? 'sequential' : 'parallel';
  return 'none';
}
export function getLoopCardinality(businessObject: any): string {
  return businessObject.loopCharacteristics?.loopCardinality?.body || '';
}
export function getCollection(businessObject: any): string {
  return businessObject.loopCharacteristics?.get?.('flowable:collection') || '';
}
export function getElementVariable(businessObject: any): string {
  return businessObject.loopCharacteristics?.get?.('flowable:elementVariable') || '';
}
export function getCompletionCondition(businessObject: any): string {
  return businessObject.loopCharacteristics?.completionCondition?.body || '';
}
export function getTimerDefinition(businessObject: any, type: string): string {
  const eventDefinitions = businessObject.eventDefinitions;
  if (eventDefinitions?.[0]?.$type === 'bpmn:TimerEventDefinition')
    return eventDefinitions[0][type]?.body || '';
  return '';
}

export function serializeElementProperties(element: any): ElementProperties {
  if (!element?.businessObject) return defaultElementProperties();
  const bo = element.businessObject;
  return {
    id: bo.id || '',
    name: bo.name || '',
    type: element.type,
    assignee: bo.get('flowable:assignee') || '',
    candidateGroups: bo.get('flowable:candidateGroups') || '',
    candidateUsers: bo.get('flowable:candidateUsers') || '',
    formKey: bo.get('flowable:formKey') || '',
    documentation: bo.documentation?.[0]?.text || '',
    scriptFormat: bo.scriptFormat || 'javascript',
    script: bo.script?.body || bo.script || '',
    conditionExpression: bo.conditionExpression?.body || '',
    dueDate: bo.get('flowable:dueDate') || '',
    priority: bo.get('flowable:priority') || '',
    category: bo.get('flowable:category') || '',
    asyncBefore:
      bo.get('flowable:asyncBefore') === 'true' || bo.get('flowable:asyncBefore') === true,
    asyncAfter: bo.get('flowable:asyncAfter') === 'true' || bo.get('flowable:asyncAfter') === true,
    exclusive: bo.get('flowable:exclusive') !== 'false',
    skipExpression: bo.get('flowable:skipExpression') || '',
    implementation: bo.get('flowable:class')
      ? 'class'
      : bo.get('flowable:delegateExpression')
        ? 'delegateExpression'
        : bo.get('flowable:expression')
          ? 'expression'
          : 'class',
    expression: bo.get('flowable:expression') || '',
    delegateExpression: bo.get('flowable:delegateExpression') || '',
    resultVariable: bo.get('flowable:resultVariable') || '',
    class: bo.get('flowable:class') || '',
    multiInstanceType: getMultiInstanceType(bo),
    loopCardinality: getLoopCardinality(bo),
    collection: getCollection(bo),
    elementVariable: getElementVariable(bo),
    completionCondition: getCompletionCondition(bo),
    documentType: bo.get('flowable:documentType') || '',
    timeDate: getTimerDefinition(bo, 'timeDate'),
    timeDuration: getTimerDefinition(bo, 'timeDuration'),
    timeCycle: getTimerDefinition(bo, 'timeCycle')
  };
}

export function parseProcessVariablesFromElements(elements: any[]): string[] {
  const variables = new Set<string>(['initiator']);
  for (const element of elements) {
    const bo = element.businessObject;
    if (!bo) continue;
    for (const attr of ['flowable:formFields', 'flowable:formGrids']) {
      const json = bo.get?.(attr);
      if (!json) continue;
      try {
        const parsed = JSON.parse(json);
        if (attr.endsWith('formFields'))
          parsed.forEach((f: any) => f.name && variables.add(f.name));
        else
          parsed.forEach((g: any) => {
            if (g.name) {
              variables.add(g.name);
              g.columns?.forEach((c: any) => c.name && variables.add(`${g.name}_${c.name}`));
            }
          });
      } catch {
        /* ignore malformed extension data */
      }
    }
    const script = bo.script?.body || bo.script;
    script?.match?.(/execution\.(?:get|set)Variable\(['"][^'"]+['"]/g)?.forEach((m: string) => {
      const v = m.match(/\(['"]([^'"]+)['"]/);
      if (v) variables.add(v[1]);
    });
  }
  return Array.from(variables).sort();
}

export type ModelerPropertyContext = {
  modeler: any;
  selectedElement: any;
  elementProperties: ElementProperties;
  scriptFormat?: string;
};

export function parseStoredFormFields(json: unknown): {
  fields: any[];
  gridConfig?: { columns?: number; gap?: number };
} {
  if (!json || typeof json !== 'string') return { fields: [] };
  try {
    const parsed = JSON.parse(json);
    if (!Array.isArray(parsed)) return { fields: [] };
    return {
      fields: parsed.map((field: any, index: number) => ({
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
        visibilityExpression: field.visibilityExpression || '',
        gridColumn: field.gridColumn || 1,
        gridRow: field.gridRow || index + 1,
        gridWidth: field.gridWidth || 1,
        cssClass: field.cssClass || '',
        onChange: field.onChange || '',
        onBlur: field.onBlur || ''
      })),
      gridConfig: parsed[0]?._gridConfig
    };
  } catch {
    return { fields: [] };
  }
}

export function parseStoredFormGrids(json: unknown, defaultGridWidth = 2): any[] {
  if (!json || typeof json !== 'string') return [];
  try {
    const parsed = JSON.parse(json);
    if (!Array.isArray(parsed)) return [];
    return parsed.map((grid: any, index: number) => ({
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
      gridWidth: grid.gridWidth || defaultGridWidth,
      cssClass: grid.cssClass || '',
      visibilityExpression: grid.visibilityExpression || ''
    }));
  } catch {
    return [];
  }
}

export function updateMultiInstanceTypeInModeler(ctx: ModelerPropertyContext, type: string) {
  const modeling = ctx.modeler.get('modeling') as any;
  const moddle = ctx.modeler.get('moddle') as any;
  if (type === 'none') {
    modeling.updateProperties(ctx.selectedElement, { loopCharacteristics: undefined });
    return;
  }
  modeling.updateProperties(ctx.selectedElement, {
    loopCharacteristics: moddle.create('bpmn:MultiInstanceLoopCharacteristics', {
      isSequential: type === 'sequential'
    })
  });
}

export function updateMultiInstancePropertyInModeler(
  ctx: ModelerPropertyContext,
  property: string,
  value: string
) {
  const modeling = ctx.modeler.get('modeling') as any;
  const moddle = ctx.modeler.get('moddle') as any;
  const loopCharacteristics = ctx.selectedElement.businessObject.loopCharacteristics;
  if (!loopCharacteristics) return;
  if (property === 'loopCardinality' || property === 'completionCondition') {
    loopCharacteristics[property] = value
      ? moddle.create('bpmn:FormalExpression', { body: value })
      : undefined;
  } else if (property === 'collection') {
    loopCharacteristics.set('flowable:collection', value || undefined);
  } else if (property === 'elementVariable') {
    loopCharacteristics.set('flowable:elementVariable', value || undefined);
  }
  modeling.updateProperties(ctx.selectedElement, { loopCharacteristics });
}

export function updateTimerDefinitionInModeler(
  ctx: ModelerPropertyContext,
  property: string,
  value: string
) {
  const modeling = ctx.modeler.get('modeling') as any;
  const moddle = ctx.modeler.get('moddle') as any;
  const timerDef = ctx.selectedElement.businessObject.eventDefinitions?.[0];
  if (!timerDef || timerDef.$type !== 'bpmn:TimerEventDefinition') return;
  modeling.updateModdleProperties(ctx.selectedElement, timerDef, {
    [property]: value ? moddle.create('bpmn:FormalExpression', { body: value }) : undefined
  });
}

export function updateElementPropertyInModeler(
  ctx: ModelerPropertyContext,
  property: string,
  value: string | boolean
) {
  const modeling = ctx.modeler.get('modeling') as any;
  const moddle = ctx.modeler.get('moddle') as any;
  const businessObject = ctx.selectedElement.businessObject;
  if (property in ctx.elementProperties) (ctx.elementProperties as any)[property] = value;
  if (property === 'name' || property === 'id')
    modeling.updateProperties(ctx.selectedElement, { [property]: value });
  else if (property === 'documentation') {
    const documentation = businessObject.documentation;
    if (!value) modeling.updateProperties(ctx.selectedElement, { documentation: undefined });
    else if (!documentation?.length)
      modeling.updateProperties(ctx.selectedElement, {
        documentation: [moddle.create('bpmn:Documentation', { text: value as string })]
      });
    else {
      documentation[0].text = value as string;
      modeling.updateProperties(ctx.selectedElement, { documentation: [...documentation] });
    }
  } else if (property === 'conditionExpression') {
    modeling.updateProperties(ctx.selectedElement, {
      conditionExpression: value
        ? moddle.create('bpmn:FormalExpression', { body: value as string })
        : undefined
    });
  } else if (property === 'script')
    modeling.updateProperties(ctx.selectedElement, {
      script: value as string,
      scriptFormat: ctx.scriptFormat
    });
  else if (property === 'scriptFormat')
    modeling.updateProperties(ctx.selectedElement, {
      scriptFormat: value as string,
      script: ctx.elementProperties.script
    });
  else if (['asyncBefore', 'asyncAfter', 'exclusive'].includes(property))
    modeling.updateProperties(ctx.selectedElement, {
      [`flowable:${property}`]: value ? 'true' : 'false'
    });
  else if (property === 'multiInstanceType') updateMultiInstanceTypeInModeler(ctx, value as string);
  else if (
    ['loopCardinality', 'collection', 'elementVariable', 'completionCondition'].includes(property)
  )
    updateMultiInstancePropertyInModeler(ctx, property, value as string);
  else if (['timeDate', 'timeDuration', 'timeCycle'].includes(property))
    updateTimerDefinitionInModeler(ctx, property, value as string);
  else if (property.startsWith('flowable:'))
    modeling.updateProperties(ctx.selectedElement, { [property]: value });
  else modeling.updateProperties(ctx.selectedElement, { [`flowable:${property}`]: value });
}
