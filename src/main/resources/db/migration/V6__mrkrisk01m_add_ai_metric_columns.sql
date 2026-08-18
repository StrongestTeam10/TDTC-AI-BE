-- =========================================================================
-- V6 — mrkrisk01m에 AI 분석 지표 컬럼 추가 (2026-08-18)
--
-- CCTV AI 서버(ai_server.py)가 프레임마다 산출하는 지표 중 지금까지 DB에
-- 저장하지 못하던 두 가지를 받는다.
--   occupancy_rate : 밀집도(%) - 0.0 ~ 100.0
--   stagnation_sec : 정체 시간(초) - 현재 AI 쪽은 0 고정으로 보내고 있고,
--                    정체 시간 산출이 켜지면 실수 초 단위 값이 들어온다.
--
-- 둘 다 NULL 허용이다. 이미 쌓여 있는 위험도 이력에는 이 값이 없고, 파이썬
-- 적재 경로도 아직 이 컬럼을 채우지 않는다(백엔드 Risk 엔티티가 먼저 받아둔
-- 상태). 값이 없는 행과 있는 행이 섞이는 게 정상이라 DEFAULT도 두지 않는다.
--
-- 타입을 REAL로 잡은 이유: 같은 테이블의 risk_score가 REAL이고, 엔티티도
-- Float으로 받는다(Hibernate validate가 float4를 기대한다).
--
-- ⚠️ IF NOT EXISTS를 쓰는 이유: 공용 개발 DB에는 이 컬럼 2개를 SQL 편집기에서
--    손으로 먼저 추가해둔 상태다. 그 DB에서는 이 마이그레이션이 아무것도 하지
--    않고 지나가면서 flyway_schema_history에 V6 적용 기록만 남는다. 빈 DB나
--    아직 손대지 않은 DB에서는 실제로 컬럼이 생긴다. 두 경우 모두 같은 스키마에
--    도달한다.
-- =========================================================================

ALTER TABLE mrkrisk01m ADD COLUMN IF NOT EXISTS occupancy_rate REAL;
ALTER TABLE mrkrisk01m ADD COLUMN IF NOT EXISTS stagnation_sec REAL;

COMMENT ON COLUMN mrkrisk01m.occupancy_rate IS
    'CCTV AI가 산출한 구역 밀집도(%). 0.0~100.0, 값이 없으면 NULL.';
COMMENT ON COLUMN mrkrisk01m.stagnation_sec IS
    'CCTV AI가 산출한 정체 시간(초). 정체 산출이 꺼져 있는 동안은 0 또는 NULL.';

-- -------------------------------------------------------------------------
-- 함께 맞추는 기존 어긋남: video_url
--
-- Risk 엔티티에는 @Column(name = "video_url", length = 1000)이 있는데, 이
-- 컬럼을 만드는 마이그레이션이 저장소에 없다(V1의 mrkrisk01m 정의에도 없다).
-- Flyway 도입 전에 운영/개발 DB에 손으로 추가된 컬럼이라 지금 쓰는 DB에서는
-- 멀쩡히 동작하지만, 빈 DB로 처음 띄우는 사람은 ddl-auto: validate에 걸려
-- "missing column [video_url]"로 기동이 실패한다.
--
-- 이번에 컬럼을 추가하는 김에 같이 메운다. 기존 DB에서는 IF NOT EXISTS로
-- 그냥 지나간다.
-- (이 블록만 빼고 싶으면 아래 두 문장만 지우면 된다. 위 두 컬럼과는 독립적이다.)
-- -------------------------------------------------------------------------

ALTER TABLE mrkrisk01m ADD COLUMN IF NOT EXISTS video_url VARCHAR(1000);

COMMENT ON COLUMN mrkrisk01m.video_url IS
    '해당 위험도가 산출된 CCTV 클립 S3 URL. Flyway 도입 전 수동 추가된 컬럼을 V6에서 정식 반영.';
