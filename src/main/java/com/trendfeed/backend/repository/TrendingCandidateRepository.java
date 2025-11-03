package com.trendfeed.backend.repository;

import com.trendfeed.backend.entity.TrendingCandidateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TrendingCandidateRepository extends JpaRepository<TrendingCandidateEntity, Long> {

    boolean existsByRepoId(Long repoId);

    // 오래된 순으로 아직 AI에 주지 않은 후보 n개
    @Query(value = """
            SELECT tc FROM TrendingCandidateEntity tc
            WHERE tc.givenToAI = false
            ORDER BY tc.promotedAt ASC
            """)
    List<TrendingCandidateEntity> findByGivenToAIFalseOrderByPromotedAtAsc(org.springframework.data.domain.Pageable pageable);

    // Pageable 없이 limit 숫자만 받기 위한 헬퍼 (스프링 데이터가 Pageable 필수라 별도 default 메서드 제공)
    default List<TrendingCandidateEntity> findByGivenToAIFalseOrderByPromotedAtAsc(int limit) {
        return findByGivenToAIFalseOrderByPromotedAtAsc(org.springframework.data.domain.PageRequest.of(0, limit));
    }
}
