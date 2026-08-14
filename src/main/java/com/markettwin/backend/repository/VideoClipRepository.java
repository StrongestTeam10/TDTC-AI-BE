package com.markettwin.backend.repository;

import com.markettwin.backend.domain.entity.VideoClip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface VideoClipRepository extends JpaRepository<VideoClip, Long> {

    // 1시간이 지났고, raw-videos 폴더에 있는 영상의 상태를 삭제(true)로 일괄 변경
    @Modifying
    @Query("UPDATE VideoClip v SET v.isDeleted = true WHERE v.s3ClipUrl LIKE '%raw-videos%' AND v.startTime < :threshold")
    int markOldRawVideosAsDeleted(@Param("threshold") Instant threshold);

    // ★ 추가됨: zone_id로 영상 클립 목록 조회
    List<VideoClip> findByZoneId(Long zoneId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM VideoClip v WHERE v.clipType = 'TEMP' AND v.startTime < :threshold")
    void deleteTempClipsOlderThan(@org.springframework.data.repository.query.Param("threshold") java.time.Instant threshold);

}
