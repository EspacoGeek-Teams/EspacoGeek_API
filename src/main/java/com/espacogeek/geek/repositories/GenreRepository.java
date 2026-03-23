package com.espacogeek.geek.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.espacogeek.geek.models.GenreModel;

@Repository
public interface GenreRepository extends JpaRepository<GenreModel, Integer> {
    public List<GenreModel> findAllByNameIn(List<String> nameGenres);

    @Query("SELECT g FROM MediaModel m JOIN m.genre g WHERE m.id = :mediaId")
    List<GenreModel> findByMediaId(@Param("mediaId") Integer mediaId);
}
