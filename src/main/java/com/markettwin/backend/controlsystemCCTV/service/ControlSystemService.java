package com.markettwin.backend.controlsystemCCTV.service;

import com.markettwin.backend.controlsystemCCTV.entity.EmergencyAlert;
import com.markettwin.backend.controlsystemCCTV.entity.PostReport;
import com.markettwin.backend.controlsystemCCTV.repository.EmergencyAlertRepository;
import com.markettwin.backend.controlsystemCCTV.repository.PostReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.markettwin.backend.controlsystemCCTV.entity.PedestrianCoordinateJson;
import com.markettwin.backend.controlsystemCCTV.repository.PedestrianCoordinateJsonRepository;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ControlSystemService {

    private final EmergencyAlertRepository emergencyAlertRepository;
    private final PostReportRepository postReportRepository;
    private final PedestrianCoordinateJsonRepository jsonCoordRepository;

    public List<EmergencyAlert> getUnresolvedAlerts() {
        return emergencyAlertRepository.findByIsResolvedFalse();
    }

    public List<PostReport> getReportsByDate(LocalDate date, Integer videoId) {
        return postReportRepository.findByTargetDateAndVideoId(date, videoId);
    }

    public List<PedestrianCoordinateJson> getJsonCoordinates(Long clipId, Integer frameId, Integer videoId) {
        return jsonCoordRepository.findByClipIdAndFrameIdAndVideoId(clipId, frameId, videoId);
    }


}
