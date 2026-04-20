package com.demo.bpm.controller;

import com.demo.bpm.entity.Notification;
import com.demo.bpm.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc
@Import(com.demo.bpm.exception.GlobalExceptionHandler.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @Test
    @WithMockUser(username = "user1")
    void getNotifications_shouldReturnList() throws Exception {
        Notification notification = new Notification();
        notification.setTitle("Test Notif");

        when(notificationService.getUserNotifications("user1")).thenReturn(Collections.singletonList(notification));

        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Test Notif"));
    }

    @Test
    @WithMockUser(username = "user1")
    void getNotificationsPaged_shouldReturnPage() throws Exception {
        Notification notification = new Notification();
        notification.setTitle("Paged Notif");

        when(notificationService.getUserNotifications(eq("user1"), any()))
                .thenReturn(new PageImpl<>(Collections.singletonList(notification), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/notifications/paged"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Paged Notif"));
    }

    @Test
    @WithMockUser(username = "user1")
    void getUnreadCount_shouldReturnCount() throws Exception {
        when(notificationService.getUnreadCount("user1")).thenReturn(5L);

        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(5));
    }

    @Test
    @WithMockUser
    void markAsRead_shouldReturnOk() throws Exception {
        mockMvc.perform(post("/api/notifications/notif1/read")
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user1")
    void markAllAsRead_shouldReturnOk() throws Exception {
        mockMvc.perform(post("/api/notifications/read-all")
                        .with(csrf()))
                .andExpect(status().isOk());
    }
}
