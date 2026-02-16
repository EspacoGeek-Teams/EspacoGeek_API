package com.espacogeek.geek.services.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.espacogeek.geek.models.ExternalReferenceModel;
import com.espacogeek.geek.models.MediaModel;
import com.espacogeek.geek.models.TypeReferenceModel;
import com.espacogeek.geek.repositories.ExternalReferenceRepository;
import com.espacogeek.geek.services.ExternalReferenceService;

/**
 * A Implementation class of @see ExternalReferenceService
 */
@Service
public class ExternalReferenceServiceImpl implements ExternalReferenceService {

    @SuppressWarnings("rawtypes")
    @Autowired
    private ExternalReferenceRepository externalReferenceRepository;

    /**
     * @see ExternalReferenceService#findAll(MediaModel)
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<ExternalReferenceModel> findAll(MediaModel media) {
        return this.externalReferenceRepository.findAllByMedia(media);
    }

    /**
     * @see ExternalReferenceService#findById(Integer)
     */
    @SuppressWarnings("unchecked")
    @Override
    public Optional<ExternalReferenceModel> findById(Integer id) {
        return this.externalReferenceRepository.findById(id);
    }

    /**
     * @see ExternalReferenceService#save(ExternalReferenceModel)
     */
    @SuppressWarnings("unchecked")
    @Override
    public ExternalReferenceModel save(ExternalReferenceModel externalReference) {
        return (ExternalReferenceModel) this.externalReferenceRepository.save(externalReference);
    }

    /**
     * @see ExternalReferenceService#saveAll(List<ExternalReferenceModel>)
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<ExternalReferenceModel> saveAll(List<ExternalReferenceModel> externalReferences) {
        return this.externalReferenceRepository.saveAll(externalReferences);
    }

    /**
     * @see ExternalReferenceService#findByIdAndType(Integer, TypeReferenceModel)
     */
    @SuppressWarnings("unchecked")
    @Override
    public Optional<ExternalReferenceModel> findByReferenceAndType(String reference, TypeReferenceModel typeReference) {
        return this.externalReferenceRepository.findByReferenceAndTypeReference(reference, typeReference);
    }
}
