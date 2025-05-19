package com.example.travel_yatra.travel_yatra.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "trip")
public class Trip {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private double price;

    @Column(nullable = true)
    private String reason;

    @Column(name = "trip_status", nullable = true)
    private String tripStatus = "scheduled"; // departed, arrived, teardown ( Default is scheduled )

    @Column(name = "departure_date", nullable = false)
    private LocalDate departureDate;

    @Column(name = "departure_time", nullable = true)
    private LocalTime departureTime;

    @ManyToOne(optional = false)
    @JoinColumn(name = "bus_id", nullable = false)
    private Bus bus;

    @ManyToOne(optional = false)
    @JoinColumn(name = "bus_driver_id", nullable = false)
    private User busDriver;

    @ManyToOne(optional = false)
    @JoinColumn(name = "from_stop_id", nullable = false)
    private BusStop from;

    @ManyToOne(optional = false)
    @JoinColumn(name = "to_stop_id", nullable = false)
    private BusStop to;

    public Trip() {
        // Ensure default status if not set by Jackson
        if (this.tripStatus == null) {
            this.tripStatus = "scheduled";
        }
    }

    public enum TripStatus {
        SCHEDULED("scheduled"),
        DEPARTED("departed"),
        ARRIVED("arrived"),
        TEARDOWN("teardown");
        private final String value;
        TripStatus(String value) { this.value = value; }
        @Override
        public String toString() { return value; }
        public static TripStatus fromString(String value) {
            for (TripStatus status : TripStatus.values()) {
                if (status.value.equalsIgnoreCase(value)) return status;
            }
            throw new IllegalArgumentException("Invalid trip status: " + value);
        }
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getTripStatus() { return tripStatus; }
    public void setTripStatus(String tripStatus) { this.tripStatus = tripStatus; }
    public LocalDate getDepartureDate() { return departureDate; }
    public void setDepartureDate(LocalDate departureDate) { this.departureDate = departureDate; }
    public LocalTime getDepartureTime() { return departureTime; }
    public void setDepartureTime(LocalTime departureTime) { this.departureTime = departureTime; }
    public Bus getBus() { return bus; }
    public void setBus(Bus bus) { this.bus = bus; }
    public User getBusDriver() { return busDriver; }
    public void setBusDriver(User busDriver) { this.busDriver = busDriver; }
    public BusStop getFrom() { return from; }
    public void setFrom(BusStop from) { this.from = from; }
    public BusStop getTo() { return to; }
    public void setTo(BusStop to) { this.to = to; }
}
