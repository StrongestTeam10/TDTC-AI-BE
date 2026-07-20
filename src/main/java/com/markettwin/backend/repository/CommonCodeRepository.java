package com.markettwin.backend.repository;

import com.markettwin.backend.domain.entity.CommonCode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommonCodeRepository extends JpaRepository<CommonCode, String> {
}
