package com.example.travel_yatra.travel_yatra.controller;

import com.example.travel_yatra.travel_yatra.model.Notification;
import com.example.travel_yatra.travel_yatra.repository.NotificationRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    @Autowired
    private NotificationRepository notificationRepository;

    @GetMapping("/my")
    public List<Notification> getMyNotifications(HttpServletRequest request) {
        UUID userId = extractUserIdFromJwt(request);
        return notificationRepository.findByUserIdAndSeenFalseOrderByCreatedAtDesc(userId);
    }

    @PostMapping("/mark-seen")
    public ResponseEntity<?> markSeen(@RequestBody List<Long> ids) {
        for (Long id : ids) {
            Optional<Notification> notifOpt = notificationRepository.findById(id);
            notifOpt.ifPresent(notif -> {
                notif.setSeen(true);
                notificationRepository.save(notif);
            });
        }
        return ResponseEntity.ok().build();
    }

    private UUID extractUserIdFromJwt(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7);
        io.jsonwebtoken.Claims claims = io.jsonwebtoken.Jwts.parserBuilder()
            .setSigningKey(System.getenv("JWT_SECRET").getBytes())
            .build()
            .parseClaimsJws(token).getBody();
        return UUID.fromString(claims.get("id", String.class));
    }
}
