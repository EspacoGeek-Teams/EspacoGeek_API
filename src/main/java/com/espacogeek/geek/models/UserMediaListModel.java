package com.espacogeek.geek.models;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_media_list")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserMediaListModel implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_user_media_list")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull
    private UserModel user;

    @ManyToOne
    @JoinColumn(name = "media_id", nullable = false)
    @NotNull
    private MediaModel media;

    @Column(name = "media_type", nullable = false, length = 50)
    @NotNull
    private String mediaType;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "score")
    private Float score;

    @Column(name = "progress")
    private Integer progress;
}
