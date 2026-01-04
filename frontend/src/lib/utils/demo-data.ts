import type { ProcessFieldLibrary, FormField, FormGrid } from '$lib/types';

/**
 * Generates a complex demo configuration for testing advanced form capabilities
 */
export const complexDemoLibrary: ProcessFieldLibrary = {
  fields: [
    {
      id: 'requestor_info_header',
      name: 'requestor_info_header',
      label: 'Requestor Information',
      type: 'header',  // Assuming we implement a header/section type
      gridColumn: 1,
      gridWidth: 2,
      cssClass: 'text-lg font-bold mt-4 mb-2'
    },
    {
      id: 'fullName',
      name: 'fullName',
      label: 'Full Name',
      type: 'text',
      required: true,
      placeholder: 'John Doe',
      validation: {
        minLength: 2,
        maxLength: 100
      },
      gridColumn: 1,
      gridWidth: 1
    },
    {
      id: 'email',
      name: 'email',
      label: 'Email Address',
      type: 'email',
      required: true,
      placeholder: 'john@example.com',
      validation: {
        pattern: '^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$',
        message: 'Invalid email format'
      },
      gridColumn: 2,
      gridWidth: 1
    },
    {
      id: 'department',
      name: 'department',
      label: 'Department',
      type: 'select',
      required: true,
      options: [
        { label: 'Engineering', value: 'eng' },
        { label: 'Sales', value: 'sales' },
        { label: 'Marketing', value: 'mkt' },
        { label: 'HR', value: 'hr' }
      ],
      gridColumn: 1,
      gridWidth: 1
    },
    {
      id: 'manager',
      name: 'manager',
      label: 'Reporting Manager',
      type: 'text',
      required: true,
      logic: {
        type: 'Dependency', // Conditional visibility
        dependencies: ['department'],
        content: "department == 'eng' ? 'Engineering Director' : 'Department Head'"
      },
      gridColumn: 2,
      gridWidth: 1
    },
    {
      id: 'project_details_header',
      name: 'project_details_header',
      label: 'Project Details',
      type: 'header',
      gridColumn: 1,
      gridWidth: 2,
      cssClass: 'text-lg font-bold mt-6 mb-2 border-t pt-4'
    },
    {
      id: 'projectType',
      name: 'projectType',
      label: 'Project Type',
      type: 'radio',
      options: [
        { label: 'Internal', value: 'internal' },
        { label: 'Client', value: 'client' }
      ],
      defaultValue: 'internal',
      gridColumn: 1,
      gridWidth: 2
    },
    {
      id: 'clientName',
      name: 'clientName',
      label: 'Client Name',
      type: 'text',
      required: true,
      logic: {
        type: 'Visibility',
        dependencies: ['projectType'],
        content: "projectType === 'client'"
      },
      gridColumn: 1,
      gridWidth: 1
    },
    {
      id: 'contractValue',
      name: 'contractValue',
      label: 'Contract Value',
      type: 'currency',
      required: true,
      logic: {
        type: 'Visibility',
        dependencies: ['projectType'],
        content: "projectType === 'client'"
      },
      validation: {
        min: 1000
      },
      gridColumn: 2,
      gridWidth: 1
    },
    {
      id: 'description',
      name: 'description',
      label: 'Project Description',
      type: 'textarea',
      required: true,
      gridColumn: 1,
      gridWidth: 2,
      validation: {
        maxLength: 1000
      }
    }
  ],
  grids: [
    {
      id: 'milestones',
      name: 'milestones',
      label: 'Project Milestones',
      description: 'Key deliverables and dates',
      minRows: 1,
      maxRows: 10,
      columns: [
        {
          id: 'm_name',
          name: 'name',
          label: 'Milestone Name',
          type: 'text',
          required: true
        },
        {
          id: 'm_date',
          name: 'dueDate',
          label: 'Due Date',
          type: 'date',
          required: true
        },
        {
          id: 'm_status',
          name: 'status',
          label: 'Status',
          type: 'select',
          options: [
            { label: 'Pending', value: 'pending' },
            { label: 'In Progress', value: 'in_progress' },
            { label: 'Completed', value: 'completed' }
          ]
        }
      ]
    }
  ]
};
