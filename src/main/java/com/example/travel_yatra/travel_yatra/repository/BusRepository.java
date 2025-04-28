package com.example.travel_yatra.travel_yatra.repository;

import com.example.travel_yatra.travel_yatra.model.Bus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusRepository extends JpaRepository<Bus, Long> {
    Bus findByBusNumber(String busNumber);
}
