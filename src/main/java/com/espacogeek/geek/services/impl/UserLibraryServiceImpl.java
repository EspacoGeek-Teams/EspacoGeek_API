package com.espacogeek.geek.services.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.espacogeek.geek.exception.GenericException;
import com.espacogeek.geek.models.MediaModel;
import com.espacogeek.geek.models.TypeStatusModel;
import com.espacogeek.geek.models.UserLibraryModel;
import com.espacogeek.geek.models.UserModel;
import com.espacogeek.geek.repositories.MediaRepository;
import com.espacogeek.geek.repositories.TypeStatusRepository;
import com.espacogeek.geek.repositories.UserLibraryRepository;
import com.espacogeek.geek.repositories.UserRepository;
import com.espacogeek.geek.services.UserLibraryService;

import lombok.RequiredArgsConstructor;

/**
 * Implementation of {@link UserLibraryService}.
 */
@Service
@RequiredArgsConstructor
public class UserLibraryServiceImpl implements UserLibraryService {

    /** ID of the default "Planning" status in the {@code types_status} table. */
    private static final int DEFAULT_STATUS_ID = 1;

    private final UserLibraryRepository userLibraryRepository;
    private final UserRepository userRepository;
    private final MediaRepository mediaRepository;
    private final TypeStatusRepository typeStatusRepository;

    @Override
    public List<UserLibraryModel> findByUserId(Integer userId) {
        return userLibraryRepository.findByUserId(userId);
    }

    @Override
    @Transactional
    public UserLibraryModel addMedia(Integer userId, Integer mediaId) {
        UserModel user = userRepository.findById(userId)
                .orElseThrow(() -> new GenericException(HttpStatus.NOT_FOUND.toString()));

        MediaModel media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new GenericException(HttpStatus.NOT_FOUND.toString()));

        TypeStatusModel defaultStatus = typeStatusRepository.findById(DEFAULT_STATUS_ID)
                .orElseThrow(() -> new GenericException(HttpStatus.INTERNAL_SERVER_ERROR.toString()));

        UserLibraryModel entry = new UserLibraryModel();
        entry.setUser(user);
        entry.setMedia(media);
        entry.setTypeStatus(defaultStatus);
        entry.setProgress(0);

        return userLibraryRepository.save(entry);
    }

    @Override
    @Transactional
    public boolean removeMedia(Integer userId, Integer mediaId) {
        Optional<UserLibraryModel> entry = userLibraryRepository.findByUserIdAndMediaId(userId, mediaId);
        if (entry.isEmpty()) {
            return false;
        }
        userLibraryRepository.deleteByUserIdAndMediaId(userId, mediaId);
        return true;
    }
}
