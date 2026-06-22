import { describe, expect, it } from 'vitest';
import { validateBpmnElements } from './modeler';

const bo = (id: string, props: Record<string, any> = {}) => ({
  id,
  ...props,
  get: (key: string) => props[key]
});

describe('designer BPMN validation', () => {
  it('requires process-level start and end events', () => {
    expect(validateBpmnElements([])).toMatchObject({
      valid: false,
      errors: [
        'Process must have at least one Start Event',
        'Process must have at least one End Event'
      ]
    });
  });

  it('warns for unassigned user tasks and undefined expression variables', () => {
    const start = {
      type: 'bpmn:StartEvent',
      businessObject: bo('start'),
      incoming: [],
      outgoing: [{}]
    };
    const task = {
      type: 'bpmn:UserTask',
      businessObject: bo('approve', { 'flowable:assignee': '${manager}' }),
      incoming: [{}],
      outgoing: [{}]
    };
    const end = { type: 'bpmn:EndEvent', businessObject: bo('end'), incoming: [{}], outgoing: [] };
    const result = validateBpmnElements([start, task, end], ['initiator']);
    expect(result.valid).toBe(true);
    expect(result.warnings).toContain(
      'Element "approve": Variable \'manager\' used in flowable:assignee but not defined in process'
    );
  });
});
