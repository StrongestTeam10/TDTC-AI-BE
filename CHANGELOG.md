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

## [미출시] - 2026-07-20 (5차)
### 수정 - 최신 ERD 재대조 반영
- USRUSRS01M: roles_code/org_code VARCHAR(3)→VARCHAR(5), created_ip/updated_ip VARCHAR(15)→VARCHAR(16)
- SIMSCNR01M: policy_type_code VARCHAR(5)→VARCHAR(3)
- SIMRSLT01D: predicted_risk_score(오류) → predicted_density로 정정, generated_report_path VARCHAR(300)→VARCHAR(1000), avg_stay_time INTEGER→INTERVAL(Duration+@JdbcTypeCode 매핑)
- MRKFCTS01M: facility_type/name VARCHAR(30)→VARCHAR(50)
- MRKRISK01M: risk_score NOT NULL 추가, reason_code VARCHAR(300)→VARCHAR(200)
- SENSENS01M: sensor_type_code VARCHAR(3)→VARCHAR(5)
- SENLIDR01M: pt_cloud_cnt NOT NULL 추가
- COMCODE01M: code_name VARCHAR(30)→VARCHAR(50), code_desc→describe로 필드명 복원(예약어 아님 확인됨), mrk VARCHAR(200)→VARCHAR(500)
### 참고
- SIMSCNR01M.scenario_name이 ERD상 TIMESTAMP 타입으로 표기되어 있으나 명백한 오타로 판단, VARCHAR(100) 유지 (원본 ERD 작성자 확인 권장)

## [미출시] - 2026-07-20 (6차)
### 수정
- CorsConfig.java: cors.allowed-origins에 기본값(http://localhost:5173) 추가 - profile 미지정 시에도 앱이 죽지 않도록 방어

## [미출시] - 2026-07-20 (7차)
### 추가
- .env.example: 로컬(DEV_)/운영(PRO_) 환경변수 통합 템플릿, 향후 추가 변수는 하단에 이어서 작성하는 구조
### 수정
- application-prod.yml: DB_URL/DB_USERNAME/DB_PASSWORD/SIMULATION_ENGINE_URL/FRONTEND_ORIGIN -> PRO_ 접두사로 통일 (DEV_ 접두사와 짝 맞춤)
- .gitignore: .env 추가

## [미출시] - 2026-07-20 (8차)
### 추가 - 파이프라인 A 준비
- MRKADJC01M(구역 인접 관계) 테이블 신규 추가: ZoneAdjacency 엔티티, ZoneAdjacencyRepository, DDL
  - Mesa NetworkGrid 및 유동인구 이동 경로 계산의 기반 데이터
  - path_width(통로 폭): 병목/수용 인원 계산용
  - is_active: 통로 폐쇄 시나리오(파이프라인 B)에서 스키마 변경 없이 활용
  - UNIQUE(from_zone_id, to_zone_id), CHECK(자기 자신 참조 금지) 제약 포함
- drop-all.sql, enable-rls.sql에도 신규 테이블 반영 (총 19개 테이블)

## [미출시] - 2026-07-20 (9차)
### 추가 - 실제 시장 공간 데이터 적재
- seed-market-data.sql: 실제 시장 폴리곤/출입구 좌표 기반 시드 데이터
  - 시장 1개(망원시장, 남북 240m 골목형, 면적 1682m2)
  - 구역 3개(남측/중앙/북측) - 출입구 위치 기준 분할, GeoJSON 형식으로 polygon_coordinates 저장
  - 출입구 6개 - facility_type='GATE'
  - 구역 인접 관계 4행 (Z1<->Z2, Z2<->Z3 양방향), 실측 통로폭/거리 반영
### 수정 - ERD 변경
- MRKFCTS01M에 latitude DECIMAL(10,8), longitude DECIMAL(11,8) 컬럼 추가
  - 사유: 출입구는 시뮬레이션에서 에이전트 생성/소멸 지점이라 좌표가 필수인데 기존 스키마에 위치 정보가 없었음
- schema-init.sql: 더미 샘플 데이터(테스트 전통시장/A,B구역) 제거, seed-market-data.sql로 분리
### 계산 근거
- 구역 경계는 중간 출입구 쌍의 평균 위도(W1/E1 -> 37.55589589, W2/E2 -> 37.55656358) 기준
- 통로 폭은 구역 면적 / 대각 연장 길이로 산출 (골목이 대각선 방향이라 수평 절단폭은 과대평가되므로 배제)
- 출입구 6개 모두 폴리곤 경계로부터 0~2.5m 이내에 위치함을 사전 검증

## [미출시] - 2026-07-21 (10차)
### 수정 - 확대 ERD 3장 재대조 반영
- SENRAD01M/H -> SENRADR01M/H 테이블명 변경 (네이밍 규칙 3+4+2+1 준수)
- USRUSRS01M: name VARCHAR(30)->VARCHAR(50), roles_code -> rules_code(컬럼명), created_ip NOT NULL 추가
- MRKADDR01M: latitude DECIMAL(10,6)->(10,8), longitude DECIMAL(11,6)->(11,8)
- MRKADDR01D: zone_name VARCHAR(30)->VARCHAR(50) + NOT NULL
- COMCODE01M: code VARCHAR(3)->VARCHAR(5), mrk -> rmk(컬럼명)
- SIMSCNR01M: policy_type_code VARCHAR(3)->VARCHAR(5) + NOT NULL, agent_count NOT NULL
- SIMRSLT01D: predicted_risk_score(INT) 복원 - ERD에 3개 예측값이 모두 존재함 확인
- SENLIDR01M/H, SENRADR01M/H: status_level_code VARCHAR(3)->VARCHAR(5)
### 확정
- 센서 테이블 PK는 단일 PK 유지 (crowd_density_id가 SERIAL로 이미 유일하므로 복합 PK는 논리적 중복)
- MRKADJC01M은 ERD 이미지상 MRKADDR01D의 복사본이었으나, 인접 관계 전용 스키마로 교체 확정

## [미출시] - 2026-07-22 (11차)
### 수정 - 최종 ERD 재대조 반영
- CRDDNST01M, MRKRISK01M, SENSENS01M에서 market_id 컬럼 제거
  - 사유: zone_id로 MRKADDR01D를 조인하면 얻을 수 있는 중복 정보. ERD 정규화 반영
  - 영향: 시장 단위 조회는 MRKADDR01D 조인이 필요해짐 (CrowdDensityRepository에 @Query 추가)
- SIMRSLT01D.predicted_density NOT NULL 제약 추가
- crddnst01m 인덱스를 market_id -> zone_id 기준으로 변경
### 검증
- 19개 테이블 전체 컬럼 구성이 ERD와 일치함을 자동 대조로 확인
- Java 엔티티 18개 전체가 DDL과 일치함을 자동 대조로 확인
- 시뮬레이션 엔진 회귀 테스트 통과 (망원시장 4개 시나리오)

## [미출시] - 2026-07-22 (12차)
### 추가
- comcode-seed.sql: _code로 끝나는 5개 도메인의 공통코드 정의 (18건)
  - 규칙: 5자 고정 = [3자 도메인 접두사] + [2자 순번 또는 약어]
  - ROL(권한, 순번) / ORG(기관, 약어) / SEN(센서종류, 약어) / LVL(위험도, 순번) / POL(시나리오유형, 약어)
  - reason_code(VARCHAR(200))는 자유 텍스트라 공통코드 대상에서 제외
