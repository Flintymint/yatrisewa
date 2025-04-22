package com.example.yatrisewa.services;

import com.example.yatrisewa.entities.Bus;

import java.util.List;

public interface BusService {
    Bus addBus(Bus bus);

    List<Bus> getAllBus();
}
