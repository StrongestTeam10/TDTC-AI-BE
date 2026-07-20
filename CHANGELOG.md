# Changelog

이 파일은 Claude와의 작업 세션에서 변경된 내용을 기록합니다.
각 항목은 zip으로 전달된 시점 기준입니다.

## [미출시] - 2026-07-20 (3차)
### 추가
- 나머지 14개 테이블 엔티티/리포지토리 (User, EntityChangeLog, Scenario, ScenarioResult,
  Facility, CrowdDensityLog, Sensor, AcousticEvent/Log, LidarReading/Log, RadarReading/Log, CommonCode)
### 수정
- README.md (TODO 체크리스트, ERD 편차사항 섹션)
- schema-init.sql (4개 → 18개 테이블 전체로 확장)
### 변경 없음
- build.gradle, Dockerfile, .gitignore, settings.gradle, BackendApplication.java,
  CorsConfig.java, WebClientConfig.java, Market/Zone/CrowdDensity/Risk 엔티티,
  MarketService, DashboardService, MarketController, DashboardController,
  SimulationController, SimulationService, SimulationEngineClient, DTO 일체

## [미출시] - 2026-07-20 (2차)
### 변경
- 엔티티/서비스/컨트롤러/DTO 전면 교체: SpatialNode/CctvDetection/AlertLog(임의 이름) →
  Market/Zone/CrowdDensity/Risk(실제 ERD 기준 MRKADDR01M/MRKADDR01D/CRDDNST01M/MRKRISK01M)
### 삭제
- SpatialService, RiskScoringService, SpatialController, SpatialNodeDto, AlertLogDto

## [미출시] - 2026-07-20 (1차)
### 변경
- 빌드 도구를 Maven(pom.xml)에서 Gradle(build.gradle)로 전환

## [미출시] - 2026-07-17
### 추가
- 초기 Spring Boot 백엔드 구조 (레이어드 아키텍처 + FastAPI 연동 클라이언트)

## [미출시] - 2026-07-20 (4차)
### 수정
- schema-init.sql: 테이블명 표기를 전부 명시적 소문자로 통일 (기존엔 대문자로 적혀있었으나 실행 시 자동으로 소문자 변환되던 것을, 혼동 방지를 위해 파일 자체를 소문자로 재작성)
- 18개 엔티티의 @Table(name=...) 어노테이션을 동일하게 소문자로 통일 (기능 변화 없음, 표기 일관성 목적)
### 추가
- enable-rls.sql: Supabase RLS 활성화용 스크립트 (소문자 테이블명, 대소문자 폴딩 문제 회피)

## [미출시] - 2026-07-20 (5차)
### 수정 - 최신 ERD 재대조 반영
- USRUSR501M: roles_code/org_code VARCHAR(3)→VARCHAR(5), created_ip/updated_ip VARCHAR(15)→VARCHAR(16)
- SIMSCNR01M: policy_type_code VARCHAR(5)→VARCHAR(3)
- SIMRSLT01D: predicted_risk_score(오류) → predicted_density로 정정, generated_report_path VARCHAR(300)→VARCHAR(1000), avg_stay_time INTEGER→INTERVAL(Duration+@JdbcTypeCode 매핑)
- MRKFCTS01M: facility_type/name VARCHAR(30)→VARCHAR(50)
- MRKRISK01M: risk_score NOT NULL 추가, reason_code VARCHAR(300)→VARCHAR(200)
- SENSENS01M: sensor_type_code VARCHAR(3)→VARCHAR(5)
- SENLIDR01M: pt_cloud_cnt NOT NULL 추가
- COMCODE01M: code_name VARCHAR(30)→VARCHAR(50), code_desc→describe로 필드명 복원(예약어 아님 확인됨), mrk VARCHAR(200)→VARCHAR(500)
### 참고
- SIMSCNR01M.scenario_name이 ERD상 TIMESTAMP 타입으로 표기되어 있으나 명백한 오타로 판단, VARCHAR(100) 유지 (원본 ERD 작성자 확인 권장)
