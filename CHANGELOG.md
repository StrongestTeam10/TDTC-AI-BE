# Changelog

이 파일은 Claude와의 작업 세션에서 변경된 내용을 기록합니다.
각 항목은 zip으로 전달된 시점 기준입니다.

### 2026-07-24 (로그인/회원가입 API 구현 - Spring Security + JWT 최초 도입)
- FE의 authStore mock 로그인을 대체할 실제 인증 API를 처음 구현함
- 🆕 `POST /api/auth/signup`: 회원가입. FE SignupPage 입력값(아이디/비밀번호/이름/
  소속기관/동의 항목)과 대응. 비밀번호는 BCrypt로 해시 저장, 비밀번호 조합 규칙
  (8자 이상 + 영문 대/소문자·숫자·특수문자 모두 포함)을 FE와 동일하게 서버에서도
  재검증. 소속기관(`orgCode`)은 `comcode01m`의 `ORG` 도메인 코드인지 확인 후 저장.
  자가 가입 시 기본 권한은 `ROL03`(조회자, 최소 권한)로 고정 — 관리자/관제요원으로의
  승격은 이번 범위에 없음(추후 별도 승인 절차 필요)
- 🆕 `POST /api/auth/login`: 로그인. 아이디/비밀번호 검증 후 JWT 액세스 토큰 발급
  (기본 만료 1시간, `jwt.expiration-ms`로 설정 가능)
- 🆕 `GET /api/auth/me`: 토큰으로 로그인 상태 확인(선택적으로 FE에서 활용 가능)
- 🆕 `GET /api/common-codes?domain=ORG`: 공통코드 조회 API. FE `constants/orgCode.ts`에
  하드코딩돼 있던 소속기관 옵션을 실제 DB(`comcode01m`) 조회로 대체할 수 있게 함
- ✏️ `User.java`/`schema-init.sql`: `usrusrs01m`에 동의 이력 컬럼 3개 추가
  (`agree_terms_at`, `agree_privacy_at`, `agree_marketing_at`, 전부 nullable TIMESTAMP).
  기존 DB에도 반영되도록 `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` 포함
- 🆕 Spring Security 최초 도입: `security/JwtTokenProvider.java`(JWT 발급/검증),
  `security/JwtAuthenticationFilter.java`(Authorization 헤더 검증), `config/SecurityConfig.java`
- 🗑️ `config/CorsConfig.java` 삭제 → `SecurityConfig`의 `corsConfigurationSource` 빈으로 통합
  (Spring Security 필터 단계 CORS 처리와 WebMvcConfigurer 방식이 중복/충돌할 수 있어서)
- ⚠️ **중요한 범위 결정**: 지금 `SecurityConfig`는 전체 API를 `permitAll`로 열어둠
  (로그인/회원가입 포함). 기존 대시보드/시뮬레이션/시장 API가 FE에서 아직 토큰을
  붙여 호출하도록 연동되지 않았기 때문에, 지금 바로 잠그면 파이프라인 A/B가 전부
  깨짐. **FE가 로그인 후 토큰을 모든 API 호출에 Authorization 헤더로 붙이도록
  연동되면, 그 다음 작업으로 `anyRequest().permitAll()` → `anyRequest().authenticated()`로
  좁히는 걸 권장함** (`SecurityConfig.java` 주석에도 명시)
- ✏️ `build.gradle`: `spring-boot-starter-security`, `jjwt-api/impl/jackson:0.12.6` 추가
- ✏️ `application-local.yml`/`application-prod.yml`: `jwt.secret`(로컬은 기본값 포함,
  운영은 환경변수 `JWT_SECRET` 필수), `jwt.expiration-ms` 추가
- ⚠️ **참고(기존 제약, 이번에 만든 건 아님)**: `usrusrs01m.created_ip`가 `VARCHAR(16)`인데,
  운영 환경에서 IPv6 주소가 그대로 들어오면 16자를 넘겨 저장 실패할 수 있음. 지금
  당장 문제는 아니지만 운영 배포 전 컬럼 길이 검토를 권장함
- ⚠️ **이 환경(샌드박스)에서는 Maven Central/Gradle 배포 서버 접근이 막혀 있어서
  실제 컴파일(`./gradlew build`)로 검증하지 못했습니다.** 코드 리뷰 수준으로는
  문제를 못 찾았지만, 받으시면 로컬에서 `./gradlew build`를 꼭 한 번 돌려서 확인
  부탁드립니다. 에러 나면 로그 그대로 붙여주시면 바로 봐드릴게요
- FE 연동은 이번 작업 범위에 포함 안 함: `authStore.ts`는 여전히 mock 계정으로
  동작 중이며, 실제 로그인/회원가입 API를 붙이는 건 별도 다음 작업으로 진행 필요

### 2026-07-24 (오브젝트 점유 반경 컬럼 추가 — SIM 장애물 회피용)
- SIM이 격자 기반 이동으로 바뀌면서 매대/푸드트럭을 실제 장애물로 취급해 회피
  경로를 계산하게 됨. 그 오브젝트가 차지하는 물리적 반경 데이터가 필요해서 추가
- ✏️ `Facility.java`/`schema-init.sql`: `mrkfcts01m`에 `footprint_radius_m`(DOUBLE
  PRECISION, nullable) 컬럼 추가. 값이 없으면 SIM이 임시 기본값(1.2m)으로 대체하므로
  기존 데이터도 그대로 동작함
- 기존에 이미 생성돼 있던 DB에도 반영되도록 `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` 포함

### 2026-07-24 (예측 요청 필드 변경: inflowPerStep -> totalInflow)
- SIM `PredictRequest`가 "스텝당 고정 인원" 대신 "총 인원(랜덤 분산)"으로 바뀐 것과
  맞춰 `PredictRequestDto.inflowPerStep` → `totalInflow`로 변경

### 2026-07-24 (통로 중심선 데이터 컬럼 추가 — SIM 동선 정확도 개선용)
- SIM이 구역 간 실제 통로 모양(꺾인 골목 등)을 반영해 걷도록 하기 위한 데이터 컬럼
- ✏️ `ZoneAdjacency.java`/`schema-init.sql`: `mrkadjc01m`에 `path_coordinates`(TEXT,
  GeoJSON LineString) 컬럼 추가. NULL 허용 — 안 채워져 있으면 SIM이 자동으로 기존
  방식(경계 중점 근사)으로 대체하므로 기존 데이터는 그대로 동작함
- 기존에 이미 생성돼 있던 DB에도 반영되도록 `ALTER TABLE ... ADD COLUMN IF NOT EXISTS`
  포함
- ⚠️ 아직 이 컬럼을 입력/편집하는 BE API(컨트롤러)는 없음 — 지금은 레이아웃 에디터가
  아직 BE와 완전히 통합되기 전이라, 값 입력은 DB에 직접 SQL로 넣는 방식으로 진행
  예정. 나중에 에디터-BE 통합 시 이 컬럼도 CRUD API에 포함시켜야 함

### 2026-07-24 (WebClient 버퍼 한도 초과로 인한 502 수정)
- **증상**: FE에서 예측 시뮬레이션(steps=30, 기본값)을 실행하면 502 발생. curl로 steps=10은
  성공, PowerShell `Invoke-RestMethod`로도 steps=10은 성공했는데 FE 기본값(steps=30)에서만
  재현됨
- **원인**: `WebClientConfig`가 응답 버퍼 크기를 별도 설정하지 않아 Spring 기본값(256KB)을
  그대로 썼음. 예측 스텝이 늘고 게이트 유입으로 에이전트 수가 누적되면 `frames` 응답
  JSON이 256KB를 쉽게 넘어서 `DataBufferLimitException` → `SimulationEngineException` →
  (의도된 동작으로) 502 응답까지 이어짐
- ✏️ `WebClientConfig`: `ExchangeStrategies`로 응답 버퍼 한도를 10MB로 상향
- ✏️ `GlobalExceptionHandler`: 예외를 콘솔에 전혀 로깅하지 않고 조용히 삼키던 문제도
  같이 발견해서 `log.error(...)` 추가 (이번 디버깅이 오래 걸린 이유이기도 함 — 실제
  원인이 콘솔에 하나도 안 찍혀서 추적이 어려웠음)
- 참고: 502 자체는 원래 `GlobalExceptionHandler`가 `SimulationEngineException`을 의도적으로
  502로 매핑하도록 설계돼 있던 것 (제가 새로 만든 동작 아님) — SIM 호출이 어떤 이유로든
  실패하면 항상 502로 응답함

### 2026-07-24 (예측 시뮬레이션 API 추가 — SIM `/simulate/predict` 연동)
- SIM에 신규 추가된 `/simulate/predict`(실측 상태 + 게이트 신규 유입 기반 예측)를
  호출하는 BE 계약 추가
- 🆕 `PredictRequestDto`(marketId, capturedAt, steps, inflowPerStep, seed)
- 🆕 `ZoneRiskPointDto`, `RiskTrendPointDto`(스텝별 위험도 추이), `PredictResultDto`
- ✏️ `SimulationEngineClient`에 `predict()` 추가 (`POST /simulate/predict`, 타임아웃 60초 —
  최대 1000스텝까지 갈 수 있어 시나리오(30초)보다 여유를 둠)
- ✏️ `SimulationService`/`SimulationController`에 `predict()` 추가 → `POST /api/simulation/predict`
- ✏️ `Facility.java`/`schema-init.sql`: `mrkfcts01m`에 `weight` 컬럼 추가
  (GATE=유입 가중치, 그 외 시설=매력도 가중치). 기존에 이미 생성돼 있던 DB에도 반영되도록
  `ALTER TABLE ... ADD COLUMN IF NOT EXISTS`도 함께 추가함
- ⚠️ 아직 미완료: 실제 매대(STALL) 시설 데이터 입력 — 재재님이 레이아웃 에디터로 직접
  입력 예정. 그 전까지는 GATE 외 시설이 없어 `attraction`이 전부 0으로 계산됨 (예측은
  동작하지만 매대 쪽으로 쏠리는 움직임은 안 보임)
- 실제 gradle 빌드는 이 환경(네트워크 제약)에서 검증 못 함 — 로컬에서 빌드 확인 필요

### 2026-07-24 (파이프라인 A 실제 구현 — SIM 실시간 호출)
- **문제**: `DashboardService`가 SIM을 전혀 호출하지 않고 `CrowdDensityRepository`/
  `RiskRepository`로 DB(`crddnst01m`, `mrkrisk01m`)를 직접 조회만 했음. 실제 센서 장비가
  없어 시딩 데이터 갱신 코드가 전혀 없었기 때문에, "실시간 관제"가 아니라 마지막 수동
  호출/시딩 시점의 잔여값을 보여주는 상태였음
- **해결**: `DashboardService`가 `SimulationEngineClient.getSnapshot()`으로 SIM
  `POST /simulate/snapshot`을 실제로 호출하도록 전면 재작성
- 🆕 `SnapshotRequestDto`(marketId 필수, capturedAt, persistRisk, includeAgents) 추가
- 🆕 `ZoneResultDto`(SIM `ZoneResult`와 1:1 매칭, 기존 `RiskBreakdownDto` 재사용) 추가
- ✏️ `DashboardSnapshotDto` 구조 전면 교체: 기존 `snapshotTime/crowdDensities/risks/agents`
  → `marketId/marketName/mode/step/overallRiskScore/zones/agents/persistedRiskRows`
- ✏️ `SimulationEngineClient`에 `getSnapshot()` 메서드 추가 (`runScenario()`와 동일 패턴)
- ✏️ `DashboardController` `/snapshot`에 `marketId`(필수), `persistRisk`, `includeAgents`
  쿼리 파라미터 추가 (기존 `snapshotTime`은 `capturedAt`으로 이름 변경, SIM 계약과 일치)
- ⚠️ 아직 미완료: FE(`api/client.ts`, `types`, `DashboardPage`, `useSimulationData`)가
  여전히 `marketId`를 안 보내는 구계약 그대로임 — 별도 작업 필요
- ⚠️ 아직 미완료: BE를 실제로 띄워서 SIM과 통합 테스트 (이번 변경은 SIM 스키마와의
  정적 필드 대조만 완료, 실행 테스트는 재재님이 로컬에서 진행 예정)
- 참고: 기존 `CrowdDensityDto`/`RiskDto`는 이제 어디서도 참조되지 않는 미사용 코드가 됨
  (테이블 자체는 존재하므로 Acoustic/Radar 때처럼 강제 삭제 대상은 아니지만, 필요 없다면
  추후 정리 가능)

### 2026-07-23 (레이더/음향 센서 완전 제거)
- **결정**: 레이더 센서를 제거하기로 하면서, 지난번 "코드는 유지"로 남겨뒀던 음향 센서 관련
  코드도 이번에 함께 완전히 삭제함
- DB: `senradr01m`/`senradr01h`(레이더), `audevnt01m`/`audevnt01h`(음향, 기존 제거분) 테이블
  전부 스키마에서 제거. `comcode-seed.sql`의 `SENRD`/`SENAU` 공통코드도 삭제
- BE 엔티티/리포지토리 완전 삭제: `RadarReading`, `RadarReadingLog`, `AcousticEvent`,
  `AcousticEventLog` 및 각각의 Repository (총 8개 파일)
- **`ddl-auto`를 다시 `validate`로 복원** — 테이블 없는 엔티티가 전부 사라져서 지난번
  `none`으로 낮췄던 이유가 해소됨. 19개 테이블 전체 엔티티-스키마 불일치 조기 탐지 기능 복원
- `RiskBreakdownDto`(파이프라인 A)에서 `flow`/`acoustic` 필드 제거, `density`/`bottleneck`만 남음
- ⚠️ `RiskScoreDto.ContributingFactors`(파이프라인 B, 팀원 담당)도 SIM 쪽 스키마 변경에
  맞춰 `acoustic`/`flowRate` 제거 — **담당 팀원에게 꼭 공유 필요**
- SIM `risk.py`의 위험도 가중치도 density(0.55)/bottleneck(0.10) 2개만 남고, 기존 "결측 지표
  재정규화" 로직이 그대로 재사용되어 두 지표만으로 100% 배분됨

### 2026-07-23 (파이프라인 A)
- **`DashboardService`가 SIM `/simulate/snapshot`을 실제로 호출하도록 전면 재작성**
  - 기존에는 `crddnst01m`/`mrkrisk01m`을 DB에서 직접 읽었으나, 확인 결과 `mrkrisk01m`엔
    SIM이 `persistRisk=true`로 호출됐을 때만 값이 쌓이고 BE는 절대 쓰지 않았으며,
    `crddnst01m`은 실제 센서 장비가 없어 시딩 데이터 외엔 갱신 코드가 전혀 없었음
    (즉 "실시간 관제"가 아니라 마지막 수동 SIM 호출/시딩 시점의 잔여값을 보여주던 상태)
  - `SimulationEngineClient`에 `getSnapshot()` 추가 (`runScenario()`와 동일한 패턴)
  - `DashboardSnapshotDto`를 SIM `SnapshotResponse`와 1:1 일치하도록 재설계
    (`snapshotTime`/`crowdDensities`/`risks` 분리 구조 → `marketId`/`marketName`/`mode`/`step`/
    `overallRiskScore`/`zones`/`agents`/`persistedRiskRows`로 교체)
  - 신규 DTO: `SnapshotRequestDto`(`marketId` 필수 + `capturedAt`/`persistRisk`/`includeAgents`),
    `ZoneResultDto`, `RiskBreakdownDto`
  - `DashboardController` `/api/dashboard/snapshot`에 `marketId`(필수), `persistRisk`,
    `includeAgents` 쿼리 파라미터 추가
  - SIM 쪽 모의 테스트로 응답 필드 전체 일치 확인 완료. **BE 실행 후 실제 통합 테스트는 아직 안 함**
  - ⚠️ **FE는 아직 옛 계약(`snapshotTime`/`crowdDensities`/`risks`, `marketId` 미전송) 그대로임.**
    `api/client.ts`, `types/index.ts`(`DashboardSnapshot`), `DashboardPage.tsx`,
    `useSimulationData.ts`를 이 새 계약에 맞게 정렬하는 작업이 남아있음
  - `CrowdDensityRepository`/`RiskRepository`/`CrowdDensityDto`/`RiskDto`는 `DashboardService`에서
    더 이상 쓰지 않지만 코드는 그대로 둠 (다른 용도로 재사용 가능성, 엔티티-테이블 자체는 실존)

### 2026-07-23 (파이프라인 B)
- **파이프라인 B(BE↔SIM 시나리오) 계약 정렬**
  - `ScenarioRequestDto`: `marketId` 필드 추가(필수), `eventNodeId` → `eventZoneId`로 이름 변경 (SIM `ScenarioRequest`와 일치)
  - `AgentStateDto`: `nodeId` → `zoneId`로 명명 정정, `latitude`/`longitude` 필드 추가 (SIM `AgentState`와 1:1 일치)
  - SIM `/simulate/scenario` 응답이 `scenarioId`/`requestedAt`/스텝별 `frames`/`evacuationTimeSeconds`/`finalRiskScore`를 반환하도록 확장됨에 따라 BE `ScenarioResultDto`와 필드 구조 일치 확인 완료
  - BE→SIM 실제 통합 테스트 완료 (정상 응답 수신 확인)
- **음향 센서 데이터 사용 중단**
  - `audevnt01m`/`audevnt01h` 테이블을 `schema-init.sql`/`drop-all.sql`
  - `AcousticEvent`/`AcousticEventLog` 엔티티·리포지토리는 코드상 유지하되 어떤 서비스에서도 호출하지 않음(원래도 미사용 상태였음)
  - ⚠️ 위 엔티티가 가리키는 테이블이 사라졌기 때문에, `application-local.yml`/`application-prod.yml`의 `hibernate.ddl-auto`를 `validate`에서 `none`으로 변경 (validate 유지 시 부팅 실패). 이로 인해 19개 테이블 전체에 대한 "엔티티-스키마 불일치 조기 탐지" 기능이 함께 비활성화됨 — 추후 해당 엔티티를 완전히 정리하면 `validate`로 되돌리는 것을 권장

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
