package com.markettwin.backend.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * 상점 외관 직접 촬영 데이터 수집 파이프라인
 *
 * 사진 파일에서 EXIF GPS 좌표와 촬영일시(DateTimeOriginal)를 서버(BE)에서 직접
 * 추출한다. 스마트폰 GPS 오차(5~15m)와 좁은 골목의 반사 오차 때문에 이 값은
 * "참고용 초기값"일 뿐이고, 최종 위치는 항상 사용자가 지도에서 보정한다.
 * EXIF 태그 자체가 없는 사진(스크린샷, GPS 꺼둔 채 촬영 등)도 흔하므로 모든
 * 필드가 null일 수 있음을 호출부가 감안해야 한다.
 */
@Component
public class ExifGpsExtractor {

    private static final Logger log = LoggerFactory.getLogger(ExifGpsExtractor.class);

    public Result extract(MultipartFile file) {
        // 수정(보안 감사 BE-14): 업로드 스트림을 try-with-resources로 닫는다.
        //
        // ImageMetadataReader.readMetadata()는 넘겨받은 스트림을 닫아주지 않는다.
        // 상점 사진은 한 장을 등록할 때 이 메서드가 두 번 호출되므로(previewExif에서
        // 한 번, save에서 다시 한 번) 열린 스트림이 그만큼 쌓인다. 파일 기반
        // MultipartFile은 임시 파일 핸들을 잡기 때문에 업로드가 몰리면 문제가 된다.
        try (InputStream in = file.getInputStream()) {
            Metadata metadata = ImageMetadataReader.readMetadata(in);

            BigDecimal latitude = null;
            BigDecimal longitude = null;
            GpsDirectory gpsDirectory = metadata.getFirstDirectoryOfType(GpsDirectory.class);
            if (gpsDirectory != null) {
                GeoLocation geoLocation = gpsDirectory.getGeoLocation();
                if (geoLocation != null && !geoLocation.isZero()) {
                    latitude = BigDecimal.valueOf(geoLocation.getLatitude());
                    longitude = BigDecimal.valueOf(geoLocation.getLongitude());
                }
            }

            Instant capturedAt = null;
            ExifSubIFDDirectory exifSubIFDDirectory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (exifSubIFDDirectory != null) {
                java.util.Date dateOriginal = exifSubIFDDirectory.getDateOriginal();
                if (dateOriginal != null) {
                    capturedAt = dateOriginal.toInstant();
                }
            }

            return new Result(latitude, longitude, capturedAt);
        } catch (ImageProcessingException | IOException e) {
            // EXIF가 없거나 손상된 이미지 형식일 수 있음 - 업로드 자체를 막을 이유는 아니라서
            // 값 없이 진행하고 로그만 남김(사용자는 어차피 지도에서 위치를 직접 보정함)
            log.warn("EXIF 추출 실패(값 없이 진행): filename={}, reason={}", file.getOriginalFilename(), e.getMessage());
            return new Result(null, null, null);
        }
    }

    public record Result(BigDecimal latitude, BigDecimal longitude, Instant capturedAt) {
        public boolean hasGps() {
            return latitude != null && longitude != null;
        }
    }
}
