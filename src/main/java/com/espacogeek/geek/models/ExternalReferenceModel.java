package com.espacogeek.geek.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.proxy.HibernateProxy;

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
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        ExternalReferenceModel that = (ExternalReferenceModel) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
