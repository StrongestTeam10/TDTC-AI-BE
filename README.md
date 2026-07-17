# market-digital-twin-backend

전통시장 AI 안전탐지 관제 솔루션 - Spring Boot API 서버

## ⚠️ 로컬 환경 안내
이 컨테이너 환경은 Maven Central 접근이 차단되어 있어 `mvn` 빌드 검증을 하지 못했습니다.
로컬 또는 CI(Maven Central 접근 가능한 환경)에서 아래로 검증해주세요.

```bash
mvn clean package
```

Maven Wrapper(`mvnw`)가 없는 상태이므로, 필요하면 로컬에서 다음으로 생성하세요.
```bash
mvn -N wrapper:wrapper
```

## 아키텍처 상 역할
```
[React/S3]  →  [Spring Boot API (이 저장소)]  →  [FastAPI 시뮬레이션 엔진 (Mesa, 별도 저장소)]
                        │                                    │
                        └──────────────► [PostgreSQL] ◄──────┘
```
Spring Boot는 Mesa 시뮬레이션을 직접 실행하지 않고, `SimulationEngineClient`를 통해
별도의 FastAPI 마이크로서비스(`market-digital-twin` 저장소를 FastAPI로 감싼 것)를 호출합니다.

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
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

## API 문서
로컬 실행 후 http://localhost:8080/swagger-ui.html

## TODO (다음 단계에서 구체화 필요)
- [ ] LidarReading, AcousticEvent 등 나머지 엔티티/리포지토리 추가 (현재 CctvDetection, AlertLog, SpatialNode만 구현)
- [ ] DashboardService의 acousticScore/flowRateScore 계산 로직 연결
- [ ] getAvailableTimestamps() 실제 쿼리 구현
- [ ] Spring Security 인증/인가 (현재 미적용 — B2G 요건상 필수 검토)
- [ ] DB 마이그레이션 도구(Flyway/Liquibase) 도입 여부 결정
