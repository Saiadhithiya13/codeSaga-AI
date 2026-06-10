package com.codesage.domain.prreview.service;

import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PullRequestDiffService {

    @Data
    @Builder
    public static class DiffFile {
        private String filePath;
        private int additions;
        private int deletions;
        private String diffContent;
    }

    public List<DiffFile> parseDiff(String rawDiff) {
        List<DiffFile> files = new ArrayList<>();
        if (rawDiff == null || rawDiff.isBlank()) {
            return files;
        }

        String[] lines = rawDiff.split("\n");
        DiffFile.DiffFileBuilder currentFileBuilder = null;
        StringBuilder contentBuilder = new StringBuilder();

        for (String line : lines) {
            if (line.startsWith("diff --git ")) {
                if (currentFileBuilder != null) {
                    currentFileBuilder.diffContent(contentBuilder.toString());
                    files.add(currentFileBuilder.build());
                }
                currentFileBuilder = DiffFile.builder().additions(0).deletions(0);
                contentBuilder = new StringBuilder();
                continue;
            }

            if (currentFileBuilder != null) {
                if (line.startsWith("+++ b/")) {
                    currentFileBuilder.filePath(line.substring(6));
                } else if (line.startsWith("+") && !line.startsWith("+++")) {
                    currentFileBuilder.additions(currentFileBuilder.build().getAdditions() + 1);
                } else if (line.startsWith("-") && !line.startsWith("---")) {
                    currentFileBuilder.deletions(currentFileBuilder.build().getDeletions() + 1);
                }
                contentBuilder.append(line).append("\n");
            }
        }

        if (currentFileBuilder != null) {
            currentFileBuilder.diffContent(contentBuilder.toString());
            files.add(currentFileBuilder.build());
        }

        return files;
    }
}
