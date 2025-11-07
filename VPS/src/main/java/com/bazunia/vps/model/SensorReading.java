package com.bazunia.vps.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
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

    private String gatewayId;
    private String sensorId;
    private String type;

    // --- POPRAWKA TUTAJ ---
    @Column(name = "sensor_value") // Mówi Springowi, by w bazie SQL ta kolumna nazywała się "sensor_value"
    private String value; // W kodzie Java nadal możesz używać "value"

    private long timestamp;
}