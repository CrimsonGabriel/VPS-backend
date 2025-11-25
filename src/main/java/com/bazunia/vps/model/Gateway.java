package com.bazunia.vps.model;

import jakarta.persistence.*;
// <<< POPRAWKA: Zmiana importów Lombok >>>
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonIgnore;

// <<< POPRAWKA: Usunięto @Data >>>
@Getter
@Setter
@Entity
@Table(name = "gateways")
// <<< POPRAWKA: Dodano @ToString z wykluczeniami, aby uniknąć pętli i CME >>>
@ToString(exclude = {"owner", "sensors", "readings", "permissions", "linkedFolders"})
public class Gateway {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id")
    private User owner;

    @Column(nullable = false, length = 100)
    private String name;
    @Column(length = 20)
    private String status;
    @Column(length = 50)
    private String version;
    @Column(length = 100)
    private String folder;
    @Column
    private String description;
    private LocalDateTime lastSeen;
    @CreationTimestamp
    private LocalDateTime createdAt;

    // Relacje do innych tabel
    @OneToMany(mappedBy = "gateway", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Sensor> sensors;

    @JsonIgnore
    @OneToMany(mappedBy = "gateway", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<SensorReading> readings;

    @JsonIgnore
    @OneToMany(mappedBy = "gateway", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserGatewayPermission> permissions;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "folder_gateways", // Nazwa tabeli z Twojego screena SQL
            joinColumns = @JoinColumn(name = "gateway_id"),
            inverseJoinColumns = @JoinColumn(name = "folder_id")
    )
    private Set<Folder> linkedFolders = new HashSet<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Gateway gateway = (Gateway) o;
        return id != null && id.equals(gateway.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode(); // Użyj stałej wartości lub ID, jeśli masz pewność, że nie jest nullem
    }
}