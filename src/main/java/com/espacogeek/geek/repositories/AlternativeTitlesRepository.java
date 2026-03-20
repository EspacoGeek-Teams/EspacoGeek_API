package com.espacogeek.geek.repositories;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.espacogeek.geek.models.AlternativeTitleModel;
import com.espacogeek.geek.models.MediaModel;

@Repository
public interface AlternativeTitlesRepository extends JpaRepository<AlternativeTitleModel, Integer> {

    List<AlternativeTitleModel> findByMediaIn(Collection<MediaModel> medias);

}
