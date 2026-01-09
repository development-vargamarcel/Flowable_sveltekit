package com.demo.bpm.config;

import com.demo.bpm.entity.DocumentTypeDefinition;
import com.demo.bpm.service.DocumentTypeService;
import com.demo.bpm.service.ProcessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class DataSeeder {

    private final DocumentTypeService documentTypeService;
    private final ProcessService processService;

    @Bean
    public CommandLineRunner seedData() {
        return args -> {
            seedAllData();
        };
    }

    public void seedAllData() {
        seedDocumentTypes();
        seedProcessCategories();
    }

    private void seedDocumentTypes() {
        if (documentTypeService.getAllDocumentTypes().isEmpty()) {
            log.info("Seeding Document Types...");

            String invoiceSchema = """
                {
                  "fields": [
                    {
                      "id": "invoiceNumber",
                      "name": "invoiceNumber",
                      "label": "Invoice Number",
                      "type": "text",
                      "required": true,
                      "placeholder": "INV-0000",
                      "validation": {}
                    },
                    {
                      "id": "vendor",
                      "name": "vendor",
                      "label": "Vendor Name",
                      "type": "text",
                      "required": true,
                      "validation": {}
                    },
                    {
                      "id": "amount",
                      "name": "amount",
                      "label": "Total Amount",
                      "type": "currency",
                      "required": true,
                      "validation": { "min": 0 }
                    },
                    {
                      "id": "date",
                      "name": "date",
                      "label": "Invoice Date",
                      "type": "date",
                      "required": true,
                      "validation": {}
                    }
                  ],
                  "grids": [
                    {
                      "id": "lineItems",
                      "name": "lineItems",
                      "label": "Line Items",
                      "description": "Invoice line items",
                      "minRows": 1,
                      "columns": [
                        {
                          "id": "desc",
                          "name": "description",
                          "label": "Description",
                          "type": "text",
                          "required": true,
                          "validation": {}
                        },
                        {
                          "id": "qty",
                          "name": "quantity",
                          "label": "Quantity",
                          "type": "number",
                          "required": true,
                          "min": 1,
                          "validation": { "min": 1 }
                        },
                        {
                          "id": "price",
                          "name": "unitPrice",
                          "label": "Unit Price",
                          "type": "number",
                          "required": true,
                          "min": 0,
                          "validation": { "min": 0 }
                        }
                      ]
                    }
                  ]
                }
                """;

            documentTypeService.createDocumentType(DocumentTypeDefinition.builder()
                    .key("invoice")
                    .name("Invoice")
                    .description("Standard vendor invoice document")
                    .schemaJson(invoiceSchema)
                    .build());

            String leaveRequestSchema = """
                {
                  "fields": [
                    {
                      "id": "employeeName",
                      "name": "employeeName",
                      "label": "Employee Name",
                      "type": "text",
                      "required": true,
                      "readonly": true,
                      "validation": {}
                    },
                    {
                      "id": "type",
                      "name": "leaveType",
                      "label": "Leave Type",
                      "type": "select",
                      "options": [
                        { "value": "vacation", "label": "Vacation" },
                        { "value": "sick", "label": "Sick Leave" },
                        { "value": "personal", "label": "Personal" }
                      ],
                      "required": true,
                      "validation": {}
                    },
                    {
                      "id": "startDate",
                      "name": "startDate",
                      "label": "Start Date",
                      "type": "date",
                      "required": true,
                      "validation": {}
                    },
                    {
                      "id": "endDate",
                      "name": "endDate",
                      "label": "End Date",
                      "type": "date",
                      "required": true,
                      "validation": {}
                    }
                  ],
                  "grids": []
                }
                """;

            documentTypeService.createDocumentType(DocumentTypeDefinition.builder()
                    .key("leave_request")
                    .name("Leave Request")
                    .description("Employee leave request form")
                    .schemaJson(leaveRequestSchema)
                    .build());

            log.info("Document Types seeded.");
        }
    }

    private void seedProcessCategories() {
        // Categorize known demo processes
        try {
            processService.getAllProcessDefinitions().forEach(process -> {
                if (process.getKey().toLowerCase().contains("advanced")) {
                    processService.updateProcessDefinitionCategory(process.getId(), "Advanced");
                }
            });
            log.info("Process categories seeded.");
        } catch (Exception e) {
            log.warn("Failed to seed process categories: {}", e.getMessage());
        }
    }
}
