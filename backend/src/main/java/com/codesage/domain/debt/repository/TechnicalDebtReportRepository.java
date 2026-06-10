package com.codesage.domain.debt.repository;

import com.codesage.domain.debt.model.TechnicalDebtReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TechnicalDebtReportRepository extends JpaRepository<TechnicalDebtReport, UUID> {
    
    @Query("SELECT r FROM TechnicalDebtReport r WHERE r.repository.id = :repositoryId ORDER BY r.generatedAt DESC LIMIT 1")
    Optional<TechnicalDebtReport> findLatestByRepositoryId(@Param("repositoryId") UUID repositoryId);
}
