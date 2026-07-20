-- market-digital-twin-backend 초기 스키마
-- 실제 ERD 기준 (2026-07-20 공유분), 우선 구현된 4개 테이블만 포함
-- 나머지 14개 테이블(USRUSR501M, SENSENS01M, SENLIDR01M/H, SENRAD01M/H,
--  AUDEVNT01M/H, SIMSCNR01M, SIMRSLT01D, ENTCHAN01H, MRKFCTS01M,
--  CRDDNST01H, COMCODE01M)은 다음 단계에서 추가
-- Supabase SQL Editor에서 실행

-- MRKADDR01M - 시장 위치
CREATE TABLE IF NOT EXISTS MRKADDR01M (
    market_id    BIGSERIAL PRIMARY KEY,
    market_name  VARCHAR(50) NOT NULL,
    latitude     DECIMAL(10,6),
    longitude    DECIMAL(11,6)
);

-- MRKADDR01D - 시장 구역 위치 좌표
CREATE TABLE IF NOT EXISTS MRKADDR01D (
    zone_id              BIGSERIAL PRIMARY KEY,
    market_id            BIGINT NOT NULL REFERENCES MRKADDR01M(market_id),
    zone_name            VARCHAR(30),
    polygon_coordinates  TEXT
);

-- CRDDNST01M - 인구 밀집도
CREATE TABLE IF NOT EXISTS CRDDNST01M (
    crowd_density_id  BIGSERIAL PRIMARY KEY,
    market_id         BIGINT NOT NULL REFERENCES MRKADDR01M(market_id),
    zone_id           BIGINT NOT NULL REFERENCES MRKADDR01D(zone_id),
    visitor_count     INTEGER DEFAULT 0,
    density_score     DECIMAL(4,2),
    status_level      VARCHAR(10),
    captured_at       TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_crddnst01m_captured_at ON CRDDNST01M(captured_at);
CREATE INDEX IF NOT EXISTS idx_crddnst01m_market_id ON CRDDNST01M(market_id);

-- MRKRISK01M - 위험 척수
CREATE TABLE IF NOT EXISTS MRKRISK01M (
    risk_id      BIGSERIAL PRIMARY KEY,
    market_id    BIGINT NOT NULL REFERENCES MRKADDR01M(market_id),
    zone_id      BIGINT NOT NULL REFERENCES MRKADDR01D(zone_id),
    risk_score   REAL,
    risk_level   VARCHAR(10) NOT NULL,
    reason_code  VARCHAR(300) NOT NULL,
    detected_at  TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_mrkrisk01m_detected_at ON MRKRISK01M(detected_at);

-- (선택) 동작 확인용 샘플 데이터
INSERT INTO MRKADDR01M (market_name, latitude, longitude) VALUES
    ('테스트 전통시장', 37.5665, 126.9780);

INSERT INTO MRKADDR01D (market_id, zone_name, polygon_coordinates) VALUES
    (1, 'A구역', NULL),
    (1, 'B구역', NULL);
