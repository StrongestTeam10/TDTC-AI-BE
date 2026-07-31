package com.markettwin.backend.controlsystemCCTV.repository;

import com.markettwin.backend.controlsystemCCTV.entity.VideoClip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VideoClipRepository extends JpaRepository<VideoClip, Long> {
}
