package com.example.yatrisewa.controller;

import com.example.yatrisewa.entities.BusSchedule;
import com.example.yatrisewa.models.ResponseModel;
import com.example.yatrisewa.services.BusScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schedule")
public class BusScheduleController {
    @Autowired
    private BusScheduleService busScheduleService;

    @PostMapping("/add")
    public ResponseModel<BusSchedule> addBusSchedule(@RequestBody BusSchedule busSchedule) {
        final BusSchedule schedule = busScheduleService.addSchedule(busSchedule);
        return new ResponseModel<>(HttpStatus.OK.value(), "Schedule added successfully", schedule);
    }

    @GetMapping("/all")
    public ResponseEntity<List<BusSchedule>> getAllBusSchedules() {
        return ResponseEntity.ok(busScheduleService.getAllBusSchedules());
    }

    @GetMapping("/{routeName}")
    public ResponseEntity<List<BusSchedule>> getBusScheduleByRouteName(@PathVariable(name = "routeName") String routeName) {
        return ResponseEntity.ok(busScheduleService.getSchedulesByRoute(routeName));
    }
}
