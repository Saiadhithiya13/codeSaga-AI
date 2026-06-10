package com.codesage.domain.prreview.repository;

import com.codesage.domain.prreview.model.PullRequestReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PullRequestReviewRepository extends JpaRepository<PullRequestReview, UUID> {
    
    @Query("SELECT r FROM PullRequestReview r WHERE r.repository.id = :repositoryId ORDER BY r.createdAt DESC")
    List<PullRequestReview> findByRepositoryIdOrderByCreatedAtDesc(@Param("repositoryId") UUID repositoryId);

    @Query("SELECT r FROM PullRequestReview r WHERE r.repository.id = :repositoryId AND r.githubPrId = :githubPrId ORDER BY r.createdAt DESC LIMIT 1")
    Optional<PullRequestReview> findLatestByRepositoryIdAndGithubPrId(@Param("repositoryId") UUID repositoryId, @Param("githubPrId") String githubPrId);
}
