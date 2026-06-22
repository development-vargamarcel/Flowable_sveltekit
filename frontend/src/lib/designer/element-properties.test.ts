import { describe, expect, it } from 'vitest';
import {
  parseProcessVariablesFromElements,
  serializeElementProperties
} from './element-properties';

const bo = (props: Record<string, any>) => ({
  id: props.id,
  name: props.name,
  ...props,
  get: (key: string) => props[key]
});

describe('designer element property helpers', () => {
  it('serializes Flowable attributes into panel state', () => {
    expect(
      serializeElementProperties({
        type: 'bpmn:UserTask',
        businessObject: bo({
          id: 'Task_1',
          name: 'Approve',
          'flowable:assignee': '${manager}',
          'flowable:asyncBefore': 'true',
          'flowable:exclusive': 'false'
        })
      })
    ).toMatchObject({
      id: 'Task_1',
      name: 'Approve',
      assignee: '${manager}',
      asyncBefore: true,
      exclusive: false
    });
  });

  it('extracts variables from fields, grids, and scripts', () => {
    expect(
      parseProcessVariablesFromElements([
        { businessObject: bo({ 'flowable:formFields': JSON.stringify([{ name: 'amount' }]) }) },
        {
          businessObject: bo({
            'flowable:formGrids': JSON.stringify([{ name: 'items', columns: [{ name: 'sku' }] }]),
            script: "execution.setVariable('approved', true);"
          })
        }
      ])
    ).toEqual(['amount', 'approved', 'initiator', 'items', 'items_sku']);
  });
});
