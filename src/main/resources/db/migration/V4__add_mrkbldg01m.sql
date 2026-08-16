-- 2026-08-14 추가: 건물 폴리곤 테이블(MRKBLDG01M).
--
-- 이 테이블은 지금까지 마이그레이션에도 시드에도 없었다. 운영 DB에는 손으로 만들어
-- 넣은 상태였고, 저장소에는 Building.java(JPA 엔티티)와 SIM repository.py의 SELECT만
-- 있었다. 그래서 빈 DB에 애플리케이션을 올리면 ddl-auto: validate가 "테이블 없음"으로
-- 기동 자체를 실패시킨다.
--
-- 브이월드 API로 건물을 자동 적재하기 시작하면 이 테이블이 정식 스키마의 일부가 되므로
-- 여기서 정의를 되찾아 둔다.
--
-- ⚠️ 운영 DB에는 이미 이 테이블과 데이터(망원시장 건물)가 있다. CREATE TABLE IF NOT
--    EXISTS라 운영에서는 아무 일도 일어나지 않고, 빈 DB에서만 생성된다. 기존 데이터를
--    건드리는 구문은 이 파일에 하나도 없다.

CREATE TABLE IF NOT EXISTS mrkbldg01m (
    building_id          BIGSERIAL PRIMARY KEY,
    market_id            BIGINT NOT NULL REFERENCES mrkaddr01m(market_id),

    -- 필지고유번호 19자리(법정동코드 10 + 대지구분 1 + 본번 4 + 부번 4).
    -- 브이월드 건물 레이어(LT_C_SPBD)는 PNU를 따로 주지 않고 25자리 건물관리번호
    -- (bd_mgt_sn)를 주는데, 그 앞 19자리가 곧 PNU다. 한 필지에 건물이 여러 동일 수
    -- 있어 값이 중복될 수 있으므로 유니크 제약은 걸지 않는다.
    pnu_code             VARCHAR(30) NOT NULL,

    -- GeoJSON Polygon 문자열(WGS84 [경도, 위도] 순). mrkaddr01d·mrkcctv01m과 같은
    -- 형식이라 FE 파싱 함수와 SIM parse_polygon을 그대로 쓴다.
    -- ⚠️ MultiPolygon은 넣지 않는다 - SIM parse_polygon이 거부하고 FE는 조용히
    --    잘못된 좌표를 만든다. 외부 API가 MultiPolygon을 주면 조각별로 나눠 저장한다.
    polygon_coordinates  TEXT NOT NULL,

    -- 건물 높이(m). 브이월드는 높이를 주지 않고 지상층수(gro_flo_co)만 주므로
    -- 대부분 층수로 추정한 값이 들어간다. 그 사실을 height_estimated로 남긴다.
    height_m             NUMERIC(6,2) NOT NULL,
    height_estimated     BOOLEAN NOT NULL,
    floors               INTEGER NOT NULL,

    created_at           TIMESTAMP
);

-- 조회는 항상 시장 단위다(BuildingRepository.findByMarketId, SIM fetch_buildings).
CREATE INDEX IF NOT EXISTS idx_mrkbldg01m_market ON mrkbldg01m(market_id);
