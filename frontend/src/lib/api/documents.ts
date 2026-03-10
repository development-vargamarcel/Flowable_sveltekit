import { fetchApi } from './core';
import type {
  TableColumn,
  TableDataResponse,
  DocumentDTO,
  SaveDocumentRequest,
  Page,
  GridRowDTO,
  SaveGridRowsRequest,
  DocumentTypeDefinition,
  DocumentTypeDefinitionRequest
} from '$lib/types';
import { createLogger } from '$lib/utils/logger';

const log = createLogger('api.documents');

export const documentsApi = {
  // Database Table Viewer
  /**
   * Get a list of all database tables.
   * @returns A promise that resolves to an array of table names.
   */
  async getDatabaseTables(): Promise<string[]> {
    log.debug('getDatabaseTables called');
    return fetchApi('/api/database/tables');
  },

  /**
   * Get columns for a specific table.
   * @param tableName - The name of the table.
   * @returns A promise that resolves to an array of table columns.
   */
  async getTableColumns(tableName: string): Promise<TableColumn[]> {
    log.debug('getTableColumns called', { tableName });
    return fetchApi(`/api/database/tables/${tableName}/columns`);
  },

  /**
   * Get data rows from a table.
   * @param tableName - The name of the table.
   * @param page - Page number.
   * @param size - Page size.
   * @returns A promise that resolves to the table data response.
   */
  async getTableData(
    tableName: string,
    page: number = 0,
    size: number = 20
  ): Promise<TableDataResponse> {
    log.debug('getTableData called', { tableName, page, size });
    const params = new URLSearchParams();
    params.append('page', page.toString());
    params.append('size', size.toString());
    return fetchApi(`/api/database/tables/${tableName}/data?${params.toString()}`);
  },

  // Save Draft
  /**
   * Save a process draft.
   * @param processDefinitionKey - The key of the process definition.
   * @param processDefinitionName - The name of the process definition.
   * @param variables - Process variables.
   * @param userId - The ID of the user saving the draft.
   * @param processInstanceId - Optional ID of an existing process instance.
   * @param businessKey - Optional business key.
   * @param documentType - Optional document type.
   * @returns A promise that resolves to the save result.
   */
  async saveDraft(
    processDefinitionKey: string,
    processDefinitionName: string,
    variables: Record<string, unknown>,
    userId: string,
    processInstanceId?: string,
    businessKey?: string,
    documentType?: string
  ): Promise<{ message: string; processInstanceId: string }> {
    log.debug('saveDraft called', { processDefinitionKey, processInstanceId, businessKey });
    return fetchApi('/api/business/save-draft', {
      method: 'POST',
      body: JSON.stringify({
        processInstanceId,
        businessKey,
        processDefinitionKey,
        processDefinitionName,
        documentType,
        variables,
        userId
      })
    });
  },

  // ==================== Document Operations ====================

  /**
   * Get all documents for a process instance.
   * @param processInstanceId - The ID of the process instance.
   * @param page - Page number.
   * @param size - Page size.
   * @returns A promise that resolves to a page of documents.
   */
  async getDocuments(
    processInstanceId: string,
    page: number = 0,
    size: number = 10
  ): Promise<Page<DocumentDTO>> {
    log.debug('getDocuments called', { processInstanceId, page, size });
    const params = new URLSearchParams();
    params.append('page', page.toString());
    params.append('size', size.toString());
    return fetchApi(
      `/api/business/processes/${processInstanceId}/document-types?${params.toString()}`
    );
  },

  /**
   * Get a specific document by type.
   * @param processInstanceId - The ID of the process instance.
   * @param documentType - The type of the document.
   * @returns A promise that resolves to the document DTO.
   */
  async getDocument(processInstanceId: string, documentType: string): Promise<DocumentDTO> {
    log.debug('getDocument called', { processInstanceId, documentType });
    return fetchApi(`/api/business/processes/${processInstanceId}/document-types/${documentType}`);
  },

  /**
   * Save a document with specific type.
   * @param processInstanceId - The ID of the process instance.
   * @param documentType - The type of the document.
   * @param request - The save request.
   * @returns A promise that resolves to the saved document DTO.
   */
  async saveDocument(
    processInstanceId: string,
    documentType: string,
    request: SaveDocumentRequest
  ): Promise<DocumentDTO> {
    log.debug('saveDocument called', { processInstanceId, documentType });
    return fetchApi(`/api/business/processes/${processInstanceId}/document-types/${documentType}`, {
      method: 'POST',
      body: JSON.stringify(request)
    });
  },

  /**
   * Get grid rows for a document type.
   * @param processInstanceId - The ID of the process instance.
   * @param documentType - The type of the document.
   * @param gridName - The name of the grid.
   * @param page - Page number.
   * @param size - Page size.
   * @returns A promise that resolves to a page of grid rows.
   */
  async getGridRows(
    processInstanceId: string,
    documentType: string,
    gridName: string,
    page: number = 0,
    size: number = 10
  ): Promise<Page<GridRowDTO>> {
    log.debug('getGridRows called', { processInstanceId, documentType, gridName, page, size });
    const params = new URLSearchParams();
    params.append('page', page.toString());
    params.append('size', size.toString());
    return fetchApi(
      `/api/business/processes/${processInstanceId}/document-types/${documentType}/grids/${gridName}?${params.toString()}`
    );
  },

  /**
   * Save grid rows for a document type.
   * @param processInstanceId - The ID of the process instance.
   * @param documentType - The type of the document.
   * @param gridName - The name of the grid.
   * @param request - The save request containing rows.
   * @returns A promise that resolves to the saved grid rows.
   */
  async saveGridRows(
    processInstanceId: string,
    documentType: string,
    gridName: string,
    request: SaveGridRowsRequest
  ): Promise<Page<GridRowDTO>> {
    log.debug('saveGridRows called', { processInstanceId, documentType, gridName });
    return fetchApi(
      `/api/business/processes/${processInstanceId}/document-types/${documentType}/grids/${gridName}`,
      {
        method: 'POST',
        body: JSON.stringify(request)
      }
    );
  },

  /**
   * Delete grid rows for a document type.
   * @param processInstanceId - The ID of the process instance.
   * @param documentType - The type of the document.
   * @param gridName - The name of the grid.
   */
  async deleteGridRows(
    processInstanceId: string,
    documentType: string,
    gridName: string
  ): Promise<void> {
    log.debug('deleteGridRows called', { processInstanceId, documentType, gridName });
    await fetchApi(
      `/api/business/processes/${processInstanceId}/document-types/${documentType}/grids/${gridName}`,
      {
        method: 'DELETE'
      }
    );
  },

  /**
   * Get all documents by business key.
   * @param businessKey - The business key.
   * @param page - Page number.
   * @param size - Page size.
   * @returns A promise that resolves to a page of documents.
   */
  async getDocumentsByBusinessKey(
    businessKey: string,
    page: number = 0,
    size: number = 10
  ): Promise<Page<DocumentDTO>> {
    log.debug('getDocumentsByBusinessKey called', { businessKey, page, size });
    const params = new URLSearchParams();
    params.append('page', page.toString());
    params.append('size', size.toString());
    return fetchApi(
      `/api/business/document-types/all/by-business-key/${businessKey}?${params.toString()}`
    );
  },

  // ==================== Document Type Definitions ====================
  /**
   * Get all document type definitions.
   * @returns A promise that resolves to an array of document types.
   */
  async getDocumentTypes(): Promise<DocumentTypeDefinition[]> {
    log.debug('getDocumentTypes called');
    return fetchApi('/api/document-types');
  },

  /**
   * Get a specific document type definition.
   * @param key - The key of the document type.
   * @returns A promise that resolves to the document type definition.
   */
  async getDocumentType(key: string): Promise<DocumentTypeDefinition> {
    log.debug('getDocumentType called', { key });
    return fetchApi(`/api/document-types/${key}`);
  },

  /**
   * Create a new document type definition.
   * @param data - The document type data.
   * @returns A promise that resolves to the created document type.
   */
  async createDocumentType(data: DocumentTypeDefinitionRequest): Promise<DocumentTypeDefinition> {
    log.debug('createDocumentType called');
    return fetchApi('/api/document-types', {
      method: 'POST',
      body: JSON.stringify(data)
    });
  },

  /**
   * Update a document type definition.
   * @param key - The key of the document type.
   * @param data - The updated document type data.
   * @returns A promise that resolves to the updated document type.
   */
  async updateDocumentType(
    key: string,
    data: DocumentTypeDefinitionRequest
  ): Promise<DocumentTypeDefinition> {
    log.debug('updateDocumentType called', { key });
    return fetchApi(`/api/document-types/${key}`, {
      method: 'PUT',
      body: JSON.stringify(data)
    });
  },

  /**
   * Delete a document type definition.
   * @param key - The key of the document type.
   */
  async deleteDocumentType(key: string): Promise<void> {
    log.debug('deleteDocumentType called', { key });
    await fetchApi(`/api/document-types/${key}`, { method: 'DELETE' });
  }
};
