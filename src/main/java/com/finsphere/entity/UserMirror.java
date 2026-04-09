package com.finsphere.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "user_mirror")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserMirror {

    @Id
    private Long userId;

    private String fullName;
    private String phoneNumber;
    private String careOf;
    private Boolean isActive;
}
