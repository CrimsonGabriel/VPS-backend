package com.bazunia.vps.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "system_updates")
public class SystemUpdate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private String version;

    @Enumerated(EnumType.STRING)
    private UpdateUrgency urgency;

    @Enumerated(EnumType.STRING)
    private UpdateTargetType targetType;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum UpdateUrgency { REQUIRED, OPTIONAL, CUSTOM }
    public enum UpdateTargetType { APP, GATEWAY, SENSOR }
}