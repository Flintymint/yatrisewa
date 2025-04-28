package com.example.travel_yatra.travel_yatra.controller;

import com.example.travel_yatra.travel_yatra.model.Booking;
import com.example.travel_yatra.travel_yatra.model.Notification;
import com.example.travel_yatra.travel_yatra.repository.BookingRepository;
import com.example.travel_yatra.travel_yatra.repository.NotificationRepository;
import com.example.travel_yatra.travel_yatra.repository.TripRepository;
import com.example.travel_yatra.travel_yatra.repository.UserRepository;
import com.example.travel_yatra.travel_yatra.service.KhaltiService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/**
 * Controller for handling Khalti payment-related requests.
 */
@RestController
@RequestMapping("/api/payments/khalti")
public class KhaltiPaymentController {
    @Autowired
    private KhaltiService khaltiService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    // 1. Initiate Payment (Generate pidx)
    @PostMapping("/initiate")
    public ResponseEntity<?> initiatePayment(@RequestBody KhaltiInitiateRequest req) {
        Map<String, Object> result = khaltiService.initiatePayment(req);
        if (result.containsKey("error")) {
            return ResponseEntity.status(400).body(result);
        }
        return ResponseEntity.ok(result);
    }

    // 2. Lookup Payment Status (After Payment)
    @PostMapping("/lookup")
    public ResponseEntity<?> lookupPayment(@RequestBody KhaltiLookupAndBookRequest req, HttpServletRequest httpRequest) {
        // 1. Lookup payment status from Khalti
        Map<String, Object> result = khaltiService.lookupPayment(req.getPidx());
        if (result.containsKey("error")) {
            return ResponseEntity.status(400).body(result);
        }
        // 2. If payment is completed, reserve the seat(s)
        String status = (String) result.get("status");
        if ("Completed".equalsIgnoreCase(status)) {
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
            java.util.UUID uuid;
            try {
                uuid = java.util.UUID.fromString(userId);
            } catch (Exception e) {
                return ResponseEntity.status(400).body("Invalid user id in token");
            }
            var userOpt = userRepository.findById(uuid);
            var tripOpt = tripRepository.findById(req.getTripId());
            if (userOpt.isEmpty() || tripOpt.isEmpty()) {
                return ResponseEntity.status(404).body("User or trip not found");
            }
            java.util.List<Booking> bookings = new java.util.ArrayList<>();
            java.util.List<String> alreadyBooked = new java.util.ArrayList<>();
            for (String seatLabel : req.getSeatLabels()) {
                if (bookingRepository.existsByTripIdAndSeatLabel(req.getTripId(), seatLabel)) {
                    alreadyBooked.add(seatLabel);
                    continue;
                }
                Booking booking = new Booking(userOpt.get(), tripOpt.get(), seatLabel);
                booking.setPaid(true);
                booking.setKhaltiTxnId((String) result.get("idx"));
                booking.setKhaltiDetails(result.toString());
                bookingRepository.save(booking);
                bookings.add(booking);
                try {
                    Notification notification = new Notification(
                        userOpt.get().getId(),
                        "Booking successful for trip ID: " + req.getTripId() + ", seat: " + seatLabel,
                        "BOOKING_SUCCESS"
                    );
                    notificationRepository.save(notification);
                } catch (Exception e) {
                    System.err.println("[ERROR] Failed to create notification for Khalti booking: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            // Optionally, return reserved seats for the trip
            java.util.List<String> reservedSeats = bookingRepository.findAllByTripId(req.getTripId())
                .stream().map(Booking::getSeatLabel).toList();
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("reservedSeats", reservedSeats);
            response.put("bookings", bookings);
            response.put("alreadyBooked", alreadyBooked);
            response.put("paymentStatus", status);
            return ResponseEntity.ok(response);
        }
        // If payment is not completed, just return the payment status
        return ResponseEntity.ok(result);
    }

    public static class KhaltiInitiateRequest {
        public String return_url;
        public String website_url;
        public int amount;
        public String purchase_order_id;
        public String purchase_order_name;
        public CustomerInfo customer_info;
        // Optionals
        public Object amount_breakdown;
        public Object product_details;
        public static class CustomerInfo {
            public String name;
            public String email;
            public String phone;
        }
    }

    public static class KhaltiLookupRequest {
        public String pidx;
        public String getPidx() { return pidx; }
        public void setPidx(String pidx) { this.pidx = pidx; }
    }

    // Request body for lookup and booking
    public static class KhaltiLookupAndBookRequest {
        public String pidx;
        public Long tripId;
        public java.util.List<String> seatLabels;
        public String getPidx() { return pidx; }
        public void setPidx(String pidx) { this.pidx = pidx; }
        public Long getTripId() { return tripId; }
        public void setTripId(Long tripId) { this.tripId = tripId; }
        public java.util.List<String> getSeatLabels() { return seatLabels; }
        public void setSeatLabels(java.util.List<String> seatLabels) { this.seatLabels = seatLabels; }
    }
}
