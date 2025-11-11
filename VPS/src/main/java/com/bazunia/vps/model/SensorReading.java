package com.bazunia.vps.model;

import com.fasterxml.jackson.annotation.JsonIgnore; // <<< DODAJ TEN IMPORT
import jakarta.persistence.*;
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

    @JsonIgnore // <<< DODAJ TĘ ADNOTACJĘ
    @ManyToOne
    @JoinColumn(name = "gateway_id", nullable = false)
    private Gateway gateway;

    @JsonIgnore // <<< DODAJ TĘ ADNOTACJĘ
    @ManyToOne
    @JoinColumn(name = "sensor_id", nullable = false)
    private Sensor sensor;

    @Column(name = "sensor_value")
    private String value;

    private long timestamp;
}