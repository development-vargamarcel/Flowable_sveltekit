package com.demo.bpm.service;

import com.demo.bpm.entity.Notification;
import com.demo.bpm.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;

    public List<Notification> getUserNotifications(String userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Page<Notification> getUserNotifications(String userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    public long getUnreadCount(String userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public Notification createNotification(String userId, String title, String message, 
                                         Notification.NotificationType type, String link) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setLink(link);
        Notification saved = notificationRepository.save(notification);

        // Send email for task assignments/reminders
        // In a real app, you'd lookup the user's email address here using userId
        // For demonstration, we'll try to guess email or log it
        if (type == Notification.NotificationType.TASK_ASSIGNED || 
            type == Notification.NotificationType.SLA_WARNING || 
            type == Notification.NotificationType.SLA_BREACH) {
            
            // NOTE: Since we don't have a real User entity with email, we assume userId might be an email 
            // OR simply skip if it doesn't look like one.
            if (userId != null && userId.contains("@")) {
                emailService.sendSimpleMessage(userId, title, message + "\n\nLink: " + link);
            }
        }

        return saved;
    }

    @Transactional
    public void markAsRead(String notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setRead(true);
            notificationRepository.save(notification);
        });
    }

    @Transactional
    public void markAllAsRead(String userId) {
        List<Notification> userNotifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        userNotifications.forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(userNotifications);
    }
}
