package com.markettwin.backend.service;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.markettwin.backend.domain.entity.Risk;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
public class PdfReportService {

    public byte[] generateEmergencyReport(Long zoneId, Risk riskData) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, baos);
            document.open();

            document.add(new Paragraph("🚨 Emergency Dispatch Report 🚨"));
            document.add(new Paragraph("Target Zone: " + zoneId));
            document.add(new Paragraph("--------------------------------------------------"));

            // 🟢 실제 Risk 엔티티에 있는 메서드명(getTotalCount, getRiskScore)으로 변경 완료!
            document.add(new Paragraph("Pedestrian Count: " + riskData.getTotalCount() + " people"));
            document.add(new Paragraph("Occupancy Rate: " + riskData.getOccupancyRate() + " %"));
            document.add(new Paragraph("Stagnation Time: " + riskData.getStagnationSec() + " sec"));
            document.add(new Paragraph("CRI Score: " + riskData.getRiskScore()));

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("PDF 생성 중 오류 발생", e);
        }
    }
}
