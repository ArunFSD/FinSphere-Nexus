package com.finsphere.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chit_enrollments")
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

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "joined_date")
    private LocalDateTime joinedDate = LocalDateTime.now();
}
