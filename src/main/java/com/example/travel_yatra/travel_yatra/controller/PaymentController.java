package com.example.travel_yatra.travel_yatra.controller;

import com.example.travel_yatra.travel_yatra.model.Booking;
import com.example.travel_yatra.travel_yatra.model.Trip;
import com.example.travel_yatra.travel_yatra.model.User;
import com.example.travel_yatra.travel_yatra.repository.BookingRepository;
import com.example.travel_yatra.travel_yatra.service.KhaltiService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments/khalti")
public class PaymentController {
    @Autowired
    private KhaltiService khaltiService;
    @Autowired
    private BookingRepository bookingRepository;

    @PostMapping("/verify")
    @Transactional
    public ResponseEntity<?> verifyKhaltiPayment(@RequestBody KhaltiVerifyRequest request, HttpServletRequest httpRequest) {
        // Extract user ID from JWT (required for booking creation)
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(new KhaltiVerifyResponse(false, "Missing or invalid Authorization header.", null));
        }
        String tokenJwt = authHeader.substring(7);
        String userId;
        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(System.getenv("JWT_SECRET").getBytes())
                .build()
                .parseClaimsJws(tokenJwt)
                .getBody();
            userId = claims.get("id", String.class);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(new KhaltiVerifyResponse(false, "Invalid or expired JWT.", null));
        }
        UUID uuid;
        try {
            uuid = UUID.fromString(userId);
        } catch (Exception e) {
            return ResponseEntity.status(400).body(new KhaltiVerifyResponse(false, "Invalid user id in token.", null));
        }
        // Check if seat is already booked
        if (bookingRepository.existsByTripIdAndSeatLabel(request.getTripId(), request.getSeatLabel())) {
            return ResponseEntity.status(409).body(new KhaltiVerifyResponse(false, "Seat already booked for this trip.", null));
        }
        KhaltiService.KhaltiVerifyResult result = khaltiService.verifyPaymentAndGetDetails(request.getToken(), request.getAmount());
        if (result.success) {
            // Create booking only after successful payment
            Booking booking = new Booking();
            booking.setTrip(new Trip());
            booking.getTrip().setId(request.getTripId());
            booking.setSeatLabel(request.getSeatLabel());
            booking.setUser(new User());
            booking.getUser().setId(uuid);
            booking.setPaid(true);
            booking.setKhaltiTxnId(result.idx);
            booking.setKhaltiDetails(result.rawResponse);
            bookingRepository.save(booking);
            return ResponseEntity.ok(new KhaltiVerifyResponse(true, "Payment verified and booking confirmed.", booking));
        } else {
            return ResponseEntity.status(402).body(new KhaltiVerifyResponse(false, "Payment verification failed.", null));
        }
    }

    public static class KhaltiVerifyRequest {
        private String token;
        private int amount;
        private Long tripId;
        private String seatLabel;
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public int getAmount() { return amount; }
        public void setAmount(int amount) { this.amount = amount; }
        public Long getTripId() { return tripId; }
        public void setTripId(Long tripId) { this.tripId = tripId; }
        public String getSeatLabel() { return seatLabel; }
        public void setSeatLabel(String seatLabel) { this.seatLabel = seatLabel; }
    }

    public static class KhaltiVerifyResponse {
        private boolean success;
        private String message;
        private Booking booking;
        public KhaltiVerifyResponse(boolean success, String message, Booking booking) {
            this.success = success;
            this.message = message;
            this.booking = booking;
        }
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Booking getBooking() { return booking; }
    }
}
