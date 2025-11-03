package com.trendfeed.backend.controller;

import com.trendfeed.backend.entity.GitHubEntity;
import com.trendfeed.backend.service.GitHubService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/*
 * - GET  /api/github/ingest?fullName=owner/repo  : 단일 수집 (테스트용)
 * - POST /api/github/crawl                       : 스케줄러 즉시 실행
 * - GET  /api/ai/candidates?limit=3              : 후보 반환
 */
@RestController
@RequestMapping("/api")
public class GitHubController {

    private final GitHubService service;

    public GitHubController(GitHubService service) {
        this.service = service;
    }

    // 단일(테스트용)
    @GetMapping("/github/ingest")
    public ResponseEntity<String> ingest(@RequestParam String fullName) {
        GitHubEntity saved = service.upsertAndEvaluate(fullName);
        if (saved == null) return ResponseEntity.internalServerError().body("failed: " + fullName);
        return ResponseEntity.ok("ingested: %s (stage=%d, score=%.4f, growth=%.4f)"
                .formatted(saved.getFullName(),
                        (saved.getTrendStage() == null ? 0 : saved.getTrendStage()),
                        (saved.getTrendScore() == null ? 0.0 : saved.getTrendScore()),
                        (saved.getGrowthRate() == null ? 0.0 : saved.getGrowthRate())));
    }

    // 전체 즉시 수집
    @PostMapping("/github/crawl")
    public ResponseEntity<String> crawlNow() {
        service.crawlAllAndEvaluate();
        return ResponseEntity.ok("crawl started and finished (see logs)");
    }

    // 후보 반환
    @GetMapping("/ai/candidates")
    public ResponseEntity<List<GitHubEntity>> getCandidates(@RequestParam(defaultValue = "3") int limit) {
        if (limit <= 0) limit = 1;
        List<GitHubEntity> repos = service.getOldestUngivenCandidatesAndMark(limit);
        return ResponseEntity.ok(repos);
    }
}
