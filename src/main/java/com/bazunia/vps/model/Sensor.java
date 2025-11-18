package com.bazunia.vps.model;

import jakarta.persistence.*;
// <<< POPRAWKA: Zmiana importów Lombok >>>
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.ColumnDefault;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.HashSet; // ⭐️⭐️⭐️ DODANY BRAKUJĄCY IMPORT ⭐️⭐️⭐️
import com.fasterxml.jackson.annotation.JsonIgnore;

// <<< POPRAWKA: Usunięto @Data >>>
@Getter
@Setter
@Entity
@Table(name = "sensors")
// <<< POPRAWKA: Dodano @ToString z wykluczeniami >>>
@ToString(exclude = {"gateway", "readings", "folders"}) // ⭐️ DODANO 'folders'
public class Sensor {

    @Id

    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gateway_id", nullable = false)
    private Gateway gateway;

    // ... (wszystkie inne pola: name, type, description, interwały, bateria, createdAt)
    @Column(nullable = false, length = 100)
    private String name;
    @Column(length = 50)
    private String type;
    @Column(length = 255)
    private String description;
    @Column(name = "interval_seconds")
    private Integer intervalSeconds;
    @Column(name = "alarm_threshold_low")
    private Double alarmThresholdLow;
    @Column(name = "alarm_threshold_high")
    private Double alarmThresholdHigh;
    @Column(name = "battery_level")
    private Integer batteryLevel;
    @Column(name = "reporting_enabled", nullable = false)
    @ColumnDefault("true")
    private boolean reportingEnabled = true;
    @Column(length = 50)
    private String keyword;

    private LocalDateTime createdAt;

    @JsonIgnore
    @OneToMany(mappedBy = "sensor", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<SensorReading> readings;

    @ManyToMany(mappedBy = "sensors")
    @JsonIgnore // Zapobiegaj pętlom serializacji
    private Set<Folder> folders = new HashSet<>();

    // <<< POPRAWKA: Ręczne dodanie equals() i hashCode() opartych TYLKO na 'id' >>>
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Sensor sensor = (Sensor) o;
        return id != null && id.equals(sensor.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}