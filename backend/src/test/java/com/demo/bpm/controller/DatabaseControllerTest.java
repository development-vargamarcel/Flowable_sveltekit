package com.demo.bpm.controller;

import com.demo.bpm.config.DataSeeder;
import com.demo.bpm.service.DatabaseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DatabaseController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(com.demo.bpm.exception.GlobalExceptionHandler.class)
class DatabaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DatabaseService databaseService;

    @MockBean
    private DataSeeder dataSeeder;

    @Test
    @WithMockUser
    void listTables_shouldReturnList() throws Exception {
        when(databaseService.listTables()).thenReturn(Collections.singletonList("USERS"));

        mockMvc.perform(get("/api/database/tables"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("USERS"));
    }

    @Test
    @WithMockUser
    void getTableColumns_shouldReturnMetadata() throws Exception {
        when(databaseService.getTableColumns("USERS")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/database/tables/USERS/columns"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void getTableData_shouldReturnPaginatedData() throws Exception {
        DatabaseService.TableData data = new DatabaseService.TableData(
                "USERS", Collections.emptyList(), Collections.emptyList(), 0, 10, 0, 0);

        when(databaseService.getTableData(eq("USERS"), anyInt(), anyInt())).thenReturn(data);

        mockMvc.perform(get("/api/database/tables/USERS/data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tableName").value("USERS"));
    }

    @Test
    @WithMockUser
    void seedDatabase_shouldInvokeSeeder() throws Exception {
        mockMvc.perform(post("/api/database/seed"))
                .andExpect(status().isOk());

        verify(dataSeeder).seedAllData();
    }
}
