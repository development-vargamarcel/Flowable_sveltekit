import BpmnModeler from 'bpmn-js/lib/Modeler';
// @ts-expect-error missing bundled declarations for bpmn-js-token-simulation
import TokenSimulationModule from 'bpmn-js-token-simulation';
import { flowableModdle } from '$lib/utils/flowable-moddle';

export function createDesignerModeler(container: HTMLElement, bindTo: Document = document) {
  return new BpmnModeler({
    container,
    keyboard: { bindTo },
    additionalModules: [TokenSimulationModule],
    moddleExtensions: { flowable: flowableModdle }
  });
}

export function validateBpmnElements(allElements: any[], processVariables: string[] = []) {
  const errors: string[] = [];
  const warnings: string[] = [];
  let hasStartEvent = false;
  let hasEndEvent = false;
  const disconnectedElements: string[] = [];
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
  const noIncomingRequired = new Set([
    'bpmn:StartEvent',
    'bpmn:BoundaryEvent',
    'bpmn:EventBasedGateway'
  ]);
  const noOutgoingRequired = new Set([
    'bpmn:EndEvent',
    'bpmn:IntermediateThrowEvent',
    'bpmn:CompensateEventDefinition'
  ]);
  const isInsideSubProcess = (element: any) => {
    let parent = element.parent;
    while (parent) {
      if (['bpmn:SubProcess', 'bpmn:Transaction', 'bpmn:AdHocSubProcess'].includes(parent.type))
        return true;
      parent = parent.parent;
    }
    return false;
  };
  const isCollapsedSubProcess = (element: any) =>
    ['bpmn:SubProcess', 'bpmn:Transaction', 'bpmn:AdHocSubProcess'].includes(element.type) &&
    (element.collapsed === true || element.businessObject?.di?.isExpanded === false);
  const isTerminatingThrowEvent = (bo: any) =>
    bo?.eventDefinitions?.some((ed: any) =>
      [
        'bpmn:TerminateEventDefinition',
        'bpmn:EscalationEventDefinition',
        'bpmn:CompensateEventDefinition',
        'bpmn:SignalEventDefinition',
        'bpmn:MessageEventDefinition'
      ].includes(ed.$type)
    );
  for (const element of allElements) {
    if (element.type === 'bpmn:StartEvent' && !isInsideSubProcess(element)) hasStartEvent = true;
    if (element.type === 'bpmn:EndEvent' && !isInsideSubProcess(element)) hasEndEvent = true;
  }
  for (const element of allElements) {
    const type = element.type;
    const bo = element.businessObject;
    if (nonFlowElementTypes.has(type) || isCollapsedSubProcess(element)) continue;
    const incoming = element.incoming || [];
    const outgoing = element.outgoing || [];
    if (type === 'bpmn:IntermediateThrowEvent') {
      if (!isTerminatingThrowEvent(bo) && outgoing.length === 0)
        disconnectedElements.push(
          `${type.replace('bpmn:', '')} "${bo?.name || bo?.id}" has no outgoing connections`
        );
      if (incoming.length === 0)
        disconnectedElements.push(
          `${type.replace('bpmn:', '')} "${bo?.name || bo?.id}" has no incoming connections`
        );
      continue;
    }
    if (!noIncomingRequired.has(type) && incoming.length === 0 && type !== 'bpmn:BoundaryEvent')
      disconnectedElements.push(
        `${type.replace('bpmn:', '')} "${bo?.name || bo?.id}" has no incoming connections`
      );
    if (!noOutgoingRequired.has(type) && outgoing.length === 0)
      disconnectedElements.push(
        `${type.replace('bpmn:', '')} "${bo?.name || bo?.id}" has no outgoing connections`
      );
    if (type === 'bpmn:BoundaryEvent' && outgoing.length === 0)
      disconnectedElements.push(
        `Boundary Event "${bo?.name || bo?.id}" has no outgoing connections`
      );
    if (
      type === 'bpmn:UserTask' &&
      !bo?.get?.('flowable:assignee') &&
      !bo?.get?.('flowable:candidateGroups') &&
      !bo?.get?.('flowable:candidateUsers')
    )
      warnings.push(`User Task "${bo?.name || bo?.id}" has no assignee or candidates defined`);
    if (type === 'bpmn:ScriptTask' && !bo?.script)
      warnings.push(`Script Task "${bo?.name || bo?.id}" has no script defined`);
    if (['bpmn:ExclusiveGateway', 'bpmn:InclusiveGateway'].includes(type) && outgoing.length > 1) {
      const hasDefault = bo?.default;
      const conditioned = outgoing.filter(
        (conn: any) =>
          conn.businessObject?.conditionExpression || conn.businessObject === hasDefault
      );
      if (conditioned.length !== outgoing.length)
        warnings.push(
          `${type.replace('bpmn:', '').replace('Gateway', ' Gateway')} "${bo?.name || bo?.id}" has flows without conditions`
        );
    }
    [
      'conditionExpression',
      'flowable:assignee',
      'flowable:skipExpression',
      'flowable:collection',
      'flowable:elementVariable'
    ].forEach((prop) => {
      const val =
        prop === 'conditionExpression'
          ? bo?.conditionExpression?.body || ''
          : bo?.get?.(prop) || '';
      val?.match?.(/\$\{([a-zA-Z0-9_]+)\}/g)?.forEach((m: string) => {
        const v = m.slice(2, -1);
        if (
          !processVariables.includes(v) &&
          !['initiator', 'execution', 'task', 'authenticatedUserId'].includes(v)
        )
          warnings.push(
            `Element "${bo?.name || bo?.id}": Variable '${v}' used in ${prop} but not defined in process`
          );
      });
    });
  }
  if (!hasStartEvent) errors.push('Process must have at least one Start Event');
  if (!hasEndEvent) errors.push('Process must have at least one End Event');
  if (disconnectedElements.length)
    errors.push(
      ...(disconnectedElements.length <= 3
        ? disconnectedElements
        : [
            `${disconnectedElements.length} elements have connection issues:`,
            ...disconnectedElements.slice(0, 3),
            `...and ${disconnectedElements.length - 3} more`
          ])
    );
  return { valid: errors.length === 0, errors, warnings };
}

export function validateBpmnModeler(modeler: any, processVariables: string[] = []) {
  if (!modeler) return { valid: false, errors: ['Modeler not initialized'], warnings: [] };
  const elements: any[] = [];
  modeler.get('elementRegistry').forEach((element: any) => elements.push(element));
  return validateBpmnElements(elements, processVariables);
}
