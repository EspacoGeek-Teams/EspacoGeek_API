package com.espacogeek.geek.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.espacogeek.geek.models.UserMediaListModel;

public interface UserMediaListRepository extends JpaRepository<UserMediaListModel, UUID> {

}
