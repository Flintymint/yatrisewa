package com.example.travel_yatra.travel_yatra.controller;

import com.example.travel_yatra.travel_yatra.model.BusCategory;
import com.example.travel_yatra.travel_yatra.repository.BusCategoryRepository;
import com.example.travel_yatra.travel_yatra.repository.BusRepository;
import com.example.travel_yatra.travel_yatra.repository.TripRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class BusControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private BusRepository busRepository;
    @Autowired
    private BusCategoryRepository busCategoryRepository;
    @Autowired
    private TripRepository tripRepository;
    @Autowired
    private ObjectMapper objectMapper;

    private BusCategory testCategory;

    @BeforeEach
    void setup() {
        // Clean up dependent entities first
        tripRepository.deleteAll();
        busRepository.deleteAll();
        busCategoryRepository.deleteAll();
        testCategory = new BusCategory();
        testCategory.setName("Deluxe");
        testCategory.setSeatLabels(Collections.singletonList("A1"));
        busCategoryRepository.save(testCategory);
    }

    @Test
    void testCreateAndSearchBus() throws Exception {
        // Create bus
        String busNumber = "BUS100";
        String createPayload = objectMapper.writeValueAsString(new BusController.BusCreateRequest() {{
            setBusNumber(busNumber);
            setBusCategoryId(testCategory.getId());
        }});

        mockMvc.perform(post("/api/buses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.busNumber", is(busNumber)));

        // Search by busNumber
        mockMvc.perform(get("/api/buses/search")
                .param("busNumber", busNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].busNumber", is(busNumber)));

        // Search by categoryId
        mockMvc.perform(get("/api/buses/search")
                .param("categoryId", String.valueOf(testCategory.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category.id", is(testCategory.getId().intValue())));

        // Search by availability
        mockMvc.perform(get("/api/buses/search")
                .param("available", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].available", is(true)));
    }
}
