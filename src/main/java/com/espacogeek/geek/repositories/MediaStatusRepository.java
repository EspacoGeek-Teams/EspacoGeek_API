package com.espacogeek.geek.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.espacogeek.geek.models.MediaStatusModel;

@Repository
public interface MediaStatusRepository extends JpaRepository<MediaStatusModel, Long> {

}
