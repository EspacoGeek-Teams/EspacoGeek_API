package com.espacogeek.geek.models;

import java.io.Serializable;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
public class UserModel implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_user")
    private Integer id;

    @Size(min = 3, max = 20, message = "Invalid size")
    @Column(name = "username", nullable = false)
    private String username;

    @Size(max = 50, message = "Invalid size")
    @Column(name = "email", nullable = false, unique = true)
    @Email(message = "Invalid email.")
    private String email;

    @Size(max = 255, message = "Invalid size")
    @Column(name = "jwt_token", nullable = true)
    private String jwtToken;

    @Size(min = 8, max = 70, message = "Invalid size")
    @Column(name = "password", nullable = false)
    private byte[] password;

    @OneToMany(mappedBy = "user", orphanRemoval = true)
    private List<UserLibraryModel> userLibrary;
}
