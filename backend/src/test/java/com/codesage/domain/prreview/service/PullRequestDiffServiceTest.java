package com.codesage.domain.prreview.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PullRequestDiffServiceTest {

    private final PullRequestDiffService service = new PullRequestDiffService();

    @Test
    void parseDiff_withValidDiff_extractsFilesAndChanges() {
        String diff = """
                diff --git a/src/Auth.java b/src/Auth.java
                index 83db48f..112a1f9 100644
                --- a/src/Auth.java
                +++ b/src/Auth.java
                @@ -1,3 +1,5 @@
                 public class Auth {
                +    private String token;
                +    public void login() {}
                -    // old method
                 }
                """;

        List<PullRequestDiffService.DiffFile> files = service.parseDiff(diff);

        assertEquals(1, files.size());
        assertEquals("src/Auth.java", files.get(0).getFilePath());
        assertEquals(2, files.get(0).getAdditions());
        assertEquals(1, files.get(0).getDeletions());
    }

    @Test
    void parseDiff_multipleFiles_extractsProperly() {
        String diff = """
                diff --git a/A.java b/A.java
                --- a/A.java
                +++ b/A.java
                + line 1
                diff --git a/B.java b/B.java
                --- a/B.java
                +++ b/B.java
                - line 1
                """;

        List<PullRequestDiffService.DiffFile> files = service.parseDiff(diff);

        assertEquals(2, files.size());
        assertEquals("A.java", files.get(0).getFilePath());
        assertEquals(1, files.get(0).getAdditions());
        assertEquals(0, files.get(0).getDeletions());

        assertEquals("B.java", files.get(1).getFilePath());
        assertEquals(0, files.get(1).getAdditions());
        assertEquals(1, files.get(1).getDeletions());
    }
}
