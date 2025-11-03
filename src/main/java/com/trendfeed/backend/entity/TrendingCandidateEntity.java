package com.trendfeed.backend.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "trending_candidates")
public class TrendingCandidateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long repoId;
    private String fullName;
    private OffsetDateTime promotedAt;
    private Boolean givenToAI;

    public TrendingCandidateEntity() {}

    public TrendingCandidateEntity(Long repoId, String fullName) {
        this.repoId = repoId;
        this.fullName = fullName;
        this.promotedAt = OffsetDateTime.now();
        this.givenToAI = false;
    }

    // Getter/Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getRepoId() { return repoId; }
    public void setRepoId(Long repoId) { this.repoId = repoId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public OffsetDateTime getPromotedAt() { return promotedAt; }
    public void setPromotedAt(OffsetDateTime promotedAt) { this.promotedAt = promotedAt; }

    public Boolean getGivenToAI() { return givenToAI; }
    public void setGivenToAI(Boolean givenToAI) { this.givenToAI = givenToAI; }
}
