package com.codesage.domain.repos.repository;

import com.codesage.domain.repos.model.RepositoryMetric;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepositoryMetricRepository extends JpaRepository<RepositoryMetric, UUID> {

    List<RepositoryMetric> findByRepositoryIdOrderByRecordedAtAsc(UUID repositoryId);

    Optional<RepositoryMetric> findTopByRepositoryIdOrderByRecordedAtDesc(UUID repositoryId);
}
