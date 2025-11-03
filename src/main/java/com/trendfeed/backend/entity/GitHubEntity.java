package com.trendfeed.backend.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "git_repositories")
public class GitHubEntity {

    @Id
    private Long id;

    private String nodeId;
    private String name;
    private String fullName;
    private String ownerLogin;
    private String htmlUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String language;
    private Integer stargazersCount;

    private OffsetDateTime createdAt;
    private OffsetDateTime pushedAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime lastCrawledAt;

    // ===== README =====
    @Column(columnDefinition = "TEXT")
    private String readmeText;
    private String readmeSha;
    private String readmeEtag;

    // ===== 트렌드 분석용 =====
    private Integer previousStars;
    private Double growthRate;      // 증가율 (%)
    private Double trendScore;      // 최종 점수
    private Integer trendStage;     // 0: 기본 / 1: 1차관심 / 2: 후보로 승격
    private OffsetDateTime lastCheckedAt;

    public GitHubEntity() {}

    // Getter/Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getOwnerLogin() { return ownerLogin; }
    public void setOwnerLogin(String ownerLogin) { this.ownerLogin = ownerLogin; }

    public String getHtmlUrl() { return htmlUrl; }
    public void setHtmlUrl(String htmlUrl) { this.htmlUrl = htmlUrl; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public Integer getStargazersCount() { return stargazersCount; }
    public void setStargazersCount(Integer stargazersCount) { this.stargazersCount = stargazersCount; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getPushedAt() { return pushedAt; }
    public void setPushedAt(OffsetDateTime pushedAt) { this.pushedAt = pushedAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public OffsetDateTime getLastCrawledAt() { return lastCrawledAt; }
    public void setLastCrawledAt(OffsetDateTime lastCrawledAt) { this.lastCrawledAt = lastCrawledAt; }

    public String getReadmeText() { return readmeText; }
    public void setReadmeText(String readmeText) { this.readmeText = readmeText; }

    public String getReadmeSha() { return readmeSha; }
    public void setReadmeSha(String readmeSha) { this.readmeSha = readmeSha; }

    public String getReadmeEtag() { return readmeEtag; }
    public void setReadmeEtag(String readmeEtag) { this.readmeEtag = readmeEtag; }

    public Integer getPreviousStars() { return previousStars; }
    public void setPreviousStars(Integer previousStars) { this.previousStars = previousStars; }

    public Double getGrowthRate() { return growthRate; }
    public void setGrowthRate(Double growthRate) { this.growthRate = growthRate; }

    public Double getTrendScore() { return trendScore; }
    public void setTrendScore(Double trendScore) { this.trendScore = trendScore; }

    public Integer getTrendStage() { return trendStage; }
    public void setTrendStage(Integer trendStage) { this.trendStage = trendStage; }

    public OffsetDateTime getLastCheckedAt() { return lastCheckedAt; }
    public void setLastCheckedAt(OffsetDateTime lastCheckedAt) { this.lastCheckedAt = lastCheckedAt; }
}
