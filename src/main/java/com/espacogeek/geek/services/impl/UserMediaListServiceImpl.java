package com.espacogeek.geek.services.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.espacogeek.geek.exception.MediaAlreadyInLibraryException;
import com.espacogeek.geek.exception.NotFoundException;
import com.espacogeek.geek.models.CategoryType;
import com.espacogeek.geek.models.MediaModel;
import com.espacogeek.geek.models.StatusType;
import com.espacogeek.geek.models.UserMediaListModel;
import com.espacogeek.geek.models.UserModel;
import com.espacogeek.geek.repositories.MediaRepository;
import com.espacogeek.geek.repositories.UserMediaListRepository;
import com.espacogeek.geek.repositories.UserRepository;
import com.espacogeek.geek.services.UserMediaListService;

/**
 * An implementation class of UserMediaListService @see UserMediaListService
 */
@Service
public class UserMediaListServiceImpl implements UserMediaListService {

    @Autowired
    private UserMediaListRepository userMediaListRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MediaRepository mediaRepository;

    /**
     * @see UserMediaListService#findByUserIdWithFilters(Integer, String, Integer, Integer, CategoryType, Integer, String, Integer, String, String)
     */
    @Override
    public List<UserMediaListModel> findByUserIdWithFilters(
            Integer userId,
            String status,
            Integer statusId,
            Integer categoryId,
            CategoryType categoryName,
            Integer genreId,
            String genreName,
            Integer mediaId,
            String mediaName,
            String altTitle) {
        return userMediaListRepository.findByUserIdWithFilters(
                userId, status, statusId, categoryId, categoryName, genreId, genreName, mediaId, mediaName, altTitle);
    }

    /**
     * @see UserMediaListService#addMedia(Integer, Integer)
     */
    @Override
    @Transactional
    public UserMediaListModel addMedia(Integer userId, Integer mediaId) {
        if (userMediaListRepository.existsByUserIdAndMediaId(userId, mediaId)) {
            throw new MediaAlreadyInLibraryException();
        }

        UserModel user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        MediaModel media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new NotFoundException("Media not found"));

        UserMediaListModel entry = new UserMediaListModel();
        entry.setUser(user);
        entry.setMedia(media);
        entry.setStatus(StatusType.PLAN_TO_WATCH.name());
        entry.setProgress(0);

        return userMediaListRepository.save(entry);
    }

    /**
     * @see UserMediaListService#removeMedia(Integer, Integer)
     */
    @Override
    @Transactional
    public boolean removeMedia(Integer userId, Integer mediaId) {
        long deleted = userMediaListRepository.deleteByUserIdAndMediaId(userId, mediaId);
        return deleted > 0;
    }
}
