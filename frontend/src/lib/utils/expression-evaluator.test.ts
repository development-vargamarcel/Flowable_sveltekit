import { describe, it, expect, beforeEach } from 'vitest';
import { ExpressionEvaluator, createDefaultContext, type EvaluationContext } from './expression-evaluator';

describe('ExpressionEvaluator', () => {
  let context: EvaluationContext;
  let evaluator: ExpressionEvaluator;

  beforeEach(() => {
    context = createDefaultContext();
    context.form = {
      amount: 1000,
      status: 'pending',
      category: 'travel',
      items: ['flight', 'hotel'],
      nullField: null,
      undefinedField: undefined,
      zero: 0,
      falseField: false
    };
    context.process = {
      initiator: 'user1',
      priority: 'high'
    };
    context.user = {
      id: 'user1',
      username: 'user1',
      roles: ['user', 'admin'],
      groups: ['engineering']
    };
    evaluator = new ExpressionEvaluator(context);
  });

  describe('Basic Evaluation', () => {
    it('should evaluate boolean literals', () => {
      expect(evaluator.evaluate('true')).toBe(true);
      expect(evaluator.evaluate('false')).toBe(false);
    });

    it('should evaluate number literals', () => {
      expect(evaluator.evaluate('123')).toBe(123);
      expect(evaluator.evaluate('12.34')).toBe(12.34);
    });

    it('should evaluate string literals', () => {
      expect(evaluator.evaluate('"hello"')).toBe('hello');
      expect(evaluator.evaluate("'world'")).toBe('world');
    });

    it('should evaluate null', () => {
      expect(evaluator.evaluate('null')).toBe(null);
    });

    it('should strip ${...} wrappers', () => {
      expect(evaluator.evaluate('${true}')).toBe(true);
      expect(evaluator.evaluate('${123}')).toBe(123);
    });
  });

  describe('Variable Resolution', () => {
    it('should resolve form variables', () => {
      expect(evaluator.evaluate('amount')).toBe(1000);
      expect(evaluator.evaluate('status')).toBe('pending');
      expect(evaluator.evaluate('form.amount')).toBe(1000);
    });

    it('should resolve process variables', () => {
      expect(evaluator.evaluate('initiator')).toBe('user1');
      expect(evaluator.evaluate('process.priority')).toBe('high');
    });

    it('should resolve user context', () => {
      expect(evaluator.evaluate('user.id')).toBe('user1');
      expect(evaluator.evaluate('user.username')).toBe('user1');
    });

    it('should prioritize form over process variables', () => {
      // Add collision
      context.process['amount'] = 999;
      expect(evaluator.evaluate('amount')).toBe(1000); // from form
      expect(evaluator.evaluate('process.amount')).toBe(999);
    });

    it('should return undefined for missing variables', () => {
      expect(evaluator.evaluate('missing')).toBe(undefined);
    });
  });

  describe('Comparisons', () => {
    it('should evaluate equality', () => {
      expect(evaluator.evaluate('amount == 1000')).toBe(true);
      expect(evaluator.evaluate('amount == 999')).toBe(false);
      expect(evaluator.evaluate('status == "pending"')).toBe(true);
    });

    it('should evaluate inequality', () => {
      expect(evaluator.evaluate('amount != 999')).toBe(true);
      expect(evaluator.evaluate('amount != 1000')).toBe(false);
    });

    it('should evaluate greater/less than', () => {
      expect(evaluator.evaluate('amount > 500')).toBe(true);
      expect(evaluator.evaluate('amount < 2000')).toBe(true);
      expect(evaluator.evaluate('amount >= 1000')).toBe(true);
      expect(evaluator.evaluate('amount <= 1000')).toBe(true);
    });

    it('should handle loose equality', () => {
      expect(evaluator.evaluate('amount == "1000"')).toBe(true);
      expect(evaluator.evaluate('falseField == "false"')).toBe(true);
    });
  });

  describe('Logical Operators', () => {
    it('should evaluate AND', () => {
      expect(evaluator.evaluate('amount > 500 && status == "pending"')).toBe(true);
      expect(evaluator.evaluate('amount > 500 && status == "approved"')).toBe(false);
    });

    it('should evaluate OR', () => {
      expect(evaluator.evaluate('amount > 5000 || status == "pending"')).toBe(true);
      expect(evaluator.evaluate('amount > 5000 || status == "approved"')).toBe(false);
    });

    it('should evaluate NOT', () => {
      expect(evaluator.evaluate('!falseField')).toBe(true);
      expect(evaluator.evaluate('!true')).toBe(false);
      expect(evaluator.evaluate('!(amount > 2000)')).toBe(true);
    });

    it('should evaluate complex expressions', () => {
      expect(evaluator.evaluate('(amount > 500 && status == "pending") || process.priority == "low"')).toBe(true);
    });
  });

  describe('Helper Functions', () => {
    it('should evaluate hasRole', () => {
      expect(evaluator.evaluate('hasRole("admin")')).toBe(true);
      expect(evaluator.evaluate('hasRole("guest")')).toBe(false);
    });

    it('should evaluate hasGroup', () => {
      expect(evaluator.evaluate('hasGroup("engineering")')).toBe(true);
      expect(evaluator.evaluate('hasGroup("sales")')).toBe(false);
    });

    it('should evaluate hasAnyRole', () => {
      expect(evaluator.evaluate('hasAnyRole(["guest", "admin"])')).toBe(true);
      expect(evaluator.evaluate('hasAnyRole(["guest", "temp"])')).toBe(false);
    });

    it('should evaluate isEmpty/isNotEmpty', () => {
      expect(evaluator.evaluate('isEmpty(nullField)')).toBe(true);
      expect(evaluator.evaluate('isEmpty(undefinedField)')).toBe(true);
      expect(evaluator.evaluate('isEmpty(amount)')).toBe(false);
      expect(evaluator.evaluate('isNotEmpty(amount)')).toBe(true);
    });

    it('should evaluate array membership with "in"', () => {
       expect(evaluator.evaluate('status in ["pending", "approved"]')).toBe(true);
       expect(evaluator.evaluate('status in ["rejected", "draft"]')).toBe(false);
    });
  });

  describe('Context Update', () => {
      it('should reflect context changes', () => {
          evaluator.updateContext({ form: { ...context.form, amount: 5000 } });
          expect(evaluator.evaluate('amount')).toBe(5000);
      });
  });
});
