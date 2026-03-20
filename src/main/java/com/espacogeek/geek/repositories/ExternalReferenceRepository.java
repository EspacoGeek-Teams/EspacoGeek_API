package com.espacogeek.geek.repositories;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.espacogeek.geek.models.ExternalReferenceModel;
import com.espacogeek.geek.models.MediaModel;
import com.espacogeek.geek.models.TypeReferenceModel;

@Repository
public interface ExternalReferenceRepository<T> extends JpaRepository<ExternalReferenceModel, Integer> {
    Optional<ExternalReferenceModel> findByReferenceAndTypeReference (String reference, TypeReferenceModel typeReference);

    Optional<ExternalReferenceModel> findByMedia(MediaModel media);

    List<ExternalReferenceModel> findAllByMedia(MediaModel media);

    List<ExternalReferenceModel> findAllByMediaIn(Collection<MediaModel> medias);

    boolean existsByMediaId(Integer id);
}
