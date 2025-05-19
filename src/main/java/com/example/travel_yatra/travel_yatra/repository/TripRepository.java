package com.example.travel_yatra.travel_yatra.repository;

import com.example.travel_yatra.travel_yatra.model.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TripRepository extends JpaRepository<Trip, Long>, JpaSpecificationExecutor<Trip> {
    // Find all trips assigned to a specific bus driver
    @Query("SELECT t FROM Trip t WHERE t.busDriver.id = :busDriverId")
    List<Trip> findAllTripsByBusDriverId(@Param("busDriverId") UUID busDriverId);

    // Custom update for driver breakdown
    @Modifying
    @Query(value = "UPDATE trip SET trip_status = :tripStatus, reason = :reason WHERE id = :id AND bus_driver_id = :busDriverId", nativeQuery = true)
    int driverUpdateTripStatusAndReason(@Param("id") Long id, @Param("busDriverId") UUID busDriverId, @Param("tripStatus") String tripStatus, @Param("reason") String reason);

    List<Trip> findAllByFrom_IdAndTo_IdAndDepartureDate(UUID fromId, UUID toId, LocalDate departureDate);
}
