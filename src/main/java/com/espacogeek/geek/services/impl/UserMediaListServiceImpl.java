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
     * @see UserMediaListService#findByUserIdWithFilters(Integer, String, Integer, Integer, String, Integer, String, Integer, String, String)
     */
    @Override
    public List<UserMediaListModel> findByUserIdWithFilters(
            Integer userId,
            String status,
            Integer statusId,
            Integer categoryId,
            String categoryName,
            Integer genreId,
            String genreName,
            Integer mediaId,
            String mediaName,
            String altTitle) {
        return userMediaListRepository.findByUserIdWithFilters(
                userId, status, statusId, categoryId, categoryName, genreId, genreName, mediaId, mediaName, altTitle);
    }
}
