package com.codesage.domain.debt.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.util.UUID;

@Entity
@Table(name = "technical_debt_findings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechnicalDebtFinding {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private TechnicalDebtReport report;

    @Column(name = "file_path", nullable = false, length = 1024)
    private String filePath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private DebtCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DebtSeverity severity;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String recommendation;
}
