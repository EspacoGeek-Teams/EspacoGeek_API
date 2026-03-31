package com.espacogeek.geek.services.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        UserModel user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        UserCustomStatusModel status = new UserCustomStatusModel();
        status.setUser(user);
        status.setName(name.trim());
        return userCustomStatusRepository.save(status);
    }

    /**
     * @see UserCustomStatusService#update(Integer, Integer, String)
     */
    @Override
    @Transactional
    public UserCustomStatusModel update(Integer userId, Integer statusId, String name) {
        UserCustomStatusModel status = userCustomStatusRepository.findByIdAndUserId(statusId, userId)
                .orElseThrow(() -> new NotFoundException("Status not found"));

        status.setName(name.trim());
        return userCustomStatusRepository.save(status);
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
