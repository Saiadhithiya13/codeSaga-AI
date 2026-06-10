package com.codesage.domain.repos.repository;

import com.codesage.domain.repos.model.RepositoryContributor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RepositoryContributorRepository extends JpaRepository<RepositoryContributor, UUID> {

    List<RepositoryContributor> findByRepositoryIdOrderByContributionsDesc(UUID repositoryId);

    void deleteByRepositoryId(UUID repositoryId);
}
