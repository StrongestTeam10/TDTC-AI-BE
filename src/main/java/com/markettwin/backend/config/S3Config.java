package com.markettwin.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * 2026-07-24 추가 (게시판 첨부파일 기능)
 *
 * 자격증명은 코드/설정 파일에 직접 넣지 않고 AWS 기본 자격증명 체인
 * (환경변수 AWS_ACCESS_KEY_ID/AWS_SECRET_ACCESS_KEY, ~/.aws/credentials,
 * 운영 환경의 EC2/ECS 인스턴스 role 등)을 그대로 사용함 - S3Client.builder()에
 * credentialsProvider를 별도로 지정하지 않으면 SDK가 자동으로 이 체인을 탐색함.
 *
 * ⚠️ 로컬 개발 시 필요 사전 준비 (README에도 안내):
 *   1) S3 버킷 생성 (예: tdtc-ai-board-attachments)
 *   2) 로컬 환경변수로 AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY 설정
 *      (또는 aws configure로 ~/.aws/credentials에 등록)
 *   3) application-local.yml의 aws.s3.bucket 값을 실제 버킷명으로 맞추기
 */
@Configuration
public class S3Config {

    @Value("${aws.region:ap-northeast-2}")
    private String region;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .region(Region.of(region))
                .build();
    }
}
