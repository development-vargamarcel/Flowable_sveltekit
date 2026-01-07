// frontend/src/lib/types.ts

export interface User {
  id: string;
  username: string;
  email: string;
}

export interface Task {
  id: string;
  name: string;
  assignee: string | null;
  created: string;
  dueDate: string | null;
  followUpDate: string | null;
}

export interface ProcessInstance {
  id: string;
  processDefinitionId: string;
  processDefinitionKey: string;
  processDefinitionName: string;
  startTime: string;
  startUserId: string;
}
