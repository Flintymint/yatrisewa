package com.example.travel_yatra.travel_yatra.controller;

import com.example.travel_yatra.travel_yatra.model.BusStop;
import com.example.travel_yatra.travel_yatra.repository.BusStopRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/bus-stops")
public class BusStopController {
    @Autowired
    private BusStopRepository busStopRepository;

    @GetMapping
    public List<BusStop> getAllBusStops() {
        return busStopRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getBusStopById(@PathVariable UUID id) {
        Optional<BusStop> busStop = busStopRepository.findById(id);
        return busStop.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createBusStop(@RequestBody BusStop busStop) {
        if (busStop.getDisplayName() == null || busStop.getDisplayName().isEmpty() ||
            busStop.getLatitude() == null || busStop.getLongitude() == null) {
            return ResponseEntity.badRequest().body("Invalid bus stop data");
        }
        BusStop saved = busStopRepository.save(busStop);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBusStop(@PathVariable UUID id) {
        if (!busStopRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        busStopRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
