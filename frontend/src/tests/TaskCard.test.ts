import { render, screen } from '@testing-library/svelte';
import { describe, it, expect } from 'vitest';
import TaskCard from '../lib/components/TaskCard.svelte';
import type { Task } from '../lib/types';

describe('TaskCard Component', () => {
  const mockTask: Task = {
    id: 'task-123',
    name: 'Approve Expense Report',
    description: 'Please review the submitted expense report for Q1.',
    priority: 50,
    createTime: new Date().toISOString(),
    processInstanceId: 'proc-456',
    processDefinitionId: 'proc-def-789',
    taskDefinitionKey: 'approveTask',
    processName: 'Expense Approval',
    businessKey: 'ER-2024-001'
  };

  it('should render task name and process information', () => {
    render(TaskCard, { task: mockTask });

    expect(screen.getByText('Approve Expense Report')).toBeTruthy();
    expect(screen.getByText('Expense Approval')).toBeTruthy();
    expect(screen.getByText('Ref: ER-2024-001')).toBeTruthy();
  });

  it('should render priority label correctly', () => {
    render(TaskCard, { task: mockTask });
    expect(screen.getByText('Medium')).toBeTruthy();
  });

  it('should show claim button for unassigned task', () => {
    const unassignedTask = { ...mockTask, assignee: undefined };
    render(TaskCard, { task: unassignedTask });

    expect(screen.getByText('Claim')).toBeTruthy();
  });

  it('should show Unassigned status when no assignee', () => {
    const unassignedTask = { ...mockTask, assignee: undefined };
    render(TaskCard, { task: unassignedTask });

    expect(screen.getByText('Unassigned')).toBeTruthy();
  });
});
