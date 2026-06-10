package com.codesage.domain.repos.repository;

import com.codesage.domain.repos.model.RepositoryFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;

public interface RepositoryFileRepository extends JpaRepository<RepositoryFile, UUID> {
    List<RepositoryFile> findByRepositoryId(UUID repositoryId);
    
    @Modifying
    @Query("DELETE FROM RepositoryFile rf WHERE rf.repository.id = :repositoryId")
    void deleteByRepositoryId(@Param("repositoryId") UUID repositoryId);
}
