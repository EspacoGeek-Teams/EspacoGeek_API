package com.espacogeek.geek.services.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.espacogeek.geek.models.UserMediaListModel;
import com.espacogeek.geek.repositories.UserMediaListRepository;
import com.espacogeek.geek.services.UserMediaListService;

/**
 * An implementation class of UserMediaListService @see UserMediaListService
 */
@Service
public class UserMediaListServiceImpl implements UserMediaListService {

    @Autowired
    private UserMediaListRepository userMediaListRepository;

    /**
     * @see UserMediaListService#findByUserIdWithFilters(Integer, String, Integer, String, String, String)
     */
    @Override
    public List<UserMediaListModel> findByUserIdWithFilters(
            Integer userId,
            String status,
            Integer categoryId,
            String genreName,
            String mediaName,
            String altTitle) {
        return userMediaListRepository.findByUserIdWithFilters(userId, status, categoryId, genreName, mediaName, altTitle);
    }
}
