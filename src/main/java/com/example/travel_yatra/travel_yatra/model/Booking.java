package com.example.travel_yatra.travel_yatra.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "booking")
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(nullable = false)
    private String seatLabel;

    @Column(nullable = false, updatable = false)
    private Instant bookedAt = Instant.now();

    @Column(nullable = false)
    private boolean paid = false;

    @Column(name = "khalti_txn_id")
    private String khaltiTxnId;

    @Column(name = "khalti_details", columnDefinition = "TEXT")
    private String khaltiDetails;

    public Booking() {}
    public Booking(User user, Trip trip, String seatLabel) {
        this.user = user;
        this.trip = trip;
        this.seatLabel = seatLabel;
    }
    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Trip getTrip() { return trip; }
    public void setTrip(Trip trip) { this.trip = trip; }
    public String getSeatLabel() { return seatLabel; }
    public void setSeatLabel(String seatLabel) { this.seatLabel = seatLabel; }
    public Instant getBookedAt() { return bookedAt; }
    public void setBookedAt(Instant bookedAt) { this.bookedAt = bookedAt; }
    public boolean isPaid() { return paid; }
    public void setPaid(boolean paid) { this.paid = paid; }
    public String getKhaltiTxnId() { return khaltiTxnId; }
    public void setKhaltiTxnId(String khaltiTxnId) { this.khaltiTxnId = khaltiTxnId; }
    public String getKhaltiDetails() { return khaltiDetails; }
    public void setKhaltiDetails(String khaltiDetails) { this.khaltiDetails = khaltiDetails; }
}
