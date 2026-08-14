-- 2026-08-11 마이그레이션: CCTV 관제 구역(mrkcctv01m) + 시장 오브젝트 설정(mrkobjt01m)
--
-- schema-init.sql 전체는 재실행 시 실패한다(ALTER ... ADD PRIMARY KEY 등 IF NOT EXISTS가
-- 없는 구문이 섞여 있음). 이 파일은 이번 변경분만 떼어낸 것으로 전부 멱등이라 여러 번
-- 실행해도 안전하다.
--
-- 실행: psql -h <RDS> -U tdtcai_admin -d tdtcai -f 2026-08-11_cctv_marketobject.sql

-- =========================================
-- 31. CCTV 관제 구역 (2026-08-11 신규)
-- =========================================
-- ⚠️ 시뮬레이션 구역(MRKADDR01D)과 반드시 별도 테이블로 둔다.
--
-- MRKADDR01D는 SIM(app/db/repository.py)이 자체 SQL로
--   SELECT ... FROM mrkaddr01d WHERE market_id = %s
-- 처럼 시장의 모든 구역을 통째로 읽어 Mesa 에이전트를 배치하고 위험도를 계산한다.
-- BE 쪽에서도 MarketService(지도 표시) / ReportService(정책 보고서 구역 비교표) /
-- ZoneAdjacency(통로 연결)가 같은 테이블을 본다.
--
-- 여기에 CCTV 관제용 구역을 섞으면 시뮬레이션 결과 수치와 정책 보고서 내용이
-- 함께 바뀌고, 시뮬레이션 비교 화면 지도에도 폴리곤이 겹쳐 그려진다.
-- zone_type 컬럼으로 걸러내는 방법도 있었지만, SIM이 ORM 없이 직접 SQL을 쓰고 있어
-- 쿼리 한 곳만 놓쳐도 조용히 섞이므로 테이블 자체를 분리했다.
--
-- 이 테이블은 상점 위치 등록 화면과 CCTV 파이프라인만 참조한다.
--
-- 2026-08-11(2차) 재설계: 고정 슬롯(zone_no 1~4)을 버리고, 시뮬레이션 구역(zone_id)에
-- 소속되는 행을 등록마다 추가하는 구조로 바꿨다. CCTV 구역은 소속 시뮬레이션 구역
-- 폴리곤 안에서만 그려지며, 목록은 zone_id로 MRKADDR01D를 조인해 구역명을 보여준다.
CREATE TABLE IF NOT EXISTS mrkcctv01m (
    cctv_zone_id         BIGSERIAL PRIMARY KEY,
    market_id            BIGINT NOT NULL REFERENCES mrkaddr01m(market_id),
    -- 소속 시뮬레이션 구역. 이 구역 폴리곤 안에서만 CCTV 사각형을 그릴 수 있다.
    -- 목록에서 구역명은 이 FK로 MRKADDR01D를 조인해 가져온다.
    zone_id              BIGINT NOT NULL REFERENCES mrkaddr01d(zone_id),
    -- GeoJSON Polygon 문자열. MRKADDR01D.polygon_coordinates와 동일한 형식이라
    -- 지도 렌더링 코드(파싱 함수)를 그대로 재사용할 수 있다.
    -- CCTV 호모그래피 ROI라 꼭짓점은 4개 사각형으로 받는다.
    polygon_coordinates  TEXT NOT NULL,
    -- CCTV 사용/미사용. 미사용이면 파이프라인이 이 구역을 분석 대상에서 뺀다.
    is_active            BOOLEAN DEFAULT TRUE,
    rmk                  VARCHAR(500),
    updated_at           TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_mrkcctv01m_market_id ON mrkcctv01m(market_id);
CREATE INDEX IF NOT EXISTS idx_mrkcctv01m_zone_id ON mrkcctv01m(zone_id);

-- 이미 1차 버전(zone_no/zone_name/유니크 제약)으로 만들어진 DB를 위한 이관.
-- 테스트 단계라 기존 행이 있어도 zone_id를 소급 채울 수 없으므로 비운 뒤 새로 등록한다.
ALTER TABLE mrkcctv01m DROP CONSTRAINT IF EXISTS uq_mrkcctv01m_market_zone_no;
ALTER TABLE mrkcctv01m ADD COLUMN IF NOT EXISTS zone_id BIGINT REFERENCES mrkaddr01d(zone_id);
ALTER TABLE mrkcctv01m ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE;
ALTER TABLE mrkcctv01m DROP COLUMN IF EXISTS zone_no;
ALTER TABLE mrkcctv01m DROP COLUMN IF EXISTS zone_name;

-- =========================================
-- 32. 시장 오브젝트/구조 설정 (2026-08-11 신규)
-- =========================================
-- 시뮬레이션 비교(파이프라인 B)의 초기 배치로 쓸 "오브젝트 배치 + 통로 제어 정책"을
-- 시장 구조 등록 화면에서 미리 등록해두는 곳. 시장당 1세트(마스터)다.
--
-- ⚠️ 정규화하지 않고 JSON으로 통째 저장한다(재재님 결정). 오브젝트/정책 형식이
-- 이미 시뮬레이션 요청(PlacedObject/CorridorPolicy)과 1:1이라, 그대로 담아 화면과
-- 시뮬레이션이 변환 없이 주고받게 하기 위함. simscnr01m.virtual_config가 시나리오
-- 설정을 JSON으로 담는 것과 같은 방식이다.
--
-- 실제 시뮬레이션 실행 결과는 기존대로 현행안(simbsln01m)/시나리오(simscnr01m)에
-- 적재된다. 이 테이블은 "실행 전 초기 배치"만 보관한다.
-- 좌표 JSON을 TEXT로 두는 이유는 simscnr01m.virtual_config와 같다: 통째로 저장/조회만
-- 하고 DB 안에서 JSON 쿼리를 하지 않으므로, JSONB 대신 TEXT로 두면 엔티티를 그냥
-- String으로 매핑할 수 있어 Hibernate의 varchar↔jsonb 캐스팅 문제를 피한다.
CREATE TABLE IF NOT EXISTS mrkobjt01m (
    config_id              BIGSERIAL PRIMARY KEY,
    market_id              BIGINT NOT NULL REFERENCES mrkaddr01m(market_id),
    -- PlacedObject[] (food_truck/obstacle/event_zone/rest_area + zoneId + intensity + 좌표)
    objects_json           TEXT NOT NULL DEFAULT '[]',
    -- CorridorPolicy[] (fromZoneId/toZoneId/action/allowedDirection)
    corridor_policies_json TEXT NOT NULL DEFAULT '[]',
    updated_at             TIMESTAMP,
    -- 시장당 한 세트만 둔다(화면이 전체 세트를 통째로 덮어쓰기 하므로).
    CONSTRAINT uq_mrkobjt01m_market UNIQUE (market_id)
);

CREATE INDEX IF NOT EXISTS idx_mrkobjt01m_market_id ON mrkobjt01m(market_id);
