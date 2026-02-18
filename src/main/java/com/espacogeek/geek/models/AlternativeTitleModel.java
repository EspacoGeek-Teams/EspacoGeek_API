package com.espacogeek.geek.models;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Table(name = "alternative_titles", indexes = { @Index(name = "idx_title", columnList = "name_title", unique = false) })
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AlternativeTitleModel implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_alternative_title")
    private Integer id;

    @Column(name = "name_title", length = 1000)
    private String name;

    @ManyToOne
    @NotNull
    @JoinColumn(name = "id_media", nullable = false)
    private MediaModel media;
}
