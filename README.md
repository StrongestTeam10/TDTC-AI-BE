# market-digital-twin-backend

전통시장 AI 안전탐지 관제 솔루션 - Spring Boot API 서버 (Gradle)

## 변경 이력

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

## ⚠️ 로컬 환경 안내
이 컨테이너 환경은 Gradle/Maven Central 접근이 차단되어 있어 실제 빌드 검증을 하지 못했습니다.
로컬 또는 CI(외부 저장소 접근 가능한 환경)에서 아래로 먼저 검증해주세요.

```bash
gradle wrapper          # Gradle Wrapper 생성 (gradlew, gradlew.bat 생성됨) - 최초 1회
./gradlew clean build
```

Wrapper를 생성해두면 이후에는 로컬에 Gradle이 설치되어 있지 않아도 `./gradlew`만으로 빌드/실행이 가능합니다.
생성 후에는 `gradle/wrapper/`, `gradlew`, `gradlew.bat`을 반드시 git에 커밋하세요 (원래 Gradle 프로젝트의 표준 관례입니다).

## 아키텍처 상 역할
```
[React/S3]  →  [Spring Boot API (이 저장소)]  →  [FastAPI 시뮬레이션 엔진 (Mesa, 별도 저장소)]
                        │                                    │
                        └──────────────► [PostgreSQL] ◄──────┘
```
Spring Boot는 Mesa 시뮬레이션을 직접 실행하지 않고, `SimulationEngineClient`를 통해
별도의 FastAPI 마이크로서비스를 호출합니다.

## 폴더 구조
- `config/` : CORS, WebClient(FastAPI 호출용) 설정
- `controller/` : REST API 엔드포인트 (프론트엔드 `src/api/client.ts`와 계약 일치)
- `service/` : 비즈니스 로직 (위험도 스코어링 등)
- `client/` : 외부(FastAPI 시뮬레이션 엔진) 호출 클라이언트
- `domain/entity/` : JPA 엔티티 (16개 테이블 스키마 중 대시보드용 일부 우선 구현)
- `repository/` : Spring Data JPA 리포지토리
- `dto/request`, `dto/response/` : API 요청/응답 DTO (프론트엔드 타입과 필드명 일치 필수)
- `exception/` : 전역 예외 처리

## 환경변수 (운영, `application-prod.yml`)
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` : RDS PostgreSQL 접속 정보
- `SIMULATION_ENGINE_URL` : FastAPI 시뮬레이션 엔진 내부 주소 (VPC 내부, 외부 미노출 권장)
- `FRONTEND_ORIGIN` : CloudFront 도메인 (CORS 허용)

## 로컬 실행
```bash
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
./gradlew bootRun --args='--spring.profiles.active=local'
```

## API 문서
로컬 실행 후 http://localhost:8080/swagger-ui.html

## TODO (다음 단계에서 구체화 필요)
- [x] 실제 ERD 기준으로 엔티티/DTO 교체 완료 (Market, Zone, CrowdDensity, Risk)
- [x] 나머지 14개 테이블 엔티티/리포지토리 추가 완료 (User, EntityChangeLog, Scenario, ScenarioResult, Facility, CrowdDensityLog, Sensor, AcousticEvent/Log, LidarReading/Log, RadarReading/Log, CommonCode) — 18개 테이블 전체 완료. Service/Controller/DTO는 필요한 것부터 추가 예정
- [ ] ⚠️ **구역 간 연결(그래프) 구조가 ERD에 없음** — Mesa NetworkGrid에 필요한 인접성 정보를 폴리곤 좌표로 런타임 계산할지, 스키마에 컬럼을 추가할지 결정 필요
- [ ] `gradle wrapper` 실행 후 wrapper 파일 커밋 (이 저장소는 wrapper 미포함 상태)
- [ ] Spring Security 인증/인가 (현재 미적용 — B2G 요건상 필수 검토, `USRUSRS01M` 테이블과 연동)
- [ ] DB 마이그레이션 도구(Flyway/Liquibase) 도입 여부 결정 — SQL Editor 수동 실행 방식은 임시 조치

## ⚠️ ERD 대비 구현 시 의도적으로 조정한 부분
- `COMCODE01M.desc` → `code_desc`로 변경 (DESC는 SQL 예약어라 컬럼명 충돌 위험 방지)
- `AUDEVNT01M/H.confidence` → ERD엔 `DECIMAL(1,2)`로 표기되어 있었으나 이는 정밀도(1)보다 소수자릿수(2)가 커서 수학적으로 불가능한 값이라 `DECIMAL(3,2)`(0.00~1.00 범위)로 보정 (2026-07-23: 이후 이 두 테이블 자체를 제거함 — 아래 참고)
- `SENLIDR01M/H`, `SENRADR01M/H`의 PK 컬럼명이 ERD상 `crowd_density_id`/`crowd_density_sq`로 되어 있어 `CRDDNST01M`과 이름이 겹치지만, 실제로는 각 테이블 고유의 식별자이므로 그대로 유지(단, Java 필드명은 `lidarReadingId`/`radarReadingId` 등으로 명확화)
- `AUDEVNT01M/H`(음향 이벤트 테이블) 자체를 2026-07-23부로 DB에서 제거함 (음향 센서 데이터 사용 중단 결정). `AcousticEvent`/`AcousticEventLog` 엔티티 코드는 남아있으나 대응 테이블이 없으므로 실사용 금지
