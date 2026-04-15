package com.espacogeek.geek.models;

import java.io.Serializable;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@EqualsAndHashCode(exclude = {"medias"})
@Table(name = "media_categories")
public class MediaCategoryModel implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_media_category")
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_category")
    private CategoryType name;

    public String getTypeCategory() {
        return name != null ? name.name() : null;
    }

    @OneToMany(mappedBy = "mediaCategory")
    @Transient
    private List<MediaModel> medias;
}
