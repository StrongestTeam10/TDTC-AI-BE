package com.markettwin.backend.repository;

import com.markettwin.backend.domain.entity.VideoClip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VideoClipRepository extends JpaRepository<VideoClip, Long> {
}
