package com.espacogeek.geek.services.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.espacogeek.geek.models.GenreModel;
import com.espacogeek.geek.models.MediaModel;
import com.espacogeek.geek.repositories.GenreRepository;
import com.espacogeek.geek.services.GenreService;

@Service
public class GenreServiceImpl implements GenreService {

    @Autowired
    private GenreRepository genreRepository;

    /**
     * @see GenreService#findAllByName(List<String>)
     */
    @Override
    public List<GenreModel> findAllByNames(List<String> names) {
        return genreRepository.findAllByNameIn(names);
    }

    /**
     * @see GenreService#findAll(MediaModel)
     */
    @Override
    public List<GenreModel> findAll(MediaModel media) {
        return genreRepository.findByMediaId(media.getId());
    }

    /**
     * @see GenreService#saveAll(List<GenreModel>)
     */
    @Override
    public List<GenreModel> saveAll (List<GenreModel> genres) {
        return genreRepository.saveAll(genres);
    }
}
