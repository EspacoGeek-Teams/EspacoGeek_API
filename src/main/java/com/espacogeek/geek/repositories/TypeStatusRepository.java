package com.espacogeek.geek.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.espacogeek.geek.models.TypeStatusModel;

@Repository
public interface TypeStatusRepository extends JpaRepository<TypeStatusModel, Integer> {
}
