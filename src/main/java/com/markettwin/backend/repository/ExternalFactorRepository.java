package com.markettwin.backend.repository;

import com.markettwin.backend.domain.entity.ExternalFactor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExternalFactorRepository extends JpaRepository<ExternalFactor, Long> {
}
