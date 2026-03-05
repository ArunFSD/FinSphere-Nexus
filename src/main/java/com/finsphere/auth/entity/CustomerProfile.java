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
    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(name = "care_of", length = 100)
    private String careOf;

    @NotBlank(message = "Address is mandatory")
    @Column(name = "address", length = 500)
    private String address;

    @OneToOne
    @MapsId // This tells Hibernate to use 'id' as the FK to User
    @JoinColumn(name = "user_id") // This must match the SQL column name
    private User user;
}