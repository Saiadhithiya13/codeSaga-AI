package com.codesage.domain.debt.dto;

import com.codesage.domain.debt.model.DebtCategory;
import com.codesage.domain.debt.model.DebtSeverity;

import java.util.UUID;

public record TechnicalDebtFindingDto(
        UUID id,
        String filePath,
        DebtCategory category,
        DebtSeverity severity,
        String description,
        String recommendation
) {}
