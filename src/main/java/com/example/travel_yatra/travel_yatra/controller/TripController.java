package com.example.travel_yatra.travel_yatra.controller;

import com.example.travel_yatra.travel_yatra.model.*;
import com.example.travel_yatra.travel_yatra.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import jakarta.persistence.criteria.Predicate;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.Map;

@RestController
@RequestMapping("/api/trips")
public class TripController {
    @Autowired
    private TripRepository tripRepository;
    @Autowired
    private BusRepository busRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private NotificationRepository notificationRepository;

    // Endpoint for bus drivers to view only their assigned trips (must be first)
    @GetMapping("/my-trips")
    @PreAuthorize("hasRole('bus_driver')")
    public ResponseEntity<?> getMyTrips(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body("Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7);
        // Extract user id from JWT (assume 'id' claim is present)
        String userId;
        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(System.getenv("JWT_SECRET").getBytes())
                .build()
                .parseClaimsJws(token).getBody();
            userId = claims.get("id", String.class);
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Invalid or expired JWT");
        }
        UUID uuid;
        try {
            uuid = UUID.fromString(userId);
        } catch (Exception e) {
            return ResponseEntity.status(400).body("Invalid user id in token");
        }
        var trips = tripRepository.findAllTripsByBusDriverId(uuid);
        // Exclude trips with status 'completed'
        var filteredTrips = trips.stream()
            .filter(trip -> !"arrived".equalsIgnoreCase(trip.getTripStatus()))
            .toList();
        return ResponseEntity.ok(filteredTrips);
    }

    @GetMapping("/my-trips/current")
    @PreAuthorize("hasRole('bus_driver')")
    public ResponseEntity<?> getMyCurrentTrips(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body("Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7);
        String userId;
        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(System.getenv("JWT_SECRET").getBytes())
                .build()
                .parseClaimsJws(token).getBody();
            userId = claims.get("id", String.class);
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Invalid or expired JWT");
        }
        UUID uuid;
        try {
            uuid = UUID.fromString(userId);
        } catch (Exception e) {
            return ResponseEntity.status(400).body("Invalid user id in token");
        }
        // Get the system's default timezone and ZonedDateTime
        java.time.ZoneId systemZone = java.time.ZoneId.systemDefault();
        java.time.ZonedDateTime systemNow = java.time.ZonedDateTime.now(systemZone);
        // Get the trip data timezone
        java.time.ZoneId tripZone = java.time.ZoneId.of("Asia/Kathmandu");
        java.time.ZonedDateTime tripZoneNow = systemNow.withZoneSameInstant(tripZone);
        // Use the trip timezone's local time for comparisons
        java.time.LocalDateTime now = tripZoneNow.toLocalDateTime();
        var allTrips = tripRepository.findAllTripsByBusDriverId(uuid);
        var filteredTrips = allTrips.stream()
            .filter(trip -> {
                boolean isWithinTimeRange = false;
                if (trip.getDepartureTime() != null && trip.getDepartureDate() != null) {
                    java.time.LocalDateTime tripDateTime = java.time.LocalDateTime.of(
                        trip.getDepartureDate(), trip.getDepartureTime());
                    long diffMinutes = Math.abs(java.time.Duration.between(now, tripDateTime).toMinutes());
                    isWithinTimeRange = diffMinutes <= 60;
                }
                boolean isDeparted = "departed".equalsIgnoreCase(trip.getTripStatus());
                boolean isArrived = "arrived".equalsIgnoreCase(trip.getTripStatus());
                return (isWithinTimeRange || isDeparted) && !isArrived;
            })
            .toList();
        return ResponseEntity.ok(filteredTrips);
    }

    @PostMapping
    public ResponseEntity<?> createTrip(@RequestBody Trip trip) {
        try {
            if (trip.getFrom() == null || trip.getTo() == null || trip.getBus() == null || trip.getBusDriver() == null) {
                return ResponseEntity.badRequest().body("Missing required fields.");
            }
            if (trip.getFrom().getId().equals(trip.getTo().getId())) {
                return ResponseEntity.badRequest().body("From and To bus stops cannot be the same.");
            }
            // Validate bus driver role
            Optional<User> driverOpt = userRepository.findById(trip.getBusDriver().getId());
            if (driverOpt.isEmpty() || !"bus_driver".equals(driverOpt.get().getRole())) {
                return ResponseEntity.badRequest().body("Invalid bus driver ID or role");
            }
            // Always set trip status to 'scheduled' if null or empty (force default)
            if (trip.getTripStatus() == null || trip.getTripStatus().isEmpty()) {
                trip.setTripStatus("scheduled");
            }
            Optional<Bus> busOpt = busRepository.findById(trip.getBus().getId());
            if (busOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Bus not found");
            }
            if (!busOpt.get().isAvailable()) {
                return ResponseEntity.badRequest().body("Bus is not available");
            }
            // Reset teardown reason if not teardown
            if (!"teardown".equalsIgnoreCase(trip.getTripStatus())) {
                trip.setReason(null);
            }
            // Set bus availability to false when assigned to a new trip
            Bus assignedBus = busOpt.get();
            assignedBus.setAvailable(false);
            busRepository.save(assignedBus);
            // departureTime is now handled by the entity and will be persisted automatically
            Trip saved = tripRepository.save(trip);
            // --- Notification logic ---
            try {
                Notification notification = new Notification(
                    trip.getBusDriver().getId(),
                    "You have been assigned a new trip (Trip ID: " + saved.getId() + ")",
                    "DRIVER_ASSIGNMENT"
                );
                notificationRepository.save(notification);
            } catch (Exception e) {
                System.err.println("Failed to create driver assignment notification: " + e.getMessage());
            }
            // --- End Notification logic ---
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Failed to create trip: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllTrips(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String fromId,
            @RequestParam(required = false) String toId,
            @RequestParam(required = false) Long busId,
            @RequestParam(required = false) String tripStatus,
            @RequestParam(required = false) String busDriverId,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Boolean bookedByUser,
            @RequestParam(required = false) String departureDate
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Specification<Trip> spec = (root, query, cb) -> {
            java.util.List<Predicate> predicates = new java.util.ArrayList<>();
            if (fromId != null) {
                try {
                    java.util.UUID fromUuid = java.util.UUID.fromString(fromId);
                    predicates.add(cb.equal(root.get("from").get("id"), fromUuid));
                } catch (Exception e) {
                    
                }
            }
            if (toId != null) {
                try {
                    java.util.UUID toUuid = java.util.UUID.fromString(toId);
                    predicates.add(cb.equal(root.get("to").get("id"), toUuid));
                } catch (Exception e) {
                    
                }
            }
            if (busId != null) predicates.add(cb.equal(root.get("bus").get("id"), busId));
            if (tripStatus != null) predicates.add(cb.equal(root.get("tripStatus"), tripStatus));
            if (busDriverId != null) {
                try {
                    java.util.UUID driverUuid = java.util.UUID.fromString(busDriverId);
                    predicates.add(cb.equal(root.get("busDriver").get("id"), driverUuid));
                } catch (Exception e) {
                    
                }
            }
            // Add departure date filter
            if (departureDate != null) {
                try {
                    java.time.LocalDate depDate = java.time.LocalDate.parse(departureDate);
                    predicates.add(cb.equal(root.get("departureDate"), depDate));
                } catch (Exception e) {
                    
                }
            }
            // Only include trips with status scheduled, departed, or teardown
            predicates.add(root.get("tripStatus").in(
                "scheduled", "departed", "teardown"
            ));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<Trip> tripPage = tripRepository.findAll(spec, pageable);
        // If email and bookedByUser are provided, filter trips by whether the user has booked them
        if (email != null && bookedByUser != null) {
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent()) {
                UUID userId = userOpt.get().getId();
                java.util.Set<Long> bookedTripIds = new java.util.HashSet<>();
                java.util.List<com.example.travel_yatra.travel_yatra.model.Booking> bookings = bookingRepository.findAllByUserId(userId);
                for (var booking : bookings) {
                    bookedTripIds.add(booking.getTrip().getId());
                }
                if (bookedByUser) {
                    tripPage = new org.springframework.data.domain.PageImpl<>(tripPage.stream().filter(trip -> bookedTripIds.contains(trip.getId())).toList(), pageable, tripPage.getTotalElements());
                } else {
                    tripPage = new org.springframework.data.domain.PageImpl<>(tripPage.stream().filter(trip -> !bookedTripIds.contains(trip.getId())).toList(), pageable, tripPage.getTotalElements());
                }
            } else {
                // If user not found, return empty page
                tripPage = new org.springframework.data.domain.PageImpl<>(java.util.Collections.emptyList(), pageable, 0);
            }
        }
        return ResponseEntity.ok(tripPage);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTrip(@PathVariable Long id) {
        return tripRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTrip(@PathVariable Long id) {
        if (!tripRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        tripRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTrip(@PathVariable Long id, @RequestBody Trip trip) {
        Optional<Trip> existingOpt = tripRepository.findById(id);
        if (existingOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Trip existing = existingOpt.get();
        User previousDriver = existing.getBusDriver();
        if (trip.getFrom() != null && trip.getTo() != null && !trip.getFrom().getId().equals(trip.getTo().getId())) {
            existing.setFrom(trip.getFrom());
            existing.setTo(trip.getTo());
        }
        if (trip.getBus() != null) existing.setBus(trip.getBus());
        if (trip.getPrice() != 0) existing.setPrice(trip.getPrice());
        if (trip.getTripStatus() != null) existing.setTripStatus(trip.getTripStatus());
        existing.setReason(trip.getReason());
        if (trip.getBusDriver() != null) existing.setBusDriver(trip.getBusDriver());
        Trip saved = tripRepository.save(existing);
        // --- Notification logic for driver unassignment ---
        try {
            if (previousDriver != null && trip.getBusDriver() != null && !previousDriver.getId().equals(trip.getBusDriver().getId())) {
                Notification notification = new Notification(
                    previousDriver.getId(),
                    "You have been unassigned from trip (Trip ID: " + saved.getId() + ")",
                    "DRIVER_UNASSIGNED"
                );
                notificationRepository.save(notification);
            }
        } catch (Exception e) {
            System.err.println("Failed to create driver unassignment notification: " + e.getMessage());
        }
        // --- End Notification logic ---
        return ResponseEntity.ok(saved);
    }

    // Endpoint for bus drivers to update only trip status and reason (reason only if teardown) using custom SQL
    @PatchMapping("/{id}/driver-update")
    @PreAuthorize("hasRole('bus_driver')")
    @Transactional
    public ResponseEntity<?> driverUpdateTrip(@PathVariable Long id, @RequestBody Map<String, Object> updates, HttpServletRequest request) {
        // Get authenticated driver UUID from JWT
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body("Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7);
        String userId;
        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(System.getenv("JWT_SECRET").getBytes())
                .build()
                .parseClaimsJws(token).getBody();
            userId = claims.get("id", String.class);
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Invalid or expired JWT");
        }
        UUID driverId;
        try {
            driverId = UUID.fromString(userId);
        } catch (Exception e) {
            return ResponseEntity.status(400).body("Invalid user id in token");
        }
        String newStatus = (String) updates.get("tripStatus");
        String reason = (String) updates.getOrDefault("reason", null);
        int updated = tripRepository.driverUpdateTripStatusAndReason(id, driverId, newStatus, reason);
        if (updated == 0) {
            return ResponseEntity.status(404).body("Trip not found or unauthorized");
        }
        Optional<Trip> updatedTrip = tripRepository.findById(id);
        if (updatedTrip.isPresent()) {
            // If trip status is set to 'arrived', set the associated bus as available
            if ("arrived".equalsIgnoreCase(newStatus)) {
                Trip tripObj = updatedTrip.get();
                Bus bus = tripObj.getBus();
                if (bus != null && !bus.isAvailable()) {
                    bus.setAvailable(true);
                    busRepository.save(bus);
                }
            }
            // --- Notification logic for ADMIN ---
            try {
                // Notify all admins when driver changes trip status
                var admins = userRepository.fetchAllByRole("admin");
                for (User admin : admins) {
                    Notification notification = new Notification(
                        admin.getId(),
                        "Driver updated trip status: Trip ID " + id + ", new status: " + newStatus,
                        "DRIVER_STATUS_UPDATE"
                    );
                    notificationRepository.save(notification);
                }
            } catch (Exception e) {
                System.err.println("Failed to create admin notification: " + e.getMessage());
            }
            // --- End Notification logic ---
            return ResponseEntity.ok(updatedTrip.get());
        } else {
            return ResponseEntity.status(404).body("Trip not found after update");
        }
    }
}
