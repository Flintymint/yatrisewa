package com.example.travel_yatra.travel_yatra.model;

import jakarta.persistence.*;
import java.util.HashMap;
import java.util.Map;

@Entity
public class Bus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String busNumber;

    @ManyToOne(fetch = FetchType.EAGER)
    private BusCategory category;

    @Column(nullable = false)
    private boolean available;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private JourneyStatus journeyStatus;

    @Column(nullable = true)
    private String reason;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "bus_seat_status", joinColumns = @JoinColumn(name = "bus_id"))
    @MapKeyColumn(name = "seat_label")
    @Column(name = "status")
    private Map<String, String> seatStatus = new HashMap<>(); // e.g., {"A1": "available", "A2": "booked"}

    public enum JourneyStatus {
        departed, arrived
    }

    public Bus() {}

    public Bus(String busNumber, BusCategory category, boolean available) {
        this.busNumber = busNumber;
        this.category = category;
        this.available = available;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBusNumber() { return busNumber; }
    public void setBusNumber(String busNumber) { this.busNumber = busNumber; }
    public BusCategory getCategory() { return category; }
    public void setCategory(BusCategory category) { this.category = category; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
    public JourneyStatus getJourneyStatus() { return journeyStatus; }
    public void setJourneyStatus(JourneyStatus journeyStatus) { this.journeyStatus = journeyStatus; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Map<String, String> getSeatStatus() { return seatStatus; }
    public void setSeatStatus(Map<String, String> seatStatus) { this.seatStatus = seatStatus; }
}
