package com.espacogeek.geek.services.impl;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.espacogeek.geek.exception.InputValidationException;
import com.espacogeek.geek.exception.MediaAlreadyInLibraryException;
import com.espacogeek.geek.exception.NotFoundException;
import com.espacogeek.geek.models.CategoryType;
import com.espacogeek.geek.models.MediaModel;
import com.espacogeek.geek.models.StatusType;
import com.espacogeek.geek.models.UserCustomStatusModel;
import com.espacogeek.geek.models.UserMediaListModel;
import com.espacogeek.geek.models.UserModel;
import com.espacogeek.geek.repositories.MediaRepository;
import com.espacogeek.geek.repositories.UserCustomStatusRepository;
import com.espacogeek.geek.repositories.UserMediaListRepository;
import com.espacogeek.geek.repositories.UserRepository;
import com.espacogeek.geek.services.UserMediaListService;
import com.espacogeek.geek.types.UpdateUserMediaInput;

/**
 * An implementation class of UserMediaListService @see UserMediaListService
 */
@Service
public class UserMediaListServiceImpl implements UserMediaListService {

    private final UserMediaListRepository userMediaListRepository;
    private final UserRepository userRepository;
    private final MediaRepository mediaRepository;
    private final UserCustomStatusRepository userCustomStatusRepository;

    public UserMediaListServiceImpl(
            UserMediaListRepository userMediaListRepository,
            UserRepository userRepository,
            MediaRepository mediaRepository,
            UserCustomStatusRepository userCustomStatusRepository) {
        this.userMediaListRepository = userMediaListRepository;
        this.userRepository = userRepository;
        this.mediaRepository = mediaRepository;
        this.userCustomStatusRepository = userCustomStatusRepository;
    }

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
        entry.setStatus(StatusType.PLANNING.name());
        entry.setProgress(0);
        entry.setDatePlanned(new Date());

        return userMediaListRepository.save(entry);
    }

    /**
     * @see UserMediaListService#userMediaProgress(Integer, UpdateUserMediaInput)
     */
    @Override
    @Transactional
    public UserMediaListModel userMediaProgress(Integer userId, UpdateUserMediaInput input) {
        if (input.getMediaId() == null) {
            throw new InputValidationException("mediaId is required");
        }

        if (input.getStatus() != null) {
            validateStatus(input.getStatus());
        }

        UserCustomStatusModel customStatus = null;
        if (input.getCustomStatusId() != null) {
            customStatus = userCustomStatusRepository.findByIdAndUserId(input.getCustomStatusId(), userId)
                    .orElseThrow(() -> new NotFoundException("Custom status not found"));
        }

        Optional<UserMediaListModel> existing = userMediaListRepository
                .findByUserIdAndMediaId(userId, input.getMediaId());

        UserMediaListModel entry;
        if (existing.isPresent()) {
            entry = existing.get();
        } else {
            UserModel user = userRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException("User not found"));
            MediaModel media = mediaRepository.findById(input.getMediaId())
                    .orElseThrow(() -> new NotFoundException("Media not found"));

            entry = new UserMediaListModel();
            entry.setUser(user);
            entry.setMedia(media);
        }

        applyUpdates(entry, input, customStatus);

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

    private void validateStatus(String status) {
        try {
            StatusType.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            StringBuilder allowedValues = new StringBuilder();
            for (StatusType value : StatusType.values()) {
                if (allowedValues.length() > 0) {
                    allowedValues.append(", ");
                }
                allowedValues.append(value.name());
            }
            throw new InputValidationException("Invalid status: " + status
                    + ". Allowed values: " + allowedValues);
        }
    }

    private void applyUpdates(UserMediaListModel entry, UpdateUserMediaInput input, UserCustomStatusModel customStatus) {
        if (input.getStatus() != null) {
            String normalizedStatus = input.getStatus().toUpperCase();
            String previousStatus = entry.getStatus();
            entry.setStatus(normalizedStatus);
            if (StatusType.PLANNING.name().equals(normalizedStatus)
                    && (previousStatus == null || !StatusType.PLANNING.name().equals(previousStatus))
                    && entry.getDatePlanned() == null) {
                entry.setDatePlanned(new Date());
            }
        } else if (entry.getId() == null) {
            entry.setStatus(StatusType.PLANNING.name());
            entry.setDatePlanned(new Date());
        }

        if (input.getProgress() != null) {
            entry.setProgress(input.getProgress());
        } else if (entry.getId() == null) {
            // For new entries, default progress to 0 if not provided, to match addMedia() behavior.
            entry.setProgress(0);
        }
        if (input.getScore() != null) {
            entry.setScore(input.getScore().floatValue());
        }
        if (input.getStartDate() != null) {
            entry.setStartDate(input.getStartDate());
        }
        if (input.getFinishDate() != null) {
            entry.setFinishDate(input.getFinishDate());
        }
        if (input.getNote() != null) {
            entry.setNote(input.getNote());
        }
        if (customStatus != null) {
            entry.setCustomStatus(customStatus);
        }
    }
}
