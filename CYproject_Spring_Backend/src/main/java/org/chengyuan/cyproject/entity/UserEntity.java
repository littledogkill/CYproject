package org.chengyuan.cyproject.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "user")
public class UserEntity {
    @Id
    @Column(name = "id", length = 255)
    private String id;

    @Column(name = "username", nullable = false, length = 255)
    private String username;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "email", unique = true, length = 255)
    private String email;

    @Column(name = "avatar", length = 255)
    private String avatar;

    @Column(name = "bio", length = 255)
    private String bio;
}
