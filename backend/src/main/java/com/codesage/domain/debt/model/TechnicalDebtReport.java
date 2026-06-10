package com.codesage.domain.debt.model;

import com.codesage.domain.repos.model.Repository;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "technical_debt_reports")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechnicalDebtReport {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false)
    private Repository repository;

    @Column(name = "overall_score", nullable = false)
    private Integer overallScore;

    @Column(name = "maintainability_score", nullable = false)
    private Integer maintainabilityScore;

    @Column(name = "complexity_score", nullable = false)
    private Integer complexityScore;

    @Column(name = "duplication_score", nullable = false)
    private Integer duplicationScore;

    @Column(name = "ai_assessment", columnDefinition = "TEXT")
    private String aiAssessment;

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TechnicalDebtFinding> findings = new ArrayList<>();

    @CreatedDate
    @Column(name = "generated_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMPTZ")
    private Instant generatedAt;
    
    public void addFinding(TechnicalDebtFinding finding) {
        findings.add(finding);
        finding.setReport(this);
    }
}
