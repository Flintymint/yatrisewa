package com.example.travel_yatra.travel_yatra.repository;

import com.example.travel_yatra.travel_yatra.model.BusStop;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface BusStopRepository extends JpaRepository<BusStop, UUID> {
}
