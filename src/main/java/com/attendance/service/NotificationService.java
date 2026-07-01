package com.attendance.service;



import com.attendance.model.Notification;

import com.attendance.model.User;

import com.attendance.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;



import java.util.List;



@Service

@RequiredArgsConstructor

public class NotificationService {



    private final NotificationRepository notificationRepository;



    public List<Notification> findByUser(Long userId) {

        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);

    }



    public long countUnread(Long userId) {

        return notificationRepository.countByUserIdAndReadFalse(userId);

    }



    @Transactional

    public void markAsRead(Long id) {

        notificationRepository.findById(id).ifPresent(n -> {

            n.setRead(true);

            notificationRepository.save(n);

        });

    }



    @Transactional
    public void create(User user, String title, String message) {
        notificationRepository.save(Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .build());
    }

    @Transactional
    public void createIfAbsent(User user, String title, String message, int withinDays) {
        if (!notificationRepository.existsByUserIdAndTitleAndCreatedAtAfter(
                user.getId(), title, java.time.LocalDateTime.now().minusDays(withinDays))) {
            create(user, title, message);
        }
    }
}


