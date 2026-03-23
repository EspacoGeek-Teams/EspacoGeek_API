package com.espacogeek.geek.services;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;

import com.espacogeek.geek.models.AlternativeTitleModel;
import com.espacogeek.geek.models.MediaModel;

public interface AlternativeTitlesService {
    public List<AlternativeTitleModel> saveAll(List<AlternativeTitleModel> alternativeTitles) throws DataIntegrityViolationException;

    public List<AlternativeTitleModel> findAll(MediaModel media);
}
