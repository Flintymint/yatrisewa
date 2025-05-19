package com.example.travel_yatra.travel_yatra.service;

import com.example.travel_yatra.travel_yatra.model.Booking;
import com.example.travel_yatra.travel_yatra.model.Notification;
import com.example.travel_yatra.travel_yatra.model.Trip;
import com.example.travel_yatra.travel_yatra.model.User;
import com.example.travel_yatra.travel_yatra.repository.BookingRepository;
import com.example.travel_yatra.travel_yatra.repository.NotificationRepository;
import com.example.travel_yatra.travel_yatra.repository.TripRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class NotificationScheduler {
    @Autowired
    private TripRepository tripRepository;
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private NotificationRepository notificationRepository;


    // Runs every 5 minutes
    @Scheduled(cron = "0 */5 * * * *")
    public void sendTripReminders() {
        ZoneId kathmanduZone = ZoneId.of("Asia/Kathmandu");
        LocalDateTime now = LocalDateTime.now(kathmanduZone);
        LocalDateTime oneHourLater = now.plusHours(1);

        // Find all trips departing in exactly 1 hour
        List<Trip> trips = tripRepository.findAll();
        for (Trip trip : trips) {
            if (trip.getDepartureDate() == null || trip.getDepartureTime() == null) continue;
            LocalDateTime tripDateTime = LocalDateTime.of(trip.getDepartureDate(), trip.getDepartureTime());
            if (tripDateTime.isAfter(now) && tripDateTime.isBefore(oneHourLater.plusMinutes(1))) {
                // Notific Driver
                User driver = trip.getBusDriver();
                if (driver != null) {
                    boolean alreadyNotified = notificationRepository.findByUserIdAndSeenFalseOrderByCreatedAtDesc(driver.getId())
                        .stream().anyMatch(n -> n.getType().equals("DRIVER_TRIP_REMINDER") && n.getMessage().contains("Trip ID: " + trip.getId()));
                    if (!alreadyNotified) {
                        Notification notification = new Notification(
                            driver.getId(),
                            "Reminder: Your trip (Trip ID: " + trip.getId() + ") departs in 1 hour.",
                            "DRIVER_TRIP_REMINDER"
                        );
                        notificationRepository.save(notification);
                    }
                }
                // Notific Travellers 
                List<Booking> bookings = bookingRepository.findAllByTripId(trip.getId());
                for (Booking booking : bookings) {
                    User traveller = booking.getUser();
                    if (traveller != null) {
                        boolean alreadyNotified = notificationRepository.findByUserIdAndSeenFalseOrderByCreatedAtDesc(traveller.getId())
                            .stream().anyMatch(n -> n.getType().equals("TRAVELLER_TRIP_REMINDER") && n.getMessage().contains("Trip ID: " + trip.getId()));
                        if (!alreadyNotified) {
                            Notification notification = new Notification(
                                traveller.getId(),
                                "Reminder: Your trip (Trip ID: " + trip.getId() + ") departs in 1 hour.",
                                "TRAVELLER_TRIP_REMINDER"
                            );
                            notificationRepository.save(notification);
                        }
                    }
                }
            }
        }
    }
}
