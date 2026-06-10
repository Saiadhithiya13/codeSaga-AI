package com.codesage.domain.prreview.repository;

import com.codesage.domain.prreview.model.PullRequestFinding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PullRequestFindingRepository extends JpaRepository<PullRequestFinding, UUID> {
    
    @Query("SELECT f FROM PullRequestFinding f WHERE f.review.id = :reviewId ORDER BY f.severity DESC")
    List<PullRequestFinding> findByReviewIdOrderBySeverityDesc(@Param("reviewId") UUID reviewId);
}
