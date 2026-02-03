/* eslint-disable no-console */
import { fetchApi } from './core';
import type {
  TableColumn,
  TableDataResponse,
  DocumentDTO,
  SaveDocumentRequest,
  Page,
  GridRowDTO,
  SaveGridRowsRequest
} from '$lib/types';

export const documentsApi = {
  // Database Table Viewer
  /**
   * Get a list of all database tables.
   * @returns A promise that resolves to an array of table names.
   */
  async getDatabaseTables(): Promise<string[]> {
    console.log('[documentsApi] getDatabaseTables called');
    return fetchApi('/api/database/tables');
  },

  /**
   * Get columns for a specific table.
   * @param tableName - The name of the table.
   * @returns A promise that resolves to an array of table columns.
   */
  async getTableColumns(tableName: string): Promise<TableColumn[]> {
    console.log('[documentsApi] getTableColumns called with tableName:', tableName);
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
    console.log('[documentsApi] getTableData called with tableName:', tableName, 'page:', page);
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
    console.log('[documentsApi] saveDraft called for process:', processDefinitionKey);
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
    console.log('[documentsApi] getDocuments called with id:', processInstanceId);
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
    console.log(
      '[documentsApi] getDocument called with id:',
      processInstanceId,
      'type:',
      documentType
    );
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
    console.log(
      '[documentsApi] saveDocument called with id:',
      processInstanceId,
      'type:',
      documentType
    );
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
    console.log('[documentsApi] getGridRows called with id:', processInstanceId, 'grid:', gridName);
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
    console.log(
      '[documentsApi] saveGridRows called with id:',
      processInstanceId,
      'grid:',
      gridName
    );
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
    console.log(
      '[documentsApi] deleteGridRows called with id:',
      processInstanceId,
      'grid:',
      gridName
    );
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
    console.log('[documentsApi] getDocumentsByBusinessKey called with key:', businessKey);
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
  async getDocumentTypes(): Promise<any[]> {
    console.log('[documentsApi] getDocumentTypes called');
    return fetchApi('/api/document-types');
  },

  /**
   * Get a specific document type definition.
   * @param key - The key of the document type.
   * @returns A promise that resolves to the document type definition.
   */
  async getDocumentType(key: string): Promise<any> {
    console.log('[documentsApi] getDocumentType called with key:', key);
    return fetchApi(`/api/document-types/${key}`);
  },

  /**
   * Create a new document type definition.
   * @param data - The document type data.
   * @returns A promise that resolves to the created document type.
   */
  async createDocumentType(data: any): Promise<any> {
    console.log('[documentsApi] createDocumentType called');
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
  async updateDocumentType(key: string, data: any): Promise<any> {
    console.log('[documentsApi] updateDocumentType called with key:', key);
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
    console.log('[documentsApi] deleteDocumentType called with key:', key);
    await fetchApi(`/api/document-types/${key}`, { method: 'DELETE' });
  }
};
