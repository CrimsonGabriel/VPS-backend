package com.bazunia.vps.model;

import jakarta.persistence.*; // <<< ZMIANA (importy)
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class SensorReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;



    @ManyToOne // <<< ZMIANA: Relacja do bramki
    @JoinColumn(name = "gateway_id", nullable = false) // <<< ZMIANA
    private Gateway gateway; // <<< ZMIANA

    @ManyToOne // <<< ZMIANA: Relacja do czujnika
    @JoinColumn(name = "sensor_id", nullable = false) // <<< ZMIANA
    private Sensor sensor; // <<< ZMIANA

    @Column(name = "sensor_value")
    private String value;

    private long timestamp;
}