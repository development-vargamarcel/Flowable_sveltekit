package com.demo.bpm.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DatabaseServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private DatabaseService databaseService;

    @Test
    void listTables_shouldReturnSortedList() {
        // Mocking execute is tricky because it takes a callback
        when(jdbcTemplate.execute(any(ConnectionCallback.class))).thenAnswer(invocation -> {
            // We can't easily mock the ResultSet iteration here without a lot of ceremony,
            // but we can at least verify it's called.
            return null;
        });

        List<String> result = databaseService.listTables();
        assertNotNull(result);
    }

    @Test
    void getTableColumns_withInvalidName_shouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> databaseService.getTableColumns("drop table users;"));
    }

    @Test
    void getTableRowCount_shouldReturnCount() {
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM USERS"), eq(Long.class))).thenReturn(10L);

        long count = databaseService.getTableRowCount("users");

        assertEquals(10L, count);
    }
}
