package com.demo.bpm.controller;

import com.demo.bpm.service.DatabaseService;
import com.demo.bpm.service.DatabaseService.TableData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for database table browsing operations.
 * Provides endpoints to list tables and view data with pagination.
 */
@RestController
@RequestMapping("/api/database")
@RequiredArgsConstructor
@Slf4j
public class DatabaseController {

    private final DatabaseService databaseService;

    /**
     * List all tables in the database.
     */
    @GetMapping("/tables")
    public ResponseEntity<List<String>> listTables() {
        log.info("Listing all database tables");
        List<String> tables = databaseService.listTables();
        return ResponseEntity.ok(tables);
    }

    /**
     * Get column metadata for a specific table.
     */
    @GetMapping("/tables/{tableName}/columns")
    public ResponseEntity<List<Map<String, Object>>> getTableColumns(
            @PathVariable String tableName) {
        log.info("Getting columns for table: {}", tableName);
        try {
            List<Map<String, Object>> columns = databaseService.getTableColumns(tableName);
            return ResponseEntity.ok(columns);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get paginated data from a specific table.
     */
    @GetMapping("/tables/{tableName}/data")
    public ResponseEntity<TableDataResponse> getTableData(
            @PathVariable String tableName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Getting data for table: {}, page: {}, size: {}", tableName, page, size);
        try {
            TableData data = databaseService.getTableData(tableName, page, size);
            TableDataResponse response = new TableDataResponse(
                    data.tableName(),
                    data.columns(),
                    data.rows(),
                    data.page(),
                    data.size(),
                    data.totalRows(),
                    data.totalPages()
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error fetching table data", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Response DTO for table data.
     */
    public record TableDataResponse(
            String tableName,
            List<String> columns,
            List<Map<String, Object>> rows,
            int page,
            int size,
            long totalRows,
            int totalPages
    ) {}
}
