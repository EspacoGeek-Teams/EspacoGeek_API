package com.espacogeek.geek.services.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.espacogeek.geek.models.MediaModel;
import com.espacogeek.geek.models.SeasonModel;
import com.espacogeek.geek.repositories.SeasonRepository;
import com.espacogeek.geek.services.SeasonService;

@Service
public class SeasonServiceImpl implements SeasonService {
    @Autowired
    private SeasonRepository seasonRepository;

    @Override
    public List<SeasonModel> saveAll(List<SeasonModel> seasons) {
        return seasonRepository.saveAll(seasons);
    }

    @Override
    public List<SeasonModel> findAll(MediaModel media) {
        return seasonRepository.findByMedia(media);
    }
}
