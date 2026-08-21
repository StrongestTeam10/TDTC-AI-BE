package com.markettwin.backend.repository;

import com.markettwin.backend.domain.entity.PedestrianCoordinateJson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PedestrianCoordinateJsonRepository extends JpaRepository<PedestrianCoordinateJson, Long> {
    List<PedestrianCoordinateJson> findByClipIdAndFrameIdAndVideoId(
            Long clipId, Integer frameId, Long videoId);

    /** 추가(관측 초기배치): 특정 영상(clip)의 특정 프레임 1건. */
    Optional<PedestrianCoordinateJson> findFirstByClipIdAndFrameId(Long clipId, Integer frameId);

    /** 그 영상의 최대 frame_id(=프레임 수 근사). 반복재생 프레임 선택에 쓴다. */
    @Query("SELECT max(p.frameId) FROM PedestrianCoordinateJson p WHERE p.clipId = :clipId")
    Integer findMaxFrameId(@Param("clipId") Long clipId);

    /** 추가(자동청소 스케줄러): capturedAt이 기준시각보다 오래된 보행자 좌표 삭제. */
    @Modifying
    @Query("DELETE FROM PedestrianCoordinateJson p WHERE p.capturedAt < :threshold")
    void deleteByCapturedAtBefore(@Param("threshold") Instant threshold);

    // 추가: 부모(clipId) 리스트를 기반으로 자식 일괄 삭제
    @Modifying
    @Query("DELETE FROM PedestrianCoordinateJson p WHERE p.clipId IN :clipIds")
    void deleteByClipIdsIn(@Param("clipIds") List<Long> clipIds);

}