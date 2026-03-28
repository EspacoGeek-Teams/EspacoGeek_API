package com.espacogeek.geek.models;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
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
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    @Column(name = "id_user_media_list", columnDefinition = "BINARY(16)", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull
    private UserModel user;

    @ManyToOne
    @JoinColumn(name = "media_id", nullable = false)
    @NotNull
    private MediaModel media;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "score")
    private Float score;

    @Column(name = "progress")
    private Integer progress;

    @Column(name = "start_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date startDate;

    @Column(name = "finish_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date finishDate;

    @Column(name = "time_spent")
    private Integer timeSpent;

    @Column(name = "note", length = 2000)
    private String note;
}
