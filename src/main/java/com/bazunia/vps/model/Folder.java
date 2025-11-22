package com.bazunia.vps.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@Entity
@Table(name = "folders")
public class Folder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column
    private String color; // Np. format HEX: "#FF5733"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "folder_gateways",
            joinColumns = @JoinColumn(name = "folder_id"),
            inverseJoinColumns = @JoinColumn(name = "gateway_id")
    )
    private Set<Gateway> gateways = new HashSet<>();

    // ⭐️⭐️⭐️ NOWA SEKCJA (Krok 2) ⭐️⭐️⭐️
    @ManyToMany(fetch = FetchType.LAZY) // Używamy LAZY dla wydajności
    @JoinTable(
            name = "folder_sensors", // Nazwa nowej tabeli łączącej
            joinColumns = @JoinColumn(name = "folder_id"),
            inverseJoinColumns = @JoinColumn(name = "sensor_id")
    )
    private Set<Sensor> sensors = new HashSet<>();
    // ⭐️⭐️⭐️ KONIEC NOWEJ SEKCJI ⭐️⭐️⭐️

    public Folder(String name, String color, User owner) {
        this.name = name;
        this.color = color;
        this.owner = owner;
    }

    // Gettery i Settery dla 'sensors' zostaną automatycznie dodane przez Lombok (@Data)
}