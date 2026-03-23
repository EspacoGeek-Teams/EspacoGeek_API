package com.espacogeek.geek.services;

import java.util.List;

import com.espacogeek.geek.models.GenreModel;
import com.espacogeek.geek.models.MediaModel;

public interface GenreService {
    public List<GenreModel> findAllByNames(List<String> names);

    public List<GenreModel> findAll(MediaModel media);

    public List<GenreModel> saveAll(List<GenreModel> genres);
}
