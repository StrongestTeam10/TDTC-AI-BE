# market-digital-twin-backend

전통시장 AI 안전탐지 관제 솔루션 - Spring Boot API 서버 (Gradle)

## 변경 이력

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
