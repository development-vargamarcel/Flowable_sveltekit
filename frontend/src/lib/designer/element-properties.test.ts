import { describe, expect, it } from 'vitest';
import {
  defaultElementProperties,
  parseProcessVariablesFromElements,
  parseStoredFormFields,
  parseStoredFormGrids,
  serializeElementProperties,
  updateElementPropertyInModeler
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

  it('normalizes persisted form fields and grid config', () => {
    const result = parseStoredFormFields(
      JSON.stringify([
        {
          name: 'status',
          label: 'Status',
          type: 'select',
          options: 'open,closed',
          _gridConfig: { columns: 4, gap: 8 }
        }
      ])
    );

    expect(result.gridConfig).toEqual({ columns: 4, gap: 8 });
    expect(result.fields[0]).toMatchObject({
      name: 'status',
      options: [
        { value: 'open', label: 'open' },
        { value: 'closed', label: 'closed' }
      ],
      gridColumn: 1,
      gridRow: 1
    });
  });

  it('normalizes persisted form grids and columns', () => {
    expect(
      parseStoredFormGrids(
        JSON.stringify([
          { name: 'items', label: 'Items', columns: [{ name: 'sku', label: 'SKU' }] }
        ]),
        3
      )[0]
    ).toMatchObject({
      name: 'items',
      label: 'Items',
      gridWidth: 3,
      columns: [{ name: 'sku', label: 'SKU', type: 'text', required: false }]
    });
  });

  it('applies property updates through the modeler context', () => {
    const updates: any[] = [];
    const modeler = {
      get: (service: string) =>
        service === 'modeling'
          ? { updateProperties: (_element: any, props: any) => updates.push(props) }
          : { create: (type: string, props: any) => ({ $type: type, ...props }) }
    };
    const elementProperties = defaultElementProperties();
    updateElementPropertyInModeler(
      { modeler, selectedElement: { businessObject: bo({}) }, elementProperties },
      'documentation',
      'Helpful docs'
    );

    expect(updates[0].documentation[0]).toMatchObject({
      $type: 'bpmn:Documentation',
      text: 'Helpful docs'
    });
  });
});
