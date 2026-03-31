package com.espacogeek.geek.services.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.espacogeek.geek.exception.InputValidationException;
import com.espacogeek.geek.exception.NotFoundException;
import com.espacogeek.geek.models.UserCustomStatusModel;
import com.espacogeek.geek.models.UserModel;
import com.espacogeek.geek.repositories.UserCustomStatusRepository;
import com.espacogeek.geek.repositories.UserRepository;
import com.espacogeek.geek.services.UserCustomStatusService;

/**
 * An implementation class of UserCustomStatusService @see UserCustomStatusService
 */
@Service
public class UserCustomStatusServiceImpl implements UserCustomStatusService {

    private final UserCustomStatusRepository userCustomStatusRepository;
    private final UserRepository userRepository;

    public UserCustomStatusServiceImpl(
            UserCustomStatusRepository userCustomStatusRepository,
            UserRepository userRepository) {
        this.userCustomStatusRepository = userCustomStatusRepository;
        this.userRepository = userRepository;
    }

    /**
     * @see UserCustomStatusService#findByUserId(Integer)
     */
    @Override
    public List<UserCustomStatusModel> findByUserId(Integer userId) {
        return userCustomStatusRepository.findByUserId(userId);
    }

    /**
     * @see UserCustomStatusService#create(Integer, String)
     */
    @Override
    @Transactional
    public UserCustomStatusModel create(Integer userId, String name) {
        String trimmedName = validateStatusName(name);

        UserModel user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        UserCustomStatusModel status = new UserCustomStatusModel();
        status.setUser(user);
        status.setName(trimmedName);
        return userCustomStatusRepository.save(status);
    }

    /**
     * @see UserCustomStatusService#update(Integer, Integer, String)
     */
    @Override
    @Transactional
    public UserCustomStatusModel update(Integer userId, Integer statusId, String name) {
        String trimmedName = validateStatusName(name);

        UserCustomStatusModel status = userCustomStatusRepository.findByIdAndUserId(statusId, userId)
                .orElseThrow(() -> new NotFoundException("Status not found"));

        status.setName(trimmedName);
        return userCustomStatusRepository.save(status);
    }

    private String validateStatusName(String name) {
        String trimmedName = name == null ? "" : name.trim();
        if (trimmedName.isEmpty()) {
            throw new InputValidationException("Status name must not be blank");
        }
        if (trimmedName.length() > 100) {
            throw new InputValidationException("Status name must not exceed 100 characters");
        }
        return trimmedName;
    }

    /**
     * @see UserCustomStatusService#delete(Integer, Integer)
     */
    @Override
    @Transactional
    public boolean delete(Integer userId, Integer statusId) {
        int deletedCount = userCustomStatusRepository.deleteByIdAndUserId(statusId, userId);
        return deletedCount > 0;
    }
}
