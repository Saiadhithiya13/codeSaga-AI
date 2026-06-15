package com.codesage;

import com.codesage.domain.repos.service.ingestion.RepositoryIndexingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

@SpringBootTest
public class IndexingTriggerTest {

    @Autowired
    private RepositoryIndexingService indexingService;

    @Test
    public void testTrigger() throws Exception {
        UUID repoId = UUID.fromString("be230c8b-ddd1-4240-a7a5-8f078df801be");
        UUID userId = UUID.fromString("57db9884-ffe3-4515-8edb-9d2a07ed1598");
        System.out.println("=========================================");
        System.out.println("TRIGGERING INDEXING FOR: " + repoId);
        indexingService.startIndexingAsync(repoId, userId);
        System.out.println("TRIGGERED. WAITING 30 SECONDS FOR IT TO COMPLETE...");
        Thread.sleep(30000);
        System.out.println("DONE WAITING.");
        System.out.println("=========================================");
    }
}
