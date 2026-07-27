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
- `AWS_REGION`, `AWS_S3_BUCKET` : 게시판 첨부파일 저장용 S3 (2026-07-24 추가). 자격증명은
  환경변수로 직접 넣지 않고 EC2/ECS 인스턴스 role의 기본 자격증명 체인을 사용

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

## ⚠️ ERD 대비 구현 시 의도적으로 조정한 부분
- `COMCODE01M.desc` → `code_desc`로 변경 (DESC는 SQL 예약어라 컬럼명 충돌 위험 방지)
- `AUDEVNT01M/H.confidence` → ERD엔 `DECIMAL(1,2)`로 표기되어 있었으나 이는 정밀도(1)보다 소수자릿수(2)가 커서 수학적으로 불가능한 값이라 `DECIMAL(3,2)`(0.00~1.00 범위)로 보정 (2026-07-23: 이후 이 두 테이블 자체를 제거함 — 아래 참고)
- `SENLIDR01M/H`, `SENRADR01M/H`의 PK 컬럼명이 ERD상 `crowd_density_id`/`crowd_density_sq`로 되어 있어 `CRDDNST01M`과 이름이 겹치지만, 실제로는 각 테이블 고유의 식별자이므로 그대로 유지(단, Java 필드명은 `lidarReadingId`/`radarReadingId` 등으로 명확화)
- `AUDEVNT01M/H`(음향 이벤트 테이블) 자체를 2026-07-23부로 DB에서 제거함 (음향 센서 데이터 사용 중단 결정). `AcousticEvent`/`AcousticEventLog` 엔티티 코드는 남아있으나 대응 테이블이 없으므로 실사용 금지
