package com.markettwin.backend.repository;

import com.markettwin.backend.domain.entity.ExternalFactor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExternalFactorRepository extends JpaRepository<ExternalFactor, Long> {

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM ExternalFactor e WHERE e.updatedAt < :threshold")
    void deleteByUpdatedAtBefore(@org.springframework.data.repository.query.Param("threshold") java.time.Instant threshold);

}
