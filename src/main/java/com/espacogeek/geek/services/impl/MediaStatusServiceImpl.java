package com.espacogeek.geek.services.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.espacogeek.geek.models.MediaStatusModel;
import com.espacogeek.geek.repositories.MediaStatusRepository;
import com.espacogeek.geek.services.MediaStatusService;

/**
 * An Implementation class of MediaStatusService @see MediaStatusService
 */
@Service
public class MediaStatusServiceImpl implements MediaStatusService {

    @Autowired
    private MediaStatusRepository mediaStatusRepository;

    /**
     * @see MediaStatusService#findAll()
     */
    @Override
    public List<MediaStatusModel> findAll() {
        return mediaStatusRepository.findAll();
    }
}
