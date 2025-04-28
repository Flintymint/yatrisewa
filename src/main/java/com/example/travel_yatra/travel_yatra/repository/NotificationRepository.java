package com.example.travel_yatra.travel_yatra.repository;

import com.example.travel_yatra.travel_yatra.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdAndSeenFalseOrderByCreatedAtDesc(UUID userId);
}
