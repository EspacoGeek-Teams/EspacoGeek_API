package com.espacogeek.geek.repositories;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.espacogeek.geek.models.MediaModel;
import com.espacogeek.geek.models.SeasonModel;

@Repository
public interface SeasonRepository extends JpaRepository<SeasonModel, Integer> {

    List<SeasonModel> findByMedia(MediaModel media);

    List<SeasonModel> findByMediaIn(Collection<MediaModel> medias);

}
