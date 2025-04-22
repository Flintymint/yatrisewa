package com.example.yatrisewa.services;

import com.example.yatrisewa.entities.Reservation;

import java.util.List;

public interface ReservationService {
    Reservation addReservation(Reservation reservation);
    List<Reservation> getAllReservations();
    List<Reservation> getReservationByScheduleAndDepartureDate(Long schedule, String departureDate);
    List<Reservation> getReservationByMobile(String mobile);
}
