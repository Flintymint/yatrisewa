package com.example.travel_yatra.travel_yatra.model;

import jakarta.persistence.*;
import java.util.List;

@Entity
public class BusCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name; // Economy, AC Economy, AC Business

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "bus_category_seat_labels",
        joinColumns = @JoinColumn(name = "bus_category_id")
    )
    @Column(name = "seat_label")
    private List<String> seatLabels; // Static seat labels for this category

    public BusCategory() {}

    public BusCategory(String name, List<String> seatLabels) {
        this.name = name;
        this.seatLabels = seatLabels;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<String> getSeatLabels() { return seatLabels; }
    public void setSeatLabels(List<String> seatLabels) { this.seatLabels = seatLabels; }
}
