-- 2026-08-16 추가: pedaggr01h.zone_id 를 기준 스키마에 반영.
--
-- 실제 DB(개발 Supabase·운영 RDS)에는 이 컬럼이 있고 JPA 엔티티
-- (PedestrianCoordinateJson.zoneId)도 갖고 있는데, 만드는 마이그레이션이
-- 저장소에 없었다(V1의 pedaggr01h 정의에도 없음). Flyway 도입 전에 손으로
-- 추가된 컬럼이라 - V6의 video_url과 같은 종류의 어긋남이다.
--
-- CCTV 파이프라인이 프레임 집계가 어느 구역 것인지 저장하는 컬럼이며,
-- 벌크 적재 API(POST /api/v1/metrics/bulk)가 이 값을 받는다.
-- 기존 DB에서는 IF NOT EXISTS 로 아무 일도 일어나지 않는다.

ALTER TABLE pedaggr01h ADD COLUMN IF NOT EXISTS zone_id BIGINT;

COMMENT ON COLUMN pedaggr01h.zone_id IS
    '이 프레임 집계가 속한 시뮬레이션 구역(mrkaddr01d). Flyway 도입 전 수동 추가된 컬럼을 V7에서 정식 반영.';
