package com.trendfeed.backend.service;

import com.trendfeed.backend.entity.GitHubEntity;
import com.trendfeed.backend.entity.TrendingCandidateEntity;
import com.trendfeed.backend.repository.GitHubRepository;
import com.trendfeed.backend.repository.TrendingCandidateRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * GitHub에서 리포지토리 정보를 수집하고,
 * 트렌드 점수/단계(0→1→2)를 관리하고,
 * stage==2 리포들을 TrendingCandidateEntity 로 승격시키고,
 * AI 요청 시 후보를 꺼내주는 모든 로직을 담당.
 *
 * 흐름:
 *  - crawlAllAndEvaluate()  [스케줄러: 3일마다 전체 스캔]
 *  - upsertAndEvaluate()    [단일 리포 강제 수집용(수동 호출)]
 *  - getOldestUngivenCandidatesAndMark() [AI에게 건네줄 후보 리턴]
 *
 * 트렌드 로직 핵심:
 *   growthRate = (currStars - prevStars) / prevStars   (prevStars<=0이면 0)
 *   agePenaltyFactor = 0.5^( ageDays / halfLifeDays )
 *   trendScore = growthRate * growthWeight * agePenaltyFactor * penaltyWeight
 *
 *   stage 0/1에 대해 같은 임계값으로 검사:
 *     - score >= threshold → stage+1 (최대 2)
 *     - score < threshold AND stage==1 → stage=0 (강등)
 *
 *   stage==2가 막 된 순간 -> TrendingCandidate 테이블에 insert
 */
@Service
public class GitHubService {

    private final WebClient github;
    private final GitHubRepository repoRepo;
    private final TrendingCandidateRepository candRepo;

    // ====== 수집(검색) 파라미터 ======
    @Value("${crawler.search.years:2}")          // 최근 N년 이내의 repo만 검색
    private int searchYears;

    @Value("${crawler.search.min-stars:1000}")   // 최소 스타 수
    private int minStars;

    @Value("${crawler.per-page:100}")            // GitHub /search/repositories per_page (최대 100)
    private int perPage;

    @Value("${crawler.max-pages:10}")            // 페이지네이션 상한
    private int maxPages;

    @Value("${crawler.sleep-millis:150}")        // 각 rep션 처리 사이 딜레이, 레이트리밋 보호
    private long sleepMillis;

    // ====== 스케줄링 크론 표현식 ======
    @Value("${crawler.cron:0 0 0 */3 * *}")
    private String cron;

    // ====== 트렌드 계산 파라미터 ======
    @Value("${trend.weight.growth:1.0}")         // 증가율 가중치
    private double growthWeight;

    @Value("${trend.weight.penalty:1.0}")        // 나이 페널티 가중치
    private double penaltyWeight;

    @Value("${trend.age.half-life-days:720}")    // 감쇠 계수
    private double ageHalfLifeDays;

    @Value("${trend.threshold:0.10}")            // 임계치
    private double trendThreshold;

    public GitHubService(
            WebClient githubWebClient,
            GitHubRepository repoRepo,
            TrendingCandidateRepository candRepo
    ) {
        this.github = githubWebClient;
        this.repoRepo = repoRepo;
        this.candRepo = candRepo;
    }

    // ──────────────────────────────────────────────────────────────
    //    주기적으로 전체 수집 (조건: 최근 N년 + 최소 스타수)
    // ──────────────────────────────────────────────────────────────
    @Scheduled(cron = "${crawler.cron:0 0 0 */3 * *}")
    @Transactional
    public void crawlAllAndEvaluate() {
        String since = OffsetDateTime.now(ZoneOffset.UTC)
                .minusYears(searchYears)
                .format(DateTimeFormatter.ISO_LOCAL_DATE);

        // GitHub search q 파라미터
        // created:>=YYYY-MM-DD → "created:%3E%3DYYYY-MM-DD" (%3E%3D == ">=" 인코딩)
        String q = "stars:>=" + minStars + "+created:%3E%3D" + since;

        for (int page = 1; page <= maxPages; page++) {

            final int currentPage = page; 

            Map<String, Object> searchResult = github.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search/repositories")
                            .queryParam("q", q)
                            .queryParam("sort", "stars")
                            .queryParam("order", "desc")
                            .queryParam("per_page", perPage)
                            .queryParam("page", currentPage)
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .onErrorResume(ex -> {
                        return Mono.empty();
                    })
                    .block();

            if (searchResult == null) {
                break; 
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) searchResult.get("items");
            if (items == null || items.isEmpty()) {
                break; 
            }

            for (Map<String, Object> item : items) {
                String fullName = (String) item.get("full_name"); // "owner/repo"
                if (fullName == null) continue;

                try {
                    upsertAndEvaluate(fullName);
                    Thread.sleep(sleepMillis);
                } catch (Exception ignore) {
                    // TODO: 로깅
                }
            }
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  단일 리포 수집 및 평가
    // ──────────────────────────────────────────────────────────────
    @Transactional
    public GitHubEntity upsertAndEvaluate(String fullName) {
        String[] parts = splitFullName(fullName);
        String owner = parts[0];
        String repoName = parts[1];

        // 메타데이터 수집
        Map<String, Object> meta = github.get()
                .uri("/repos/{owner}/{repo}", owner, repoName)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        if (meta == null) {
            return null;
        }

        GitHubEntity existing = repoRepo.findById(((Number) meta.get("id")).longValue()).orElse(null);
        GitHubEntity e = mapMetaToEntity(meta, existing);

        // README 수집
        fetchAndAttachReadme(owner, repoName, e);

        // 트렌드 점수 계산/승급 
        evaluateTrendAndMaybePromote(e);

        // 크롤링 시간 기록
        e.setLastCrawledAt(OffsetDateTime.now(ZoneOffset.UTC));
        return repoRepo.save(e);
    }

    // ──────────────────────────────────────────────────────────────
    //  AI요청에 후보 반환
    // ──────────────────────────────────────────────────────────────
    @Transactional
    public List<GitHubEntity> getOldestUngivenCandidatesAndMark(int limit) {
        var pageReq = PageRequest.of(
                0,
                limit,
                Sort.by(Sort.Direction.ASC, "promotedAt")
        );

        List<TrendingCandidateEntity> picks =
                candRepo.findByGivenToAIFalseOrderByPromotedAtAsc(pageReq);
        if (picks.isEmpty()) {
            return List.of();
        }

        // 준 걸로 표시
        for (TrendingCandidateEntity c : picks) {
            c.setGivenToAI(true);
        }
        candRepo.saveAll(picks);

        // 실제 리포 엔티티들 로드해서 반환
        List<Long> repoIds = picks.stream()
                .map(TrendingCandidateEntity::getRepoId)
                .toList();

        return repoRepo.findAllById(repoIds);
    }

    // ──────────────────────────────────────────────────────────────
    // 내부 유틸들
    // ──────────────────────────────────────────────────────────────

    /*
    * README를 GitHub API로 가져오기
    */
    private void fetchAndAttachReadme(String owner, String repoName, GitHubEntity e) {

        Map<String, Object> readmeResponse = github.get()
                .uri("/repos/{owner}/{repo}/readme", owner, repoName)
                .headers(h -> {
                    if (e.getReadmeEtag() != null) {
                        h.add("If-None-Match", e.getReadmeEtag());
                    }
                })
                .exchangeToMono(resp -> {
                    int code = resp.statusCode().value();

                    // README 변동 x 
                    if (code == 304) {
                        return Mono.empty();
                    }

                    // README 없음
                    if (code == 404) {
                        return Mono.empty();
                    }

                    // 정상 응답(2xx)
                    if (code >= 200 && code < 300) {
                        return resp.bodyToMono(Map.class);
                    }

                    // 그 외 
                    return Mono.empty();
                })
                .block();

        if (readmeResponse == null) {
            // 304나 404 등 무시 
            return;
        }

        String encoded = (String) readmeResponse.get("content");
        String encoding = (String) readmeResponse.get("encoding"); // 주로 "base64"
        String sha = (String) readmeResponse.get("sha");

        String text = null;
        if (encoded != null && "base64".equalsIgnoreCase(encoding)) {
            byte[] bytes = java.util.Base64
                    .getDecoder()
                    .decode(encoded.getBytes(StandardCharsets.UTF_8));
            text = new String(bytes, StandardCharsets.UTF_8);
        }

        e.setReadmeText(text);
        e.setReadmeSha(sha);

        // e.setReadmeEtag(newEtag);
    }

    /*
     * GitHub meta JSON을 GitHubEntity에 저장. score 관련 초기화 
     */
    @SuppressWarnings("unchecked")
    private GitHubEntity mapMetaToEntity(Map<String, Object> meta, GitHubEntity existing) {
        GitHubEntity e = (existing != null) ? existing : new GitHubEntity();

        Integer prevStarsBefore = e.getStargazersCount() != null
                ? e.getStargazersCount()
                : null;

        // 기본 메타 필드 채우기
        e.setId(((Number) meta.get("id")).longValue());
        e.setNodeId((String) meta.get("node_id"));
        e.setName((String) meta.get("name"));
        e.setFullName((String) meta.get("full_name"));

        Map<String, Object> owner = (Map<String, Object>) meta.get("owner");
        e.setOwnerLogin(owner != null ? (String) owner.get("login") : null);

        e.setHtmlUrl((String) meta.get("html_url"));
        e.setDescription((String) meta.get("description"));
        e.setLanguage((String) meta.get("language"));

        Number stars = (Number) meta.get("stargazers_count");
        e.setStargazersCount(stars == null ? null : stars.intValue());

        e.setCreatedAt(parseTime((String) meta.get("created_at")));
        e.setPushedAt(parseTime((String) meta.get("pushed_at")));
        e.setUpdatedAt(parseTime((String) meta.get("updated_at")));

        // 최초 수집 시 
        if (prevStarsBefore == null && e.getStargazersCount() != null) {
            e.setPreviousStars(e.getStargazersCount());
            e.setGrowthRate(0.0);
            e.setTrendScore(0.0);
            if (e.getTrendStage() == null) {
                e.setTrendStage(0);
            }
        } else if (prevStarsBefore != null) {
            // 기존 previousStars 유지
            e.setPreviousStars(prevStarsBefore);
        }

        return e;
    }

    /*
     * score 계산 / stage update / candidate
     */
    private void evaluateTrendAndMaybePromote(GitHubEntity e) {
        int curr = orZero(e.getStargazersCount());
        int prev = orZero(e.getPreviousStars());

        // 성장률
        double growthRate = (prev <= 0)
                ? 0.0
                : (double) (curr - prev) / (double) prev;

        // 나이 감쇠
        double agePenaltyFactor = 1.0;
        if (e.getCreatedAt() != null && ageHalfLifeDays > 0) {
            long ageDays = Math.max(
                    0,
                    ChronoUnit.DAYS.between(e.getCreatedAt(), OffsetDateTime.now(ZoneOffset.UTC))
            );
            // 예) half-life마다 절반
            agePenaltyFactor = Math.pow(0.5, ageDays / ageHalfLifeDays);
        }
        //스코어 계산
        double score = growthRate
                * growthWeight
                * agePenaltyFactor
                * penaltyWeight;

        int oldStage = (e.getTrendStage() == null) ? 0 : e.getTrendStage();
        int newStage = oldStage;

        // stage update
        if (oldStage == 0 || oldStage == 1) {
            if (score >= trendThreshold) {
                newStage = Math.min(2, oldStage + 1); // 승급
            } else if (oldStage == 1) {
                newStage = 0; // 강등
            }
        }

        // promote 여부 체크 
        boolean promotedTo2Now = (oldStage < 2 && newStage == 2);

        e.setGrowthRate(growthRate);
        e.setTrendScore(score);
        e.setTrendStage(newStage);
        e.setLastCheckedAt(OffsetDateTime.now(ZoneOffset.UTC));

        // prev갱신
        e.setPreviousStars(curr);

        // 후보 테이블 삽입
        if (promotedTo2Now) {
            boolean already = candRepo.existsByRepoId(e.getId());
            if (!already) {
                TrendingCandidateEntity c = new TrendingCandidateEntity(
                        e.getId(),
                        e.getFullName()
                );
                candRepo.save(c);
            }
        }
    }

    private OffsetDateTime parseTime(String iso) {
        if (iso == null) return null;
        return OffsetDateTime.parse(iso);
    }

    private static String[] splitFullName(String fullName) {
        if (fullName == null || !fullName.contains("/")) {
            throw new IllegalArgumentException("fullName must be like 'owner/repo'");
        }
        String[] parts = fullName.split("/", 2);
        return new String[]{ parts[0].trim(), parts[1].trim() };
    }

    private int orZero(Integer v) {
        return (v == null) ? 0 : v;
    }
}
