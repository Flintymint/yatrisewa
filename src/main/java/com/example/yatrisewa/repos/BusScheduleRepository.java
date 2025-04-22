package com.example.yatrisewa.repos;

import com.example.yatrisewa.entities.Bus;
import com.example.yatrisewa.entities.BusRoute;
import com.example.yatrisewa.entities.BusSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BusScheduleRepository extends JpaRepository<BusSchedule, Long> {
    Optional<List<BusSchedule>>findByBusRoute(BusRoute busRoute);

    Boolean existsByBusAndBusRouteAndDepartureTime(Bus bus, BusRoute busRoute, String date);
}
