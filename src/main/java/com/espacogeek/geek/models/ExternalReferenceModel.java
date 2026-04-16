package com.espacogeek.geek.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "externals_References")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExternalReferenceModel implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gen_externals_references")
    @SequenceGenerator(name = "gen_externals_references", sequenceName = "seq_externals_references", allocationSize = 50)
    @Column(name = "id_external_reference")
    private Integer id;

    @Column(name = "reference")
    private String reference;

    @ManyToOne
    @NotNull
    @JoinColumn(name = "medias_id_media", nullable = false)
    private MediaModel media;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "type_reference", nullable = false)
    private TypeReferenceModel typeReference;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExternalReferenceModel that)) return false;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return getClass().hashCode();
    }
}
