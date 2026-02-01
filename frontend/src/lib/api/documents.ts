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
  async getDatabaseTables(): Promise<string[]> {
    return fetchApi('/api/database/tables');
  },

  async getTableColumns(tableName: string): Promise<TableColumn[]> {
    return fetchApi(`/api/database/tables/${tableName}/columns`);
  },

  async getTableData(
    tableName: string,
    page: number = 0,
    size: number = 20
  ): Promise<TableDataResponse> {
    const params = new URLSearchParams();
    params.append('page', page.toString());
    params.append('size', size.toString());
    return fetchApi(`/api/database/tables/${tableName}/data?${params.toString()}`);
  },

  // Save Draft
  async saveDraft(
    processDefinitionKey: string,
    processDefinitionName: string,
    variables: Record<string, unknown>,
    userId: string,
    processInstanceId?: string,
    businessKey?: string,
    documentType?: string
  ): Promise<{ message: string; processInstanceId: string }> {
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
   * Get all documents for a process instance
   */
  async getDocuments(
    processInstanceId: string,
    page: number = 0,
    size: number = 10
  ): Promise<Page<DocumentDTO>> {
    const params = new URLSearchParams();
    params.append('page', page.toString());
    params.append('size', size.toString());
    return fetchApi(
      `/api/business/processes/${processInstanceId}/document-types?${params.toString()}`
    );
  },

  /**
   * Get a specific document by type
   */
  async getDocument(processInstanceId: string, documentType: string): Promise<DocumentDTO> {
    return fetchApi(`/api/business/processes/${processInstanceId}/document-types/${documentType}`);
  },

  /**
   * Save a document with specific type
   */
  async saveDocument(
    processInstanceId: string,
    documentType: string,
    request: SaveDocumentRequest
  ): Promise<DocumentDTO> {
    return fetchApi(`/api/business/processes/${processInstanceId}/document-types/${documentType}`, {
      method: 'POST',
      body: JSON.stringify(request)
    });
  },

  /**
   * Get grid rows for a document type
   */
  async getGridRows(
    processInstanceId: string,
    documentType: string,
    gridName: string,
    page: number = 0,
    size: number = 10
  ): Promise<Page<GridRowDTO>> {
    const params = new URLSearchParams();
    params.append('page', page.toString());
    params.append('size', size.toString());
    return fetchApi(
      `/api/business/processes/${processInstanceId}/document-types/${documentType}/grids/${gridName}?${params.toString()}`
    );
  },

  /**
   * Save grid rows for a document type
   */
  async saveGridRows(
    processInstanceId: string,
    documentType: string,
    gridName: string,
    request: SaveGridRowsRequest
  ): Promise<Page<GridRowDTO>> {
    return fetchApi(
      `/api/business/processes/${processInstanceId}/document-types/${documentType}/grids/${gridName}`,
      {
        method: 'POST',
        body: JSON.stringify(request)
      }
    );
  },

  /**
   * Delete grid rows for a document type
   */
  async deleteGridRows(
    processInstanceId: string,
    documentType: string,
    gridName: string
  ): Promise<void> {
    await fetchApi(
      `/api/business/processes/${processInstanceId}/document-types/${documentType}/grids/${gridName}`,
      {
        method: 'DELETE'
      }
    );
  },

  /**
   * Get all documents by business key
   */
  async getDocumentsByBusinessKey(
    businessKey: string,
    page: number = 0,
    size: number = 10
  ): Promise<Page<DocumentDTO>> {
    const params = new URLSearchParams();
    params.append('page', page.toString());
    params.append('size', size.toString());
    return fetchApi(
      `/api/business/document-types/all/by-business-key/${businessKey}?${params.toString()}`
    );
  },

  // ==================== Document Type Definitions ====================
  async getDocumentTypes(): Promise<any[]> {
    return fetchApi('/api/document-types');
  },

  async getDocumentType(key: string): Promise<any> {
    return fetchApi(`/api/document-types/${key}`);
  },

  async createDocumentType(data: any): Promise<any> {
    return fetchApi('/api/document-types', {
      method: 'POST',
      body: JSON.stringify(data)
    });
  },

  async updateDocumentType(key: string, data: any): Promise<any> {
    return fetchApi(`/api/document-types/${key}`, {
      method: 'PUT',
      body: JSON.stringify(data)
    });
  },

  async deleteDocumentType(key: string): Promise<void> {
    await fetchApi(`/api/document-types/${key}`, { method: 'DELETE' });
  }
};
