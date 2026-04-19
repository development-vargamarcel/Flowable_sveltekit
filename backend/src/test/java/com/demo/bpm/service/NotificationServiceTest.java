package com.demo.bpm.service;

import com.demo.bpm.entity.Notification;
import com.demo.bpm.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void getUserNotifications_shouldReturnList() {
        Notification notification = new Notification();
        notification.setUserId("user1");
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc("user1")).thenReturn(Collections.singletonList(notification));

        List<Notification> result = notificationService.getUserNotifications("user1");

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("user1", result.get(0).getUserId());
    }

    @Test
    void getUserNotificationsPaged_shouldReturnPage() {
        Notification notification = new Notification();
        Page<Notification> page = new PageImpl<>(Collections.singletonList(notification));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq("user1"), any())).thenReturn(page);

        Page<Notification> result = notificationService.getUserNotifications("user1", PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getUnreadCount_shouldReturnCount() {
        when(notificationRepository.countByUserIdAndReadFalse("user1")).thenReturn(5L);

        long count = notificationService.getUnreadCount("user1");

        assertEquals(5L, count);
    }

    @Test
    void createNotification_shouldSaveAndSendEmail() {
        Notification notification = new Notification();
        notification.setUserId("test@example.com");
        notification.setTitle("Title");

        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        Notification result = notificationService.createNotification(
                "test@example.com", "Title", "Message",
                Notification.NotificationType.TASK_ASSIGNED, "/link");

        assertNotNull(result);
        verify(notificationRepository).save(any(Notification.class));
        verify(emailService).sendSimpleMessage(eq("test@example.com"), anyString(), anyString());
    }

    @Test
    void markAsRead_shouldUpdateNotification() {
        Notification notification = new Notification();
        notification.setId("notif1");
        notification.setRead(false);

        when(notificationRepository.findById("notif1")).thenReturn(Optional.of(notification));

        notificationService.markAsRead("notif1");

        assertTrue(notification.isRead());
        verify(notificationRepository).save(notification);
    }

    @Test
    void markAllAsRead_shouldUpdateAll() {
        Notification n1 = new Notification();
        n1.setRead(false);
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc("user1")).thenReturn(Collections.singletonList(n1));

        notificationService.markAllAsRead("user1");

        assertTrue(n1.isRead());
        verify(notificationRepository).saveAll(anyList());
    }
}
