package com.markettwin.backend.service;

import com.markettwin.backend.dto.request.MetricBulkRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetricBatchService {

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public Map<String, Object> bulkInsertMetrics(MetricBulkRequest payload) {
        List<MetricBulkRequest.FrameMetricDto> list = payload.getFrames();
        if (list == null || list.isEmpty()) {
            return Map.of("clipId", payload.getClipId(), "insertedCoordinates", 0, "insertedRisks", 0);
        }

        Long clipId = payload.getClipId();
        Long zoneId = payload.getZoneId();

        String sqlPedaggrAndRisk =
                "WITH inserted_ped AS (" +
                        "    INSERT INTO pedaggr01h (clip_id, zone_id, frame_id, video_id, pixels_json, bev_xyz_json, captured_at) " +
                        "    VALUES (?, ?, ?, ?, ?::jsonb, ?::jsonb, ?) " +
                        "    RETURNING coord_id" +
                        ") " +
                        "INSERT INTO mrkrisk01m (coord_id, risk_score, risk_level, reason_code, detected_at, total_count, occupancy_rate, stagnation_sec, video_url) " +
                        "SELECT coord_id, ?, ?, ?, ?, ?, ?, ?, ? " +
                        "FROM inserted_ped";

        try {
            jdbcTemplate.batchUpdate(sqlPedaggrAndRisk, list, 100, (ps, m) -> {
                Timestamp captured = Timestamp.from(m.getCapturedAt() != null ? Instant.parse(m.getCapturedAt()) : Instant.now());

                // pedaggr01h
                ps.setLong(1, clipId);
                ps.setLong(2, zoneId);
                ps.setObject(3, m.getFrameId());
                ps.setObject(4, m.getVideoId() != null ? m.getVideoId() : 1L);
                ps.setString(5, m.getPixelsJson() != null ? m.getPixelsJson() : "{}");
                ps.setString(6, m.getBevXyzJson() != null ? m.getBevXyzJson() : "{}");
                ps.setTimestamp(7, captured);

                // mrkrisk01m
                ps.setDouble(8, m.getRiskScore() != null ? m.getRiskScore() : 0.0);
                ps.setString(9, m.getRiskLevel() != null ? m.getRiskLevel() : "SAFE");
                ps.setString(10, m.getReasonCode() != null ? m.getReasonCode() : "AI_REALTIME_CRI");
                ps.setTimestamp(11, captured);
                ps.setInt(12, m.getTotalCount() != null ? m.getTotalCount() : 0);
                ps.setDouble(13, m.getOccupancyRate() != null ? m.getOccupancyRate() : 0.0);
                ps.setDouble(14, m.getStagnationSec() != null ? m.getStagnationSec() : 0.0);
                ps.setString(15, m.getVideoUrl());
            });
        } catch (DuplicateKeyException e) {
            //이미 적재된 데이터면 409 Conflict 처리
            throw new IllegalArgumentException("ALREADY_EXISTS");
        }

        log.info("[MetricBatchService] 동시 적재 완료: {}건 (clipId={})", list.size(), clipId);
        return Map.of("clipId", clipId, "insertedCoordinates", list.size(), "insertedRisks", list.size());
    }
}
