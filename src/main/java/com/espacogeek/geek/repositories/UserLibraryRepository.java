package com.espacogeek.geek.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.espacogeek.geek.models.UserLibraryModel;

@Repository
public interface UserLibraryRepository extends JpaRepository<UserLibraryModel, Integer> {

    List<UserLibraryModel> findByUserId(Integer userId);

    Optional<UserLibraryModel> findByUserIdAndMediaId(Integer userId, Integer mediaId);

    void deleteByUserIdAndMediaId(Integer userId, Integer mediaId);
}
