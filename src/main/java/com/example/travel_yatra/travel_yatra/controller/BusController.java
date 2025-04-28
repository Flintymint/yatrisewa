package com.example.travel_yatra.travel_yatra.controller;

import com.example.travel_yatra.travel_yatra.model.Bus;
import com.example.travel_yatra.travel_yatra.model.BusCategory;
import com.example.travel_yatra.travel_yatra.repository.BusRepository;
import com.example.travel_yatra.travel_yatra.repository.BusCategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/buses")
public class BusController {
    @Autowired
    private BusRepository busRepository;
    @Autowired
    private BusCategoryRepository busCategoryRepository;

    // Create a new bus
    @PostMapping
    public ResponseEntity<?> createBus(@RequestBody BusCreateRequest request) {
        Optional<BusCategory> categoryOpt = busCategoryRepository.findById(request.getBusCategoryId());
        if (categoryOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Invalid bus category ID");
        }
        // Always create with available = true
        Bus bus = new Bus(request.getBusNumber(), categoryOpt.get(), true);
        // Initialize seat status to 'available' for all seats
        for (String seat : categoryOpt.get().getSeatLabels()) {
            bus.getSeatStatus().put(seat, "available");
        }
        busRepository.save(bus);
        return ResponseEntity.ok(bus);
    }

    // Read all buses
    @GetMapping
    public List<Bus> getAllBuses() {
        return busRepository.findAll();
    }

    // Filtered search for buses
    @GetMapping("/search")
    public List<Bus> searchBuses(
            @RequestParam(value = "busNumber", required = false) String busNumber,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "available", required = false) Boolean available
    ) {
        List<Bus> buses = busRepository.findAll();
        if (busNumber != null && !busNumber.isEmpty()) {
            String busNumberLower = busNumber.toLowerCase();
            buses = buses.stream().filter(b -> b.getBusNumber() != null && b.getBusNumber().toLowerCase().contains(busNumberLower)).toList();
        }
        if (categoryId != null) {
            buses = buses.stream().filter(b -> b.getCategory() != null && b.getCategory().getId().equals(categoryId)).toList();
        }
        if (available != null) {
            buses = buses.stream().filter(b -> b.isAvailable() == available).toList();
        }
        return buses;
    }

    // Read a bus by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getBusById(@PathVariable Long id) {
        Optional<Bus> busOpt = busRepository.findById(id);
        return busOpt.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Delete a bus by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBus(@PathVariable Long id) {
        if (!busRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        busRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    // DTO for bus creation
    public static class BusCreateRequest {
        private String busNumber;
        private Long busCategoryId;
        public String getBusNumber() { return busNumber; }
        public void setBusNumber(String busNumber) { this.busNumber = busNumber; }
        public Long getBusCategoryId() { return busCategoryId; }
        public void setBusCategoryId(Long busCategoryId) { this.busCategoryId = busCategoryId; }
    }
}
