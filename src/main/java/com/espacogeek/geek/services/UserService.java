package com.espacogeek.geek.services;

import com.espacogeek.geek.exception.GenericException;
import com.espacogeek.geek.models.UserModel;

import java.util.List;
import java.util.Optional;

/**
 * This interface defines the contract for the User Service.
 * It provides methods to perform CRUD operations on UserModel objects.
 */
public interface UserService {

    /**
     * Saves a new UserModel object to the database.
     *
     * @param user The UserModel object to be saved.
     * @return The saved UserModel object.
     * @throws GenericException If an error occurs during the save operation.
     */
    UserModel save(UserModel user) throws GenericException;

    /**
     * Retrieves a UserModel object by its id, username (partial match) and Email.
     *
     * @param id The id of the UserModel object to be retrieved.
     * @param username The username (partial) of the UserModel object to be retrieved.
     * @param email The email of the UserModel object to be retrieved.
     * @return An Optional containing the UserModel object if found, otherwise an empty Optional.
     */
    List<UserModel> findByIdOrUsernameContainsOrEmail(Integer id, String username, String email);

    /**
     * Retrieves a UserModel object by its ID.
     *
     * @param id The ID of the UserModel object to be retrieved.
     * @return An Optional containing the UserModel object if found, otherwise an empty Optional.
     */
    Optional<UserModel> findById(Integer id);

    /**
     * Retrieve a user by email.
     * @param email user email
     * @return Optional with user
     */
    Optional<UserModel> findUserByEmail(String email);

    /**
     * Delete user by Id provide.
     *
     * @param id
     */
    void deleteById(Integer id);
}
