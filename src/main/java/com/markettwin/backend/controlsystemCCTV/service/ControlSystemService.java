package com.markettwin.backend.controlsystemCCTV.service;

import com.markettwin.backend.controlsystemCCTV.entity.EmergencyAlert;
import com.markettwin.backend.controlsystemCCTV.entity.PedestrianCoordinate;
import com.markettwin.backend.controlsystemCCTV.entity.PostReport;
import com.markettwin.backend.controlsystemCCTV.repository.EmergencyAlertRepository;
import com.markettwin.backend.controlsystemCCTV.repository.PedestrianCoordinateRepository;
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
    private final PedestrianCoordinateRepository coordRepository;
    private final PedestrianCoordinateJsonRepository jsonCoordRepository;

    public List<EmergencyAlert> getUnresolvedAlerts() {
        return emergencyAlertRepository.findByIsResolvedFalse();
    }

    public List<PostReport> getReportsByDate(LocalDate date, String analysisMode, Long videoId) {
        return postReportRepository.findByTargetDateAndAnalysisModeAndVideoId(date, analysisMode, videoId);
    }

    public List<PedestrianCoordinate> getCoordinates(Long zoneId, Integer frameId, String analysisMode, Long videoId) {
        return coordRepository.findByZoneIdAndFrameIdAndAnalysisModeAndVideoId(zoneId, frameId, analysisMode, videoId);
    }

    public List<PedestrianCoordinateJson> getJsonCoordinates(Long zoneId, Integer frameId, String analysisMode, Long videoId) {
        return jsonCoordRepository.findByZoneIdAndFrameIdAndAnalysisModeAndVideoId(zoneId, frameId, analysisMode, videoId);
    }

}

