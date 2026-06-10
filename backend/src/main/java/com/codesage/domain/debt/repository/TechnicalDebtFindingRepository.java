package com.codesage.domain.debt.repository;

import com.codesage.domain.debt.model.TechnicalDebtFinding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TechnicalDebtFindingRepository extends JpaRepository<TechnicalDebtFinding, UUID> {
    
    @Query("SELECT f FROM TechnicalDebtFinding f WHERE f.report.id = :reportId ORDER BY f.severity DESC")
    List<TechnicalDebtFinding> findByReportIdOrderBySeverityDesc(@Param("reportId") UUID reportId);
}
