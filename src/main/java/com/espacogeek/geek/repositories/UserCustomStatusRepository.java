package com.espacogeek.geek.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.espacogeek.geek.models.UserCustomStatusModel;

@Repository
public interface UserCustomStatusRepository extends JpaRepository<UserCustomStatusModel, Integer> {

    List<UserCustomStatusModel> findByUserId(Integer userId);

    Optional<UserCustomStatusModel> findByIdAndUserId(Integer id, Integer userId);

    boolean existsByIdAndUserId(Integer id, Integer userId);

    @Modifying
    @Query("DELETE FROM UserCustomStatusModel s WHERE s.id = :id AND s.user.id = :userId")
    int deleteByIdAndUserId(@Param("id") Integer id, @Param("userId") Integer userId);
}
