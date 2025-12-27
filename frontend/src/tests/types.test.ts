import { describe, it, expect } from 'vitest';
import type {
  User,
  Task,
  TaskDetails,
  ProcessDefinition,
  ProcessInstance,
  LoginRequest
} from '$lib/types';

describe('Type definitions', () => {
  describe('User', () => {
    it('should accept valid user data', () => {
      const user: User = {
        username: 'testuser',
        displayName: 'Test User',
        email: 'test@example.com',
        roles: ['ROLE_USER']
      };

      expect(user.username).toBe('testuser');
      expect(user.roles).toContain('ROLE_USER');
    });

    it('should accept null email', () => {
      const user: User = {
        username: 'testuser',
        displayName: 'Test User',
        email: null,
        roles: []
      };

      expect(user.email).toBeNull();
    });
  });

  describe('Task', () => {
    it('should accept valid task data', () => {
      const task: Task = {
        id: 'task-123',
        name: 'Approve Request',
        description: 'Approve the expense request',
        processInstanceId: 'proc-456',
        processDefinitionKey: 'expense-approval',
        processName: 'Expense Approval',
        assignee: 'supervisor1',
        owner: 'user1',
        createTime: '2024-01-01T10:00:00Z',
        dueDate: '2024-01-05T10:00:00Z',
        priority: 50,
        formKey: 'approval-form',
        variables: { amount: 500, category: 'Travel' },
        businessKey: 'EXP-2024-001'
      };

      expect(task.id).toBe('task-123');
      expect(task.priority).toBe(50);
      expect(task.variables.amount).toBe(500);
    });

    it('should accept null optional fields', () => {
      const task: Task = {
        id: 'task-123',
        name: 'Simple Task',
        description: null,
        processInstanceId: 'proc-456',
        processDefinitionKey: 'simple-process',
        processName: 'Simple Process',
        assignee: null,
        owner: null,
        createTime: '2024-01-01T10:00:00Z',
        dueDate: null,
        priority: 0,
        formKey: null,
        variables: {},
        businessKey: null
      };

      expect(task.assignee).toBeNull();
      expect(task.dueDate).toBeNull();
    });
  });

  describe('TaskDetails', () => {
    it('should contain task and variables', () => {
      const taskDetails: TaskDetails = {
        task: {
          id: 'task-123',
          name: 'Test Task',
          description: null,
          processInstanceId: 'proc-456',
          processDefinitionKey: 'test',
          processName: 'Test Process',
          assignee: null,
          owner: null,
          createTime: '2024-01-01T10:00:00Z',
          dueDate: null,
          priority: 50,
          formKey: null,
          variables: {},
          businessKey: null
        },
        variables: {
          customVar: 'value'
        }
      };

      expect(taskDetails.task.id).toBe('task-123');
      expect(taskDetails.variables.customVar).toBe('value');
    });
  });

  describe('ProcessDefinition', () => {
    it('should accept valid process definition', () => {
      const processDef: ProcessDefinition = {
        key: 'expense-approval',
        name: 'Expense Approval Process',
        description: 'Handles expense approvals',
        version: 1
      };

      expect(processDef.key).toBe('expense-approval');
      expect(processDef.version).toBe(1);
    });
  });

  describe('ProcessInstance', () => {
    it('should accept valid process instance', () => {
      const processInstance: ProcessInstance = {
        id: 'proc-123',
        processDefinitionKey: 'expense-approval',
        processDefinitionName: 'Expense Approval',
        businessKey: 'EXP-2024-001',
        startTime: '2024-01-01T10:00:00Z',
        startUserId: 'user1',
        variables: { amount: 1000 },
        ended: false
      };

      expect(processInstance.id).toBe('proc-123');
      expect(processInstance.ended).toBe(false);
    });
  });

  describe('LoginRequest', () => {
    it('should accept valid login credentials', () => {
      const loginRequest: LoginRequest = {
        username: 'user1',
        password: 'password123'
      };

      expect(loginRequest.username).toBe('user1');
      expect(loginRequest.password).toBe('password123');
    });
  });
});
