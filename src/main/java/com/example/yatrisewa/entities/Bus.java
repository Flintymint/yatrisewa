package com.example.yatrisewa.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "bus")
public class Bus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long busId;
    private String busName;
    private String busType;
    private Integer totalSeat;
    @Column(unique = true)
    private String busNumber;

}
