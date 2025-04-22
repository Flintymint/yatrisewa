package com.example.yatrisewa.services.impl;

import com.example.yatrisewa.entities.BusRoute;
import com.example.yatrisewa.entities.BusSchedule;
import com.example.yatrisewa.models.ReservationApiException;
import com.example.yatrisewa.repos.BusRouteRepository;
import com.example.yatrisewa.repos.BusScheduleRepository;
import com.example.yatrisewa.services.BusScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BusScheduleServiceImpl implements BusScheduleService {
    @Autowired
    private BusScheduleRepository busScheduleRepository;

    @Autowired
    private BusRouteRepository busRouteRepository;

    @Override
    public BusSchedule addSchedule(BusSchedule busSchedule) throws ReservationApiException{
        final boolean exists =
                busScheduleRepository.existsByBusAndBusRouteAndDepartureTime(
                    busSchedule.getBus(),
                    busSchedule.getBusRoute(),
                    busSchedule.getDepartureTime());
        if (exists) {
            throw new ReservationApiException(HttpStatus.CONFLICT, "Schedule already exists");
        }
        return busScheduleRepository.save(busSchedule);
    }

    @Override
    public List<BusSchedule> getAllBusSchedules() {
        return busScheduleRepository.findAll();
    }

    @Override
    public List<BusSchedule> getSchedulesByRoute(String routeName) {
        final BusRoute busRoute = busRouteRepository.findByRouteName(routeName).orElseThrow(() -> new ReservationApiException(HttpStatus.BAD_REQUEST, "Route not found"));
        return busScheduleRepository.findByBusRoute(busRoute).orElseThrow(() -> new ReservationApiException(HttpStatus.BAD_REQUEST, "Route not found"));
    }
}
