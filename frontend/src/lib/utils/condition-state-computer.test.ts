import { describe, it, expect, beforeEach } from 'vitest';
import { ConditionStateComputer, createStateComputer } from './condition-state-computer';
import { createDefaultContext, type EvaluationContext } from './expression-evaluator';
import type { FormField, FormGrid, FieldConditionRule, GridColumn } from '$lib/types';

describe('ConditionStateComputer', () => {
  let context: EvaluationContext;
  let computer: ConditionStateComputer;

  beforeEach(() => {
    context = createDefaultContext();
    context.form = {
      amount: 1000,
      status: 'pending'
    };
    computer = createStateComputer(context);
  });

  const createField = (name: string, overrides: Partial<FormField> = {}): FormField => ({
    id: name,
    name,
    type: 'text',
    label: name,
    required: false,
    readonly: false,
    hidden: false,
    gridColumn: 1,
    gridRow: 1,
    gridWidth: 1,
    ...overrides
  });

  const createRule = (id: string, condition: string, effect: 'hidden'|'readonly'|'visible'|'editable', overrides: Partial<FieldConditionRule> = {}): FieldConditionRule => ({
    id,
    name: id,
    condition,
    effect,
    target: { type: 'all' },
    priority: 0,
    enabled: true,
    ...overrides
  });

  describe('Field State Computation', () => {
    it('should respect default state', () => {
      const field = createField('testField', { hidden: false, readonly: false });
      const state = computer.computeFieldState(field, []);
      expect(state.isHidden).toBe(false);
      expect(state.isReadonly).toBe(false);
    });

    it('should apply legacy hiddenExpression', () => {
      const field = createField('testField', { hiddenExpression: 'amount > 500' });
      const state = computer.computeFieldState(field, []);
      expect(state.isHidden).toBe(true);
      expect(state.appliedRules).toContain('field:hiddenExpression');
    });

    it('should apply hidden rule', () => {
      const field = createField('testField');
      const rule = createRule('r1', 'amount > 500', 'hidden');
      const state = computer.computeFieldState(field, [rule]);
      expect(state.isHidden).toBe(true);
      expect(state.appliedRules).toContain('r1');
    });

    it('should apply readonly rule', () => {
      const field = createField('testField');
      const rule = createRule('r1', 'amount > 500', 'readonly');
      const state = computer.computeFieldState(field, [rule]);
      expect(state.isReadonly).toBe(true);
    });

    it('should follow least access wins (hidden wins over visible)', () => {
      const field = createField('testField', { hidden: true });
      const ruleVisible = createRule('r1', 'true', 'visible');
      const ruleHidden = createRule('r2', 'true', 'hidden');

      const state = computer.computeFieldState(field, [ruleVisible, ruleHidden]);
      expect(state.isHidden).toBe(true);
    });

    it('should allow visible rule to override default hidden if no other hidden rule matches', () => {
      const field = createField('testField', { hidden: true });
      const ruleVisible = createRule('r1', 'true', 'visible');

      const state = computer.computeFieldState(field, [ruleVisible]);
      expect(state.isHidden).toBe(false);
    });

    it('should follow least access wins (readonly wins over editable)', () => {
      const field = createField('testField', { readonly: true });
      const ruleEditable = createRule('r1', 'true', 'editable');
      const ruleReadonly = createRule('r2', 'true', 'readonly');

      const state = computer.computeFieldState(field, [ruleEditable, ruleReadonly]);
      expect(state.isReadonly).toBe(true);
    });

    it('should allow editable rule to override default readonly if no other readonly rule matches', () => {
      const field = createField('testField', { readonly: true });
      const ruleEditable = createRule('r1', 'true', 'editable');

      const state = computer.computeFieldState(field, [ruleEditable]);
      expect(state.isReadonly).toBe(false);
    });
  });

  describe('Grid State Computation', () => {
    const createGrid = (name: string, columns: GridColumn[]): FormGrid => ({
      id: name,
      name,
      label: name,
      columns,
      gridColumn: 1,
      gridRow: 1,
      gridWidth: 1,
      minRows: 0,
      maxRows: 0
    });

    it('should compute grid visibility', () => {
      const grid = createGrid('grid1', []);
      const rule = createRule('r1', 'true', 'hidden', { target: { type: 'grid', gridNames: ['grid1'] } });
      const state = computer.computeGridState(grid, [rule]);
      expect(state.isHidden).toBe(true);
    });

    it('should compute column visibility', () => {
      const col1: GridColumn = { id: 'c1', name: 'col1', type: 'text', label: 'Col 1', required: false };
      const grid = createGrid('grid1', [col1]);

      const rule = createRule('r1', 'true', 'hidden', {
        target: {
          type: 'column',
          columnTargets: [{ gridName: 'grid1', columnNames: ['col1'] }]
        }
      });

      const state = computer.computeGridState(grid, [rule]);
      expect(state.columnStates['col1'].isHidden).toBe(true);
    });
  });
});
