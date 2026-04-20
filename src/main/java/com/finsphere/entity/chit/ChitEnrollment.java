package com.finsphere.entity.chit;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "chit_enrollments",
        indexes = {
                @Index(name = "idx_enrollment_user_id", columnList = "user_id"),
                @Index(name = "idx_enrollment_plan_id", columnList = "plan_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChitEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private ChitPlan plan;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "slot_number", nullable = false)
    private Integer slotNumber;

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    @Builder.Default
    @Column(name = "joined_date")
    private LocalDateTime joinedDate = LocalDateTime.now();
}