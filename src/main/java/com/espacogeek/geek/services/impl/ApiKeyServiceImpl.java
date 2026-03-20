package com.espacogeek.geek.services.impl;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.espacogeek.geek.models.ApiKeyModel;
import com.espacogeek.geek.repositories.ApiKeyRepository;
import com.espacogeek.geek.services.ApiKeyService;

/**
 * A Implementation class of ApiKeyService @see ApiKeyService
 */
@Service
public class ApiKeyServiceImpl implements ApiKeyService {

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    /**
     * @see ApiKeyService#findById(Integer)
     */
    @Override
    public Optional<ApiKeyModel> findById(Integer id) {
        return apiKeyRepository.findById(id);
    }

    /**
     * @see ApiKeyService#save(ApiKeyModel)
     */
    @Override
    public Optional<ApiKeyModel> save(ApiKeyModel apiKeyModel) {
        return Optional.of(apiKeyRepository.save(apiKeyModel));
    }
}
