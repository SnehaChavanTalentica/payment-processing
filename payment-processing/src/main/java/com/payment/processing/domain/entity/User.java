package com.payment.processing.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Column(name = "username", nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "roles", length = 500)
    private String roles;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;
}

