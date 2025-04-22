package com.example.yatrisewa.entities;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "bus_route")
public class BusRoute {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long routeId;
    private String routeName;
    private String cityFrom;
    private String cityTo;
    private Double distanceInKm;
}
