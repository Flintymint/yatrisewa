package com.example.travel_yatra.travel_yatra.controller;

import com.example.travel_yatra.travel_yatra.model.BusCategory;
import com.example.travel_yatra.travel_yatra.repository.BusCategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/bus-categories")
public class BusCategoryController {
    @Autowired
    private BusCategoryRepository busCategoryRepository;

    // Create a new bus category
    @PostMapping
    public ResponseEntity<?> createCategory(@RequestBody BusCategory busCategory) {
        if (busCategoryRepository.findByName(busCategory.getName()) != null) {
            return ResponseEntity.badRequest().body("Category already exists");
        }
        busCategoryRepository.save(busCategory);
        return ResponseEntity.ok(busCategory);
    }

    // Get all categories
    @GetMapping
    public List<BusCategory> getAllCategories() {
        return busCategoryRepository.findAll();
    }

    // Get category by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getCategoryById(@PathVariable Long id) {
        Optional<BusCategory> categoryOpt = busCategoryRepository.findById(id);
        return categoryOpt.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Delete category by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        if (!busCategoryRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        busCategoryRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    // Add the 3 static categories
    @PostMapping("/init-defaults")
    public ResponseEntity<?> addDefaults() {
        try {
            // AC Economy (32 seats)
            List<String> acEconomySeats = Arrays.asList(
                "A1","A2","A3","A4","B1","B2","B3","B4",
                "A5","A6","A7","A8","B5","B6","B7","B8",
                "A9","A10","A11","A12","B9","B10","B11","B12",
                "A13","A14","A15","A16","B13","B14","B15","B16"
            );
            // AC Business (18 seats)
            List<String> acBusinessSeats = Arrays.asList(
                "A1","A2","A3","B1","B2","B3",
                "A4","A5","A6","B4","B5","B6",
                "A7","A8","A9","B7","B8","B9"
            );
            // Economy (32 seats, same as AC Economy)
            List<String> economySeats = Arrays.asList(
                "A1","A2","A3","A4","B1","B2","B3","B4",
                "A5","A6","A7","A8","B5","B6","B7","B8",
                "A9","A10","A11","A12","B9","B10","B11","B12",
                "A13","A14","A15","A16","B13","B14","B15","B16"
            );

            BusCategory acEconomy = new BusCategory("AC Economy", acEconomySeats);
            BusCategory acBusiness = new BusCategory("AC Business", acBusinessSeats);
            BusCategory economy = new BusCategory("Economy", economySeats);

            busCategoryRepository.save(acEconomy);
            busCategoryRepository.save(acBusiness);
            busCategoryRepository.save(economy);
            return ResponseEntity.ok(busCategoryRepository.findAll());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
}
