package com.finsphere.auth.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "customer_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerProfile {

    @Id
    @Column(name = "user_id") // This matches your SQL structure
    private Long id; // Will share the same ID as User (Shared Primary Key)

    @NotBlank(message = "Full name is mandatory")
    private String fullName;

    @Column(name = "care_of")
    private String careOf;

    @NotBlank(message = "Address is mandatory")
    @Column(columnDefinition = "TEXT")
    private String address;

    @OneToOne
    @MapsId // This tells Hibernate to use 'id' as the FK to User
    @JoinColumn(name = "user_id") // This must match the SQL column name
    private User user;
}