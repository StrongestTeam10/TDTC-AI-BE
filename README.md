# market-digital-twin-backend

전통시장 AI 안전탐지 관제 솔루션 - Spring Boot API 서버 (Gradle)


> 변경 이력은 [CHANGELOG.md](./CHANGELOG.md) 참고

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

## 정책 보고서 생성 (2026-07-31 추가)

시뮬레이션 결과를 그 시장의 **현행안**과 비교한 정책 보고서(DOCX)를 만듭니다.

```text
POST /api/simulation/reports  { "scenarioId": 47 }
    ↓
BE가 scenarioId의 market_id로 같은 시장의 현행안을 찾아
시장·구역·시나리오·결과를 JSON 한 덩어리로 조립
    ↓
SIM POST /simulation/reports/file  (RAG 검색 + LLM 본문 생성, 1~3분)
    ↓
DOCX 바이트 + X-Report-Title 헤더 수신
    ↓
S3 업로드 → simrslt01d에 경로·제목 기록 → presigned URL 반환
```

| 엔드포인트 | 용도 |
|---|---|
| `POST /api/simulation/reports` | 보고서 생성. 응답에 `downloadUrl` 포함 |
| `GET /api/simulation/reports/{scenarioId}/download` | 만료된 다운로드 URL 재발급 |
| `GET /api/simulation/scenarios/my` | 내가 실행한 시뮬레이션 이력 (보고서 유무 포함) |

설계상 정해둔 것:

- **SIM은 DB를 직접 보지 않습니다.** BE가 필요한 행을 전부 읽어 JSON으로 실어 보냅니다.
- **보고서 1건 = 현행안 1개 vs 시나리오 1개.** SIM은 대안 N개를 받을 수 있지만 BE가 1개로
  제한합니다. `simrslt01d.generated_report_path`가 단일 컬럼이라 한 시나리오가 여러 보고서에
  속하면 이력을 표현할 수 없습니다.
- **현행안 결과를 `agent_count`로 좁히지 않습니다.** 야시장 개장처럼 정책이 방문객 수를
  늘리는 경우가 정상이기 때문입니다. 대신 보고서에 시나리오별 투입 인구를 표시하고,
  인구수가 다르면 분석 가정에 주의 문구를 자동으로 넣습니다.
- **다운로드는 presigned URL(유효 10분)로 브라우저가 S3에 직접 접근합니다.**
  302 리다이렉트 대신 JSON으로 URL을 돌려주는 이유는 `ReportController`의 주석 참고
  (브라우저 이동 시 Authorization 헤더 누락 / fetch는 S3 CORS에 막힘).
- 재발급은 S3 키만 다시 서명하는 것이라 보고서를 다시 만들지 않습니다
  (재생성은 RAG + LLM까지 다시 도는 수 분짜리 작업입니다).

## 폴더 구조
- `config/` : CORS, WebClient(FastAPI 호출용) 설정
- `controller/` : REST API 엔드포인트 (프론트엔드 `src/api/client.ts`와 계약 일치)
- `service/` : 비즈니스 로직 (위험도 스코어링 등)
- `client/` : 외부(FastAPI 시뮬레이션 엔진) 호출 클라이언트
- `domain/entity/` : JPA 엔티티 (16개 테이블 스키마 중 대시보드용 일부 우선 구현)
- `repository/` : Spring Data JPA 리포지토리
- `dto/request`, `dto/response/` : API 요청/응답 DTO (프론트엔드 타입과 필드명 일치 필수)
- `exception/` : 전역 예외 처리
- `security/` : JWT 필터, 현재 로그인 사용자 조회(`CurrentUserProvider`)
- `scheduler/` : 정기 실행 작업 (상점 매력도 갱신)

## 환경변수 (운영, `application-prod.yml`)
> 2026-08-06 정정: 아래 변수명을 `DB_URL` / `SIMULATION_ENGINE_URL` / `FRONTEND_ORIGIN`
> 으로 적어뒀었는데 실제 `application-prod.yml`이 읽는 이름과 달랐습니다.
> 운영 프로필은 전부 `PRO_` 접두사를 씁니다.

`SPRING_PROFILES_ACTIVE=prod` 를 반드시 함께 설정해야 합니다. 빠뜨리면
`application.yml`의 기본값인 `local` 프로필로 뜹니다.

아래는 모두 **기본값이 없어** 하나라도 비면 기동에 실패합니다.

- `PRO_DB_URL`, `PRO_DB_USERNAME`, `PRO_DB_PASSWORD` : RDS PostgreSQL 접속 정보
- `PRO_SIMULATION_ENGINE_URL` : FastAPI 시뮬레이션 엔진 내부 주소 (VPC 내부, 외부 미노출 권장)
  - `WebClientConfig`가 `${simulation-engine.base-url}`로 기본값 없이 주입받음
- `PRO_FRONTEND_ORIGIN` : FE 오리진 (CORS 허용). CloudFront 적용 전에는 S3 정적 호스팅 주소
- `JWT_SECRET` : HS256 서명 키. 32바이트(256비트) 이상
- `AWS_S3_BUCKET` : 게시판 첨부파일/보고서 저장용 S3 (2026-07-24 추가). 자격증명은
  환경변수로 직접 넣지 않고 EC2/ECS 인스턴스 role의 기본 자격증명 체인을 사용

기본값이 있어 생략 가능한 것:
- `AWS_REGION` (기본 `ap-northeast-2`), `JWT_EXPIRATION_MS` (기본 1시간)
- `AWS_S3_REPORT_BUCKET` : 보고서 DOCX 저장용 S3 (2026-07-31 추가). **생략하면
  `AWS_S3_BUCKET`으로 폴백**됩니다. 2026-08-04에 버킷을 하나(`tdtc-ai-report`)로
  통합하기로 해서 지금은 폴백이 의도된 동작입니다.

## 환경변수 (로컬, `application-local.yml`)
- `DEV_DB_URL`, `DEV_DB_USERNAME`, `DEV_DB_PASSWORD` : 개발 DB 접속 정보 (기본값 없음)

> 2026-08-06 변경: `DEV_DB_URL`에 있던 Supabase 호스트 하드코딩 기본값을 제거했습니다.
> 기본값이 있으면 `SPRING_PROFILES_ACTIVE`를 빠뜨린 컨테이너가 조용히 원격 DB로
> 붙어버려서(RDS 전환 후에는 새 RDS가 아니라 옛 DB에 쓰게 됨) 알아채기 어렵습니다.
> 이제 설정을 빠뜨리면 기동 단계에서 바로 실패합니다.
> 로컬 실행 시 세 변수를 모두 설정하세요 (PowerShell 예: `$env:DEV_DB_URL="jdbc:postgresql://..."`).

## 게시판 첨부파일 S3 버킷 설정 (2026-07-24 추가, 필수)
파일 업로드(`PutObject`)는 BE 서버가 직접 호출하지만, **다운로드는 presigned URL로
브라우저(FE)가 S3에 직접 접근**하는 구조라 버킷에 CORS 설정이 없으면 FE에서
"파일 다운로드에 실패했습니다" 오류가 납니다. S3 콘솔 > 버킷 > 권한 > CORS에 아래
내용을 등록해주세요 (로컬 개발은 `http://localhost:5173`, 배포 후에는 실제 CloudFront
도메인도 함께 추가):

```json
[
  {
    "AllowedOrigins": ["http://localhost:5173", "https://<실제 CloudFront 도메인>"],
    "AllowedMethods": ["GET"],
    "AllowedHeaders": ["*"],
    "ExposeHeaders": ["Content-Disposition"],
    "MaxAgeSeconds": 3000
  }
]
```
버킷 자체는 퍼블릭 액세스 차단(Block Public Access)을 켜둔 상태 그대로 두면 됩니다 -
presigned URL은 서명된 임시 링크라 버킷이 비공개여도 접근 가능합니다.

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
- [ ] 테스트 코드 — `src/test`가 아직 없음

## ⚠️ ERD 대비 구현 시 의도적으로 조정한 부분
- `COMCODE01M.desc` → `code_desc`로 변경 (DESC는 SQL 예약어라 컬럼명 충돌 위험 방지)
- `AUDEVNT01M/H.confidence` → ERD엔 `DECIMAL(1,2)`로 표기되어 있었으나 이는 정밀도(1)보다 소수자릿수(2)가 커서 수학적으로 불가능한 값이라 `DECIMAL(3,2)`(0.00~1.00 범위)로 보정 (2026-07-23: 이후 이 두 테이블 자체를 제거함 — 아래 참고)
- `SENLIDR01M/H`, `SENRADR01M/H`의 PK 컬럼명이 ERD상 `crowd_density_id`/`crowd_density_sq`로 되어 있어 `CRDDNST01M`과 이름이 겹치지만, 실제로는 각 테이블 고유의 식별자이므로 그대로 유지(단, Java 필드명은 `lidarReadingId`/`radarReadingId` 등으로 명확화)
- `AUDEVNT01M/H`(음향 이벤트 테이블) 자체를 2026-07-23부로 DB에서 제거함 (음향 센서 데이터 사용 중단 결정). `AcousticEvent`/`AcousticEventLog` 엔티티 코드는 남아있으나 대응 테이블이 없으므로 실사용 금지
