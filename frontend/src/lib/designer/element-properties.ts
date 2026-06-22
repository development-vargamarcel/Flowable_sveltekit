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
