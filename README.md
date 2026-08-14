# TDTC-AI-BE

전통시장 AI 안전탐지 관제 솔루션 — Spring Boot API 서버

Java 21 · Spring Boot 3.3 · Spring Security(JWT) · JPA · Gradle · PostgreSQL

> 변경 이력은 [CHANGELOG.md](./CHANGELOG.md) 참고

---

## 빠른 시작

```bash
# 로컬 프로필은 DB 접속 정보 3개가 없으면 기동에 실패합니다 (기본값 없음)
export DEV_DB_URL="jdbc:postgresql://<host>:5432/postgres"
export DEV_DB_USERNAME="<user>"
export DEV_DB_PASSWORD="<password>"

./gradlew bootRun --args='--spring.profiles.active=local'
```

| 명령 | 설명 |
|---|---|
| `./gradlew bootRun` | 실행 (프로필 미지정 시 `local`) |
| `./gradlew build` | 빌드 + 테스트 |
| `./gradlew compileJava` | 컴파일만 |
| `./gradlew test` | 테스트만 |

API 문서: 기동 후 http://localhost:8080/swagger-ui.html

> PowerShell은 `$env:DEV_DB_URL="..."` 형식을 씁니다.

---

## 아키텍처 상 역할

BE는 시뮬레이션을 직접 돌리지 않습니다. **DB를 읽어 요청을 조립하고 SIM에 넘긴 뒤 결과를 저장**하는 것이 역할입니다.

```text
[TDTC-AI-FE]  ──►  [TDTC-AI-BE (이 저장소)]  ──►  [TDTC-AI-SIM]  FastAPI + Mesa
  React/S3            Spring Boot :8080            시뮬레이션·보고서 생성
                            │
                            ├──►  [PostgreSQL]  Supabase / RDS
                            └──►  [S3]  게시판 첨부파일 · 보고서 DOCX

[TDTC-AI-CCTV]  ──►  DB 적재  ──►  BE가 읽어서 FE 관제 화면에 제공
  영상 AI 파이프라인
```

- SIM 호출은 `client/SimulationEngineClient`(WebClient)가 담당합니다.
- **SIM은 DB를 직접 보지 않습니다.** BE가 필요한 행을 전부 읽어 JSON으로 실어 보냅니다.
- CCTV 파이프라인이 적재한 데이터(보행자 좌표·알람·영상 클립)는 BE가 읽기 전용으로 서빙합니다.

---

## API 한눈에

| 경로 | 담당 | 접근 권한 |
|---|---|---|
| `/api/auth/**` | 로그인 · 회원가입 · 본인확인 · 비밀번호 재설정 · `/me` | 인증 불필요 (`/me` 제외) |
| `/api/common-codes` | 공통코드 조회 (ROL·ORG·LVL·POL·MKT·BCT) | 인증 불필요 |
| `/api/markets/**` | 시장 · 구역 · 통로 · 게이트 · 건물 | 로그인 |
| `/api/dashboard/**` | 관제 스냅샷 | **ROL01 · ROL02** |
| `/api/simulation/**` | 시뮬레이션 실행 · 이력 · 정책 보고서 | **ROL01 · ROL02** |
| `/api/policy/analyze` | 공문 LLM 분석 (SIM 경유) | 로그인 |
| `/api/facilities/**` | 상점 · 출입구 · 외관 사진 | 로그인 (BE가 역할 재검증) |
| `/api/cctv-zones` · `/api/market-objects` | CCTV 구역 · 시장 오브젝트 등록 | 로그인 |
| `/api/posts/**` | 게시판 | 로그인 |
| `/api/admin/users/**` | 회원관리 · 승인 | **ROL01** |
| `/api/ai/**` · `/api/v1/**` | CCTV 파이프라인 적재 데이터 조회 | 로그인 |

권한 코드: `ROL01` 관리자 · `ROL02` 관제요원 · `ROL03` 조회자
소속 코드: `ORGKT` KT · `ORGGV` 지자체 · `ORGMA` 상인회

**인가는 두 겹입니다.** FE가 메뉴·라우트를 가리고, BE가 `SecurityConfig`에서 최종 차단합니다. FE 판정을 신뢰하지 않습니다.

### 게시글 시장 지정 (2026-08-12)

`marketCode`는 **관리자만** 지정할 수 있습니다. 그 외 권한이 값을 보내도 무시하고 작성자의 담당 시장을 씁니다(`PostService.resolveMarketCode`). 빈 문자열은 "전체"(`null`)로, 모든 시장에서 보입니다.

---

## 정책 보고서 생성

시뮬레이션 결과를 그 시장의 **현행안**과 비교한 정책 보고서(DOCX)를 만듭니다.

```text
POST /api/simulation/reports  { "scenarioId": 47 }
    ↓  BE가 같은 시장의 현행안을 찾아 시장·구역·시나리오·결과를 JSON으로 조립
SIM POST /simulation/reports/file      (RAG 검색 + LLM 본문 생성, 1~3분)
    ↓  DOCX 바이트 + X-Report-Title 헤더 수신
S3 업로드 → simrslt01d에 경로·제목 기록 → presigned URL 반환
```

| 엔드포인트 | 용도 |
|---|---|
| `POST /api/simulation/reports` | 보고서 생성. 응답에 `downloadUrl` 포함 |
| `GET /api/simulation/reports/{scenarioId}/download` | 만료된 다운로드 URL 재발급 |
| `GET /api/simulation/scenarios/my` · `/scenarios` | 실행 이력 (본인 / 전체·관리자) |

설계상 정해둔 것:

- **보고서 1건 = 현행안 1개 vs 시나리오 1개.** SIM은 대안 N개를 받을 수 있지만 BE가 1개로 제한합니다. `simrslt01d.generated_report_path`가 단일 컬럼이라 한 시나리오가 여러 보고서에 속하면 이력을 표현할 수 없습니다.
- **현행안 결과를 `agent_count`로 좁히지 않습니다.** 야시장 개장처럼 정책이 방문객 수를 늘리는 경우가 정상이기 때문입니다. 대신 인구수가 다르면 보고서에 주의 문구를 자동으로 넣습니다.
- **다운로드는 presigned URL(10분)로 브라우저가 S3에 직접 접근합니다.** 302 리다이렉트 대신 JSON으로 URL을 주는 이유는 `ReportController` 주석 참고(브라우저 이동 시 Authorization 헤더 누락 / fetch는 S3 CORS에 막힘).
- **재발급은 S3 키만 다시 서명합니다.** 보고서를 다시 만들지 않습니다(재생성은 RAG + LLM까지 다시 도는 수 분짜리 작업).

---

## 폴더 구조

```text
src/main/java/com/markettwin/backend/
├─ controller/   REST 엔드포인트 (FE src/api/client.ts와 계약 일치)
├─ service/      비즈니스 로직 · 권한 재검증
├─ client/       SIM(FastAPI) 호출 WebClient
├─ domain/entity/  JPA 엔티티 (25개)
├─ repository/   Spring Data JPA + 네이티브 쿼리(ReportQueryRepository)
├─ dto/          request / response — FE 타입과 필드명 일치 필수
├─ security/     JWT 필터 · CurrentUserProvider
├─ config/       SecurityConfig(CORS·인가) · WebClientConfig · S3Config
├─ exception/    전역 예외 처리
└─ scheduler/    정기 작업 (상점 매력도 갱신)
```

---

## 환경변수

### 운영 (`application-prod.yml`)

`SPRING_PROFILES_ACTIVE=prod`를 **반드시 함께** 설정하세요. 빠뜨리면 `local` 프로필로 뜹니다.

기본값이 없어 하나라도 비면 **기동 실패**:

| 변수 | 설명 |
|---|---|
| `PRO_DB_URL` `PRO_DB_USERNAME` `PRO_DB_PASSWORD` | RDS PostgreSQL |
| `PRO_SIMULATION_ENGINE_URL` | SIM 내부 주소 (VPC 내부, 외부 미노출 권장) |
| `PRO_FRONTEND_ORIGIN` | FE 오리진 (CORS 허용) |
| `JWT_SECRET` | HS256 서명 키. 32바이트 이상 |
| `AWS_S3_BUCKET` | 첨부파일·보고서 저장용 S3 |

생략 가능:

| 변수 | 기본값 |
|---|---|
| `AWS_REGION` | `ap-northeast-2` |
| `JWT_EXPIRATION_MS` | 1시간 |
| `AWS_S3_REPORT_BUCKET` | **`AWS_S3_BUCKET`으로 폴백** (버킷을 하나로 통합해서 의도된 동작) |

> AWS 자격증명은 환경변수로 직접 넣지 않고 인스턴스 role의 **기본 자격증명 체인**을 씁니다.

### 로컬 (`application-local.yml`)

`DEV_DB_URL` · `DEV_DB_USERNAME` · `DEV_DB_PASSWORD` — 기본값 없음.

> Supabase 호스트 하드코딩 기본값을 제거했습니다(2026-08-06). 기본값이 있으면 `SPRING_PROFILES_ACTIVE`를 빠뜨린 컨테이너가 조용히 원격 DB에 붙어버려 알아채기 어렵습니다.

**로컬 AWS 자격증명** — S3 업로드(게시판 첨부·보고서)를 쓰려면 필요합니다. `~/.aws/credentials`의 프로필 이름이 `default`가 아니면 `AWS_PROFILE`을 지정하세요.

```bash
export AWS_PROFILE=tdtc-ai
```

없으면 첨부파일 업로드에서 `Unable to load credentials from any of the providers in the chain`으로 500이 납니다.

### 로컬 CORS (2026-08-12)

`cors.allowed-origins: http://localhost:[*]` — 포트를 열어뒀습니다.

Vite dev 서버는 5173이 사용 중이면 조용히 5174, 5175로 올려 잡는데, 포트를 고정해두면 그때부터 API가 전부 preflight 403으로 막힙니다(**화면은 뜨는데 로그인부터 실패**). `SecurityConfig`가 설정값에 `*`가 든 항목만 `setAllowedOriginPatterns`로 넘깁니다.

> ⚠️ `@Value`의 `String[]` 바인딩이 쉼표로 자르므로 `:[5173,5174]` 같은 포트 목록 문법은 쓸 수 없습니다. `:[*]`를 쓰세요.
> ⚠️ 운영은 정확 일치를 그대로 씁니다. `setAllowedOrigins`는 값이 `*`이면서 `allowCredentials(true)`일 때 Spring이 예외로 막아주는데, 전부 패턴으로 넘기면 그 안전장치가 사라집니다.

---

## S3 버킷 CORS 설정 (필수)

업로드는 BE가 직접 하지만 **다운로드는 presigned URL로 브라우저가 S3에 직접 접근**합니다. 버킷에 CORS가 없으면 FE에서 "파일 다운로드에 실패했습니다"가 납니다.

S3 콘솔 > 버킷 > 권한 > CORS:

```json
[
  {
    "AllowedOrigins": ["http://localhost:5173", "https://<CloudFront 도메인>"],
    "AllowedMethods": ["GET"],
    "AllowedHeaders": ["*"],
    "ExposeHeaders": ["Content-Disposition"],
    "MaxAgeSeconds": 3000
  }
]
```

버킷의 퍼블릭 액세스 차단은 켜둔 채로 두면 됩니다 — presigned URL은 서명된 임시 링크라 비공개 버킷에서도 동작합니다.

---

## 알아둘 것

### 정책 유형(`policy_type_code`)은 코드 하나만 저장됩니다

`simscnr01m.policy_type_code`가 `VARCHAR(5)` 한 칸이라, 화재와 통로폐쇄를 함께 실행해도 우선순위상 **하나만 남습니다**. 오브젝트·게이트만 배치한 실행은 `POLNO`(없음)로 남습니다.

**고치지 않기로 했습니다.** 실행 요청 원본이 `simscnr01m.virtual_config`에 JSON으로 통째로 남아 있어 정보가 사라지지 않고, 보고서도 그 원본을 씁니다. FE도 이 값을 표시하지 않습니다(2026-08-12에 시나리오 이력의 정책 유형 열·검색을 제거). 현재 이 값은 `ScenarioDisplayNameResolver`가 시나리오 이름을 조립할 때만 쓰입니다.

### DB 마이그레이션 (Flyway)

스키마 변경은 **Flyway가 기동 시 자동 적용**합니다. `ddl-auto: validate`는 그대로 두므로, Flyway가 스키마를 맞춘 뒤 Hibernate가 엔티티와 대조하는 순서가 됩니다.

```text
src/main/resources/db/
├─ migration/
│  ├─ V1__baseline_schema.sql                      기준 스키마 (2026-08-13 시점 전체)
│  ├─ V2__add_user_phone_number_and_is_duty.sql    당직자 SMS용 사용자 컬럼 추가
│  └─ V3__vdoclip01m_factor_id_drop_not_null.sql   클립의 factor_id NOT NULL 해제
└─ legacy/                                         Flyway 도입 전 수동 실행하던 파일 (참고용, 실행 안 됨)
```

**새 DDL을 추가할 때**

```text
src/main/resources/db/migration/V4__add_xxx_column.sql
```

버전 번호를 올려 파일을 추가하기만 하면 됩니다. 다음 기동에서 아직 적용되지 않은 것만 순서대로 실행되고 `flyway_schema_history`에 기록됩니다.

| 규칙 | 이유 |
|---|---|
| **적용된 파일은 절대 수정하지 않는다** | 체크섬이 달라져 기동을 거부합니다. 고칠 게 있으면 새 버전을 추가하세요 |
| 한 파일에 한 가지 변경 | 실패 지점을 좁히기 쉽습니다 |
| 되도록 멱등하게 (`IF NOT EXISTS` 등) | 중간 실패 후 재시도가 안전해집니다 |

**기존 DB에 어떻게 붙였나**

운영 RDS·개발 DB에는 이미 테이블이 다 있는 상태에서 Flyway를 도입했습니다. 그래서 `baseline-on-migrate: true` / `baseline-version: 1`로 두어,

- **기존 DB** → V1을 "적용 완료"로 표시만 하고 건너뜀 → V2부터 적용
- **빈 DB** → V1부터 전부 실행

두 경우 모두 같은 최종 스키마에 도달합니다.

**V1은 기존 DB에서 절대 실행되지 않습니다.** 전체 스키마를 새로 만드는 파일이라 그대로 돌면 "이미 존재한다"로 실패하기 때문입니다. 반면 V2 이후는 기존 DB에도 그대로 적용됩니다. 그래서 Flyway를 처음 도입한 배포에서 기존 운영 RDS·개발 DB에 실제로 실행된 DDL은 **V2와 V3**이고, 여기에 `flyway_schema_history` 테이블이 새로 생겼습니다.

바꿔 말하면, **기존 DB에 반영하고 싶은 변경은 반드시 V2 이후의 새 파일로 넣어야 합니다.** V1을 고쳐도 기존 DB에는 아무 일도 일어나지 않고, 체크섬만 달라져 기동이 거부됩니다.

**시드 데이터는 Flyway가 관리하지 않습니다**

| 파일 | 이유 |
|---|---|
| `comcode-seed.sql` | `DELETE` 후 재삽입이라 멱등하지만(참조하는 FK 없음), 운영에 손으로 추가한 코드가 있으면 지워집니다 |
| `seed-market-data.sql` | `ON CONFLICT`가 없는 순수 `INSERT`라 **재실행하면 데이터가 중복**됩니다 |

둘 다 DB를 새로 만들 때만 한 번 실행하세요. 공통코드를 배포마다 동기화하고 싶으면 `R__comcode_seed.sql`(반복 마이그레이션)로 옮길 수 있지만, 위의 "손으로 추가한 코드가 지워지는" 문제를 먼저 정리해야 합니다.

### 테스트

`src/test/java`에 보고서·시나리오 관련 단위 테스트 5개가 있습니다. 컨트롤러·통합 테스트는 아직 없습니다.

---

## ERD 대비 의도적으로 조정한 부분

- `COMCODE01M.desc` → `code_desc` (DESC는 SQL 예약어)
- `SENLIDR01M/H`, `SENRADR01M/H`의 PK 컬럼명이 ERD상 `crowd_density_id`로 `CRDDNST01M`과 겹치지만 각 테이블 고유 식별자라 그대로 두고, Java 필드명만 명확히 함
- **음향 센서 계열은 전부 제거됨** (2026-07-23 결정) — `AUDEVNT01M/H` 테이블, 관련 엔티티, 시뮬레이션의 `acoustic_anomaly` 이벤트까지 모두 삭제. 이벤트 타입은 현재 `fire` 하나뿐입니다.

---

## 관련 저장소

| 저장소 | 역할 |
|---|---|
| [TDTC-AI-FE](https://github.com/StrongestTeam10) | React 관제 화면 |
| [TDTC-AI-SIM](https://github.com/StrongestTeam10) | FastAPI + Mesa 시뮬레이션 · 보고서 생성 |
| [TDTC-AI-CCTV](https://github.com/StrongestTeam10) | CCTV 영상 AI 파이프라인 |
| [TDTC-AI-INFRA](https://github.com/StrongestTeam10) | Terraform 인프라 |
