package com.codesage.domain.repos.repository;

import com.codesage.domain.repos.model.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepositoryRepository extends JpaRepository<Repository, UUID> {

    List<Repository> findByUserId(UUID userId);

    Optional<Repository> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByUserIdAndGithubRepoId(UUID userId, Long githubRepoId);
}
