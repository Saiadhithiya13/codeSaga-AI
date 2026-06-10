package com.codesage.domain.debt.service;

import com.codesage.config.TechnicalDebtConfigProperties;
import com.codesage.domain.debt.model.DebtCategory;
import com.codesage.domain.debt.model.TechnicalDebtFinding;
import com.codesage.domain.debt.model.TechnicalDebtReport;
import com.codesage.domain.repos.model.CodeChunk;
import com.codesage.domain.repos.model.RepositoryFile;
import com.codesage.domain.repos.repository.CodeChunkRepository;
import com.codesage.domain.repos.repository.RepositoryFileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class StaticCodeAnalysisServiceTest {

    private RepositoryFileRepository fileRepository;
    private CodeChunkRepository chunkRepository;
    private TechnicalDebtConfigProperties config;
    private StaticCodeAnalysisService service;

    @BeforeEach
    void setUp() {
        fileRepository = Mockito.mock(RepositoryFileRepository.class);
        chunkRepository = Mockito.mock(CodeChunkRepository.class);
        config = new TechnicalDebtConfigProperties();
        config.setLargeFileThreshold(100);
        config.setDeepNestingThreshold(4);
        service = new StaticCodeAnalysisService(fileRepository, chunkRepository, config);
    }

    @Test
    void testLargeFileDetection() {
        UUID repoId = UUID.randomUUID();
        RepositoryFile largeFile = new RepositoryFile();
        largeFile.setId(UUID.randomUUID());
        largeFile.setPath("LargeFile.java");
        largeFile.setSizeBytes(101 * 30L); // Over 100 lines (30 bytes per line avg)

        when(fileRepository.findByRepositoryId(repoId)).thenReturn(List.of(largeFile));
        when(chunkRepository.findByRepositoryId(repoId)).thenReturn(List.of());

        TechnicalDebtReport report = new TechnicalDebtReport();
        List<TechnicalDebtFinding> findings = service.analyze(repoId, report);

        assertEquals(1, findings.size());
        assertEquals(DebtCategory.LARGE_FILE, findings.get(0).getCategory());
        assertEquals("LargeFile.java", findings.get(0).getFilePath());
    }

    @Test
    void testDeepNestingDetection() {
        UUID repoId = UUID.randomUUID();
        RepositoryFile file = new RepositoryFile();
        file.setId(UUID.randomUUID());
        file.setPath("NestedFile.java");
        file.setSizeBytes(100L);

        CodeChunk chunk = new CodeChunk();
        chunk.setRepositoryFile(file);
        // 4 levels of indentation (4 spaces each = 16 spaces)
        chunk.setContent("public void test() {\n    if(a) {\n        if(b) {\n            if(c) {\n                doIt();\n            }\n        }\n    }\n}");

        when(fileRepository.findByRepositoryId(repoId)).thenReturn(List.of(file));
        when(chunkRepository.findByRepositoryId(repoId)).thenReturn(List.of(chunk));

        TechnicalDebtReport report = new TechnicalDebtReport();
        List<TechnicalDebtFinding> findings = service.analyze(repoId, report);

        assertEquals(1, findings.size());
        assertEquals(DebtCategory.DEEP_NESTING, findings.get(0).getCategory());
    }
}
