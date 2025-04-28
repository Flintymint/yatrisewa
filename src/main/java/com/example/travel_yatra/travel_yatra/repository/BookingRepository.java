package com.example.travel_yatra.travel_yatra.repository;

import com.example.travel_yatra.travel_yatra.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    @Query("SELECT b FROM Booking b WHERE b.trip.id = :tripId")
    List<Booking> findAllByTripId(@Param("tripId") Long tripId);

    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId")
    List<Booking> findAllByUserId(@Param("userId") java.util.UUID userId);

    @Query("SELECT COUNT(b) > 0 FROM Booking b WHERE b.trip.id = :tripId AND b.seatLabel = :seatLabel")
    boolean existsByTripIdAndSeatLabel(@Param("tripId") Long tripId, @Param("seatLabel") String seatLabel);
}
