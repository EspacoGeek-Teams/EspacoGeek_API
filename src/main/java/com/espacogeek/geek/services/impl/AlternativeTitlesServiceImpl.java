package com.espacogeek.geek.services.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.espacogeek.geek.models.AlternativeTitleModel;
import com.espacogeek.geek.models.MediaModel;
import com.espacogeek.geek.repositories.AlternativeTitlesRepository;
import com.espacogeek.geek.services.AlternativeTitlesService;

@Service
public class AlternativeTitlesServiceImpl implements AlternativeTitlesService {
    @Autowired
    private AlternativeTitlesRepository alternativeTitlesRepository;

    public List<AlternativeTitleModel> saveAll(List<AlternativeTitleModel> alternativeTitles) throws DataIntegrityViolationException {
        return alternativeTitlesRepository.saveAll(alternativeTitles);
    }

    @Override
    public List<AlternativeTitleModel> findAll(MediaModel media) {
        return alternativeTitlesRepository.findByMedia(media);
    }
}
