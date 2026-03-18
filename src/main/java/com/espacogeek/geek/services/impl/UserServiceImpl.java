package com.espacogeek.geek.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionSystemException;

import com.espacogeek.geek.exception.EmailAlreadyExistsException;
import com.espacogeek.geek.exception.InputValidationException;
import com.espacogeek.geek.models.UserModel;
import com.espacogeek.geek.repositories.UserRepository;
import com.espacogeek.geek.services.UserService;

import jakarta.validation.ConstraintViolationException;

import java.util.List;
import java.util.Optional;

/**
 * A Implementation class of UserService @see UserService
 */
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    UserRepository userRepository;

    /**
     * @see UserService#findByIdOrUsernameContainsOrEmail(String, String, String)
     */
    @Override
    public List<UserModel> findByIdOrUsernameContainsOrEmail(Integer id, String username, String email) {
        return userRepository.findByIdOrUsernameContainsOrEmail(id, username, email);
    }

    /**
     * @see UserService#findById(Integer)
     */
    @Override
    public Optional<UserModel> findById(Integer id) {
        return userRepository.findById(id);
    }

    /**
     * @see UserService#findUserByEmail(String)
     */
    @Override
    public Optional<UserModel> findUserByEmail(String email) {
        return userRepository.findUserByEmail(email);
    }

    /**
     * @see UserService#save(UserModel)
     */
    @Override
    public UserModel save(UserModel user) {
        try {
            return userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new EmailAlreadyExistsException();
        } catch (ConstraintViolationException | TransactionSystemException e) {
            throw new InputValidationException();
        }
    }

    /**
     * @see UserService#delete(Integer)
     */
    @Override
    public void deleteById(Integer id) {
        userRepository.deleteById(id);
    }
}
