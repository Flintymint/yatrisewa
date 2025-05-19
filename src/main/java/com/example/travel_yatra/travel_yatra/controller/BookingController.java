package com.example.travel_yatra.travel_yatra.controller;

import com.example.travel_yatra.travel_yatra.model.Booking;
import com.example.travel_yatra.travel_yatra.model.Trip;
import com.example.travel_yatra.travel_yatra.model.User;
import com.example.travel_yatra.travel_yatra.repository.BookingRepository;
import com.example.travel_yatra.travel_yatra.repository.TripRepository;
import com.example.travel_yatra.travel_yatra.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private TripRepository tripRepository;
    @Autowired
    private UserRepository userRepository;

    // Book a seat for a trip 
    @PostMapping("/book")
    @PreAuthorize("hasRole('traveller')")
    public ResponseEntity<?> bookSeat(@RequestBody BookSeatRequest request, HttpServletRequest httpRequest) {
        // Extract user ID from JWT
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body("Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7);
        String userId;
        try {
            io.jsonwebtoken.JwtParser parser = io.jsonwebtoken.Jwts.parserBuilder()
                .setSigningKey(System.getenv("JWT_SECRET").getBytes())
                .build();
            io.jsonwebtoken.Claims claims = parser.parseClaimsJws(token).getBody();
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
        Optional<User> userOpt = userRepository.findById(uuid);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body("User not found");
        }
        Optional<Trip> tripOpt = tripRepository.findById(request.getTripId());
        if (tripOpt.isEmpty()) {
            return ResponseEntity.status(404).body("Trip not found");
        }
        // Check if seat is already booked
        if (bookingRepository.existsByTripIdAndSeatLabel(request.getTripId(), request.getSeatLabel())) {
            return ResponseEntity.status(409).body("Seat already booked for this trip");
        }
        Booking booking = new Booking(userOpt.get(), tripOpt.get(), request.getSeatLabel());
        // Set paid = false by default for new bookings
        booking.setPaid(false);
        bookingRepository.save(booking);
        return ResponseEntity.ok(booking);
    }

    // Get all reserved seats for a trip
    @GetMapping("/reserved-seats")
    public ResponseEntity<?> getReservedSeats(@RequestParam Long tripId) {
        try {
            var bookings = bookingRepository.findAllByTripId(tripId);
            var reservedSeats = bookings.stream().map(Booking::getSeatLabel).toList();
            return ResponseEntity.ok(reservedSeats);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching reserved seats: " + e.getMessage());
        }
    }

    // View all bookings for the authenticated traveller
    @GetMapping("/my")
    @PreAuthorize("hasRole('traveller')")
    public ResponseEntity<?> getMyBookings(HttpServletRequest httpRequest) {
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body("Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7);
        String userId;
        try {
            io.jsonwebtoken.JwtParser parser = io.jsonwebtoken.Jwts.parserBuilder()
                .setSigningKey(System.getenv("JWT_SECRET").getBytes())
                .build();
            io.jsonwebtoken.Claims claims = parser.parseClaimsJws(token).getBody();
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
        var bookings = bookingRepository.findAllByUserId(uuid);
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/by-email")
    public ResponseEntity<?> getTripsByEmail(@RequestParam("email") String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body("User not found");
        }
        User user = userOpt.get();
        java.util.List<Booking> bookings = bookingRepository.findAllByUserId(user.getId());
        // Extract unique trips from bookings
        java.util.List<Trip> trips = bookings.stream()
            .map(Booking::getTrip)
            .distinct()
            .toList();
        return ResponseEntity.ok(trips);
    }

    public static class BookSeatRequest {
        private Long tripId;
        private String seatLabel;
        public Long getTripId() { return tripId; }
        public void setTripId(Long tripId) { this.tripId = tripId; }
        public String getSeatLabel() { return seatLabel; }
        public void setSeatLabel(String seatLabel) { this.seatLabel = seatLabel; }
    }
}
