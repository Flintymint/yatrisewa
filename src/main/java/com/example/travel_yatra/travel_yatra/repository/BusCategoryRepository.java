package com.example.travel_yatra.travel_yatra.repository;

import com.example.travel_yatra.travel_yatra.model.BusCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusCategoryRepository extends JpaRepository<BusCategory, Long> {
    BusCategory findByName(String name);
}
