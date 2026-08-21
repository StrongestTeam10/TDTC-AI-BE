package com.markettwin.backend.repository;

import com.markettwin.backend.domain.entity.ExternalFactor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExternalFactorRepository extends JpaRepository<ExternalFactor, Long> {

    // 기존 메서드를 대체: 고립된 외부 요인(자식 영상이 없는 요인)만 삭제
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM ExternalFactor e WHERE e.updatedAt < :threshold AND e.factorId NOT IN (SELECT v.factorId FROM VideoClip v WHERE v.factorId IS NOT NULL)")
    void deleteUnreferencedOlderThan(@org.springframework.data.repository.query.Param("threshold") java.time.Instant threshold);


}
