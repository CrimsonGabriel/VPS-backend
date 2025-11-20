package com.bazunia.vps.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "update_assignments")
public class UpdateAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "update_id")
    private SystemUpdate systemUpdate;

    private Long targetId;
    private Long recipientUserId;

    @Enumerated(EnumType.STRING)
    private AssignmentStatus status;

    private int deferCount;
    private LocalDateTime lastActionAt;

    @PrePersist
    protected void onCreate() {
        lastActionAt = LocalDateTime.now();
        if (status == null) status = AssignmentStatus.PENDING;
    }

    public enum AssignmentStatus { PENDING, DEFERRED, COMPLETED, FAILED }
}