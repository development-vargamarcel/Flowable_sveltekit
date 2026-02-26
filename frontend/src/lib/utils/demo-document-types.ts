export const demoDocumentTypes = [
  {
    key: 'customer-logic-demo',
    name: 'Customer Logic Demo',
    description:
      'Showcases SQL and JS logic features. Auto-fetches customer data from DEMO tables and calculates discounts.',
    schema: {
      fields: [
        {
          id: 'f_cust_id',
          name: 'customerId',
          label: 'Customer ID (Try 1, 2, or 3)',
          type: 'number',
          required: true,
          placeholder: 'Enter ID...',
          validation: null,
          logic: null
        },
        {
          id: 'f_cust_name',
          name: 'customerName',
          label: 'Customer Name (SQL Lookups)',
          type: 'text',
          readonly: true,
          validation: null,
          logic: {
            type: 'SQL',
            content: 'SELECT name FROM DEMO_CUSTOMERS WHERE id = ${form.customerId}',
            dependencies: ['customerId'],
            autoCalculate: true
          }
        },
        {
          id: 'f_cust_email',
          name: 'customerEmail',
          label: 'Email',
          type: 'email',
          readonly: true,
          validation: null,
          logic: {
            type: 'SQL',
            content: 'SELECT email FROM DEMO_CUSTOMERS WHERE id = ${form.customerId}',
            dependencies: ['customerId'],
            autoCalculate: true
          }
        },
        {
          id: 'f_cust_segment',
          name: 'segment',
          label: 'Segment',
          type: 'text',
          readonly: true,
          validation: null,
          logic: {
            type: 'SQL',
            content: 'SELECT segment FROM DEMO_CUSTOMERS WHERE id = ${form.customerId}',
            dependencies: ['customerId'],
            autoCalculate: true
          }
        },
        {
          id: 'f_discount',
          name: 'discount',
          label: 'Calculated Discount (JS Logic)',
          type: 'percentage',
          readonly: true,
          validation: null,
          logic: {
            type: 'JS',
            content: `
              // Calculate discount based on segment
              if (form.segment === 'Enterprise') return 20;
              if (form.segment === 'SMB') return 10;
              if (form.segment === 'Individual') return 5;
              return 0;
            `,
            dependencies: ['segment'],
            autoCalculate: true
          }
        }
      ],
      grids: []
    }
  },
  {
    key: 'order-grid-demo',
    name: 'Order Grid Calculation',
    description: 'Showcases grid cell dependencies and row-level calculations.',
    schema: {
      fields: [
        {
          id: 'f_order_date',
          name: 'orderDate',
          label: 'Order Date',
          type: 'date',
          required: true
        }
      ],
      grids: [
        {
          id: 'g_items',
          name: 'items',
          label: 'Order Items',
          description: 'Add items and see totals calculate automatically',
          minRows: 1,
          maxRows: 10,
          gridWidth: 12,
          columns: [
            {
              id: 'c_prod',
              name: 'product',
              label: 'Product',
              type: 'text',
              required: true
            },
            {
              id: 'c_price',
              name: 'price',
              label: 'Price',
              type: 'number',
              step: 0.01,
              required: true
            },
            {
              id: 'c_qty',
              name: 'quantity',
              label: 'Quantity',
              type: 'number',
              step: 1,
              required: true
            },
            {
              id: 'c_total',
              name: 'total',
              label: 'Total (Calculated)',
              type: 'number',
              step: 0.01,
              readonly: true,
              logic: {
                type: 'JS',
                content: 'return (Number(row.price) || 0) * (Number(row.quantity) || 0);',
                dependencies: ['price', 'quantity'],
                autoCalculate: true
              }
            }
          ]
        }
      ]
    }
  },
  {
    key: 'validation-showcase',
    name: 'Validation Showcase',
    description: 'Demonstrates all available validation rules.',
    schema: {
      fields: [
        {
          id: 'f_username',
          name: 'username',
          label: 'Username (Min 5, Max 20, Regex)',
          type: 'text',
          required: true,
          validation: {
            minLength: 5,
            maxLength: 20,
            pattern: '^[a-zA-Z0-9_]+$',
            patternMessage: 'Only alphanumeric characters and underscores allowed'
          }
        },
        {
          id: 'f_age',
          name: 'age',
          label: 'Age (18-100)',
          type: 'number',
          required: true,
          validation: {
            min: 18,
            max: 100
          }
        },
        {
          id: 'f_website',
          name: 'website',
          label: 'Website (Regex URL)',
          type: 'text',
          validation: {
            pattern: '^https?://.+',
            patternMessage: 'Must start with http:// or https://'
          }
        }
      ],
      grids: []
    }
  }
];
