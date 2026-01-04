package com.demo.bpm.controller;

import com.demo.bpm.entity.Notification;
import com.demo.bpm.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<Notification>> getNotifications(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(notificationService.getUserNotifications(authentication.getName()));
    }

    @GetMapping("/paged")
    public ResponseEntity<Page<Notification>> getNotificationsPaged(
            Authentication authentication,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(notificationService.getUserNotifications(authentication.getName(), pageable));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.ok(0L);
        }
        return ResponseEntity.ok(notificationService.getUnreadCount(authentication.getName()));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable String id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(Authentication authentication) {
        notificationService.markAllAsRead(authentication.getName());
        return ResponseEntity.ok().build();
    }
}
