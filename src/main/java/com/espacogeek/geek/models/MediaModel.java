package com.espacogeek.geek.models;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;
import java.util.Set;

import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.proxy.HibernateProxy;

@Entity
@Table(name = "medias", indexes = {
    @Index(name = "idx_name_media", columnList = "name_media")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MediaModel implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gen_medias")
    @SequenceGenerator(name = "gen_medias", sequenceName = "seq_medias", allocationSize = 50)
    @Column(name = "id_media")
    private Integer id;

    @Column(name = "name_media")
    private String name;

    @Column(name = "episode_count")
    private Integer totalEpisodes;

    @Column(name = "episode_length_in_minutes")
    private Integer episodeLength;

    @Column(name = "about", length = 10000)
    private String about;

    @Column(name = "url_cover")
    private String cover;

    @Column(name = "url_banner")
    private String banner;

    @JoinColumn(name = "id_category", nullable = false)
    @ManyToOne
    @NotNull
    private MediaCategoryModel mediaCategory;

    @OneToMany(mappedBy = "media", fetch = FetchType.LAZY)
    private Set<ExternalReferenceModel> externalReference;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "medias_has_companies",
        joinColumns = @JoinColumn(name = "medias_id_media"),
        inverseJoinColumns = @JoinColumn(name = "companies_id_company"))
    private Set<CompanyModel> company;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "medias_has_people",
        joinColumns = @JoinColumn(name = "medias_id_media"),
        inverseJoinColumns = @JoinColumn(name = "people_id_person"))
    private Set<PeopleModel> people;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "medias_has_genres",
        joinColumns = @JoinColumn(name = "medias_id_media"),
        inverseJoinColumns = @JoinColumn(name = "genres_id_genre"))
    private Set<GenreModel> genre;

    @Column(name = "update_at")
    @UpdateTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    private Date updateAt;

    @OneToMany(mappedBy = "media", fetch = FetchType.LAZY)
    private Set<AlternativeTitleModel> alternativeTitles;

    @OneToMany(mappedBy = "media", fetch = FetchType.LAZY)
    private Set<SeasonModel> season;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        MediaModel that = (MediaModel) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
