// Flowable BPMN moddle extension descriptor
// This defines the Flowable namespace and extension attributes for bpmn-js

export const flowableModdle = {
  name: 'Flowable',
  uri: 'http://flowable.org/bpmn',
  prefix: 'flowable',
  xml: {
    tagAlias: 'lowerCase'
  },
  types: [
    {
      name: 'Assignable',
      extends: ['bpmn:UserTask'],
      properties: [
        {
          name: 'assignee',
          isAttr: true,
          type: 'String'
        },
        {
          name: 'candidateUsers',
          isAttr: true,
          type: 'String'
        },
        {
          name: 'candidateGroups',
          isAttr: true,
          type: 'String'
        },
        {
          name: 'dueDate',
          isAttr: true,
          type: 'String'
        },
        {
          name: 'priority',
          isAttr: true,
          type: 'String'
        },
        {
          name: 'category',
          isAttr: true,
          type: 'String'
        },
        {
          name: 'formKey',
          isAttr: true,
          type: 'String'
        },
        {
          name: 'formFields',
          isAttr: true,
          type: 'String'
        },
        {
          name: 'formGrids',
          isAttr: true,
          type: 'String'
        }
      ]
    },
    {
      name: 'AsyncCapable',
      extends: ['bpmn:Activity', 'bpmn:Gateway', 'bpmn:Event'],
      properties: [
        {
          name: 'asyncBefore',
          isAttr: true,
          type: 'String'
        },
        {
          name: 'asyncAfter',
          isAttr: true,
          type: 'String'
        },
        {
          name: 'exclusive',
          isAttr: true,
          type: 'String'
        }
      ]
    },
    {
      name: 'ServiceTaskLike',
      extends: ['bpmn:ServiceTask', 'bpmn:SendTask'],
      properties: [
        {
          name: 'class',
          isAttr: true,
          type: 'String'
        },
        {
          name: 'delegateExpression',
          isAttr: true,
          type: 'String'
        },
        {
          name: 'expression',
          isAttr: true,
          type: 'String'
        },
        {
          name: 'resultVariable',
          isAttr: true,
          type: 'String'
        }
      ]
    },
    {
      name: 'Skippable',
      extends: ['bpmn:Activity'],
      properties: [
        {
          name: 'skipExpression',
          isAttr: true,
          type: 'String'
        }
      ]
    },
    {
      name: 'MultiInstanceLoopCharacteristicsExtension',
      extends: ['bpmn:MultiInstanceLoopCharacteristics'],
      properties: [
        {
          name: 'collection',
          isAttr: true,
          type: 'String'
        },
        {
          name: 'elementVariable',
          isAttr: true,
          type: 'String'
        }
      ]
    },
    {
      name: 'StartEventExtension',
      extends: ['bpmn:StartEvent'],
      properties: [
        {
          name: 'formKey',
          isAttr: true,
          type: 'String'
        },
        {
          name: 'formFields',
          isAttr: true,
          type: 'String'
        },
        {
          name: 'formGrids',
          isAttr: true,
          type: 'String'
        },
        {
          name: 'initiator',
          isAttr: true,
          type: 'String'
        }
      ]
    }
  ],
  enumerations: [],
  associations: []
};
