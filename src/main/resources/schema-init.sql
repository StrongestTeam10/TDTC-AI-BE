-- market-digital-twin-backend 전체 스키마 (18개 테이블, 실제 ERD 기준 2026-07-20 공유분)
-- Supabase SQL Editor에서 실행
-- 주의: 테이블명은 대소문자 폴딩(자동 소문자화) 문제를 피하기 위해 따옴표 없이 작성함
--       (Hibernate가 기본적으로 식별자를 quoting하지 않으므로 DDL도 동일하게 유지)

-- =========================================
-- 1. 공통코드
-- =========================================
CREATE TABLE IF NOT EXISTS comcode01m (
                                          code       VARCHAR(3) PRIMARY KEY,
    code_name  VARCHAR(50) NOT NULL UNIQUE,
    describe   VARCHAR(200),
    mrk        VARCHAR(500)
    );

-- =========================================
-- 2. 사용자
-- =========================================
CREATE TABLE IF NOT EXISTS usrusrs01m (
                                          user_id     BIGSERIAL PRIMARY KEY,
                                          login_id    VARCHAR(30) NOT NULL UNIQUE,
    password    VARCHAR(64) NOT NULL,
    name        VARCHAR(30) NOT NULL,
    roles_code  VARCHAR(5) NOT NULL,
    org_code    VARCHAR(5) NOT NULL,
    created_at  TIMESTAMP NOT NULL,
    created_ip  VARCHAR(16),
    updated_at  TIMESTAMP,
    updated_ip  VARCHAR(16)
    );

-- =========================================
-- 3. 협장변경 (승인/변경 이력)
-- =========================================
CREATE TABLE IF NOT EXISTS entchan01h (
                                          change_id     BIGSERIAL PRIMARY KEY,
                                          user_id       BIGINT NOT NULL REFERENCES usrusrs01m(user_id),
    change_type   VARCHAR(10),
    before_data   TEXT,
    after_data    TEXT,
    status        VARCHAR(15),
    requested_at  TIMESTAMP,
    reviewed_at   TIMESTAMP
    );

-- =========================================
-- 4. 시장 위치
-- =========================================
CREATE TABLE IF NOT EXISTS mrkaddr01m (
                                          market_id    BIGSERIAL PRIMARY KEY,
                                          market_name  VARCHAR(50) NOT NULL,
    latitude     DECIMAL(10,6),
    longitude    DECIMAL(11,6)
    );

-- =========================================
-- 5. 시장 구역 위치 좌표
-- =========================================
CREATE TABLE IF NOT EXISTS mrkaddr01d (
                                          zone_id              BIGSERIAL PRIMARY KEY,
                                          market_id            BIGINT NOT NULL REFERENCES mrkaddr01m(market_id),
    zone_name            VARCHAR(30),
    polygon_coordinates  TEXT
    );

-- =========================================
-- 6. 시설
-- =========================================
CREATE TABLE IF NOT EXISTS mrkfcts01m (
                                          facility_id    BIGSERIAL PRIMARY KEY,
                                          market_id      BIGINT NOT NULL REFERENCES mrkaddr01m(market_id),
    facility_type  VARCHAR(50) NOT NULL,
    name           VARCHAR(50) NOT NULL,
    is_active      BOOLEAN DEFAULT TRUE,
    updated_at     TIMESTAMP
    );

-- =========================================
-- 7. 위험 척수
-- =========================================
CREATE TABLE IF NOT EXISTS mrkrisk01m (
                                          risk_id      BIGSERIAL PRIMARY KEY,
                                          market_id    BIGINT NOT NULL REFERENCES mrkaddr01m(market_id),
    zone_id      BIGINT NOT NULL REFERENCES mrkaddr01d(zone_id),
    risk_score   REAL NOT NULL,
    risk_level   VARCHAR(10) NOT NULL,
    reason_code  VARCHAR(200) NOT NULL,
    detected_at  TIMESTAMP NOT NULL
    );

CREATE INDEX IF NOT EXISTS idx_mrkrisk01m_detected_at ON mrkrisk01m(detected_at);

-- =========================================
-- 8. 인구 밀집도
-- =========================================
CREATE TABLE IF NOT EXISTS crddnst01m (
                                          crowd_density_id  BIGSERIAL PRIMARY KEY,
                                          market_id         BIGINT NOT NULL REFERENCES mrkaddr01m(market_id),
    zone_id           BIGINT NOT NULL REFERENCES mrkaddr01d(zone_id),
    visitor_count     INTEGER DEFAULT 0,
    density_score     DECIMAL(4,2),
    status_level      VARCHAR(10),
    captured_at       TIMESTAMP NOT NULL
    );

CREATE INDEX IF NOT EXISTS idx_crddnst01m_captured_at ON crddnst01m(captured_at);
CREATE INDEX IF NOT EXISTS idx_crddnst01m_market_id ON crddnst01m(market_id);

-- =========================================
-- 9. 인구 밀집도 로그
-- =========================================
CREATE TABLE IF NOT EXISTS crddnst01h (
                                          crowd_density_sq  BIGSERIAL PRIMARY KEY,
                                          crowd_density_id  BIGINT NOT NULL REFERENCES crddnst01m(crowd_density_id),
    visitor_count     INTEGER DEFAULT 0,
    density_score     DECIMAL(4,2),
    status_level      VARCHAR(10) NOT NULL,
    captured_at       TIMESTAMP NOT NULL
    );

-- =========================================
-- 10. 센서 (라이다/레이더/음향 공통 마스터)
-- =========================================
CREATE TABLE IF NOT EXISTS sensens01m (
                                          sensor_id          BIGSERIAL PRIMARY KEY,
                                          market_id          BIGINT NOT NULL REFERENCES mrkaddr01m(market_id),
    zone_id            BIGINT NOT NULL REFERENCES mrkaddr01d(zone_id),
    sensor_type_code   VARCHAR(5) NOT NULL,
    ip_address         VARCHAR(50)
    );

-- =========================================
-- 11. 음향 이벤트 분석
-- =========================================
CREATE TABLE IF NOT EXISTS audevnt01m (
                                          event_id     BIGSERIAL PRIMARY KEY,
                                          sensor_id    BIGINT NOT NULL REFERENCES sensens01m(sensor_id),
    sound_type   VARCHAR(20),
    confidence   DECIMAL(3,2),
    is_checked   BOOLEAN DEFAULT FALSE,
    detected_at  TIMESTAMP NOT NULL
    );

-- =========================================
-- 12. 음향 이벤트 로그
-- =========================================
CREATE TABLE IF NOT EXISTS audevnt01h (
                                          event_sq     BIGSERIAL PRIMARY KEY,
                                          event_id     BIGINT NOT NULL REFERENCES audevnt01m(event_id),
    sound_type   VARCHAR(20),
    confidence   DECIMAL(3,2),
    is_checked   BOOLEAN DEFAULT FALSE,
    detected_at  TIMESTAMP NOT NULL,
    created_at   TIMESTAMP
    );

-- =========================================
-- 13. 라이다 센서 데이터
-- =========================================
CREATE TABLE IF NOT EXISTS senlidr01m (
                                          crowd_density_id   BIGSERIAL PRIMARY KEY,
                                          sensor_id          BIGINT NOT NULL REFERENCES sensens01m(sensor_id),
    pt_cloud_cnt       INTEGER NOT NULL,
    updated_at         TIMESTAMP NOT NULL,
    detect_cnt         INTEGER,
    avg_dist_m         INTEGER,
    status_level_code  VARCHAR(3),
    density_score      INTEGER
    );

-- =========================================
-- 14. 라이다 센서 데이터 로그
-- =========================================
CREATE TABLE IF NOT EXISTS senlidr01h (
                                          crowd_density_sq   BIGSERIAL PRIMARY KEY,
                                          crowd_density_id   BIGINT NOT NULL REFERENCES senlidr01m(crowd_density_id),
    pt_cloud_cnt       INTEGER NOT NULL,
    updated_at         TIMESTAMP NOT NULL,
    detect_cnt         INTEGER,
    avg_dist_m         INTEGER,
    status_level_code  VARCHAR(3),
    density_score      INTEGER,
    created_at         TIMESTAMP
    );

-- =========================================
-- 15. 레이더 센서 데이터
-- =========================================
CREATE TABLE IF NOT EXISTS senrad01m (
                                         crowd_density_id   BIGSERIAL PRIMARY KEY,
                                         sensor_id          BIGINT NOT NULL REFERENCES sensens01m(sensor_id),
    refl_intens        INTEGER NOT NULL,
    updated_at         TIMESTAMP NOT NULL,
    detect_cnt         INTEGER,
    avg_speed          INTEGER,
    status_level_code  VARCHAR(3)
    );

-- =========================================
-- 16. 레이더 센서 데이터 로그
-- =========================================
CREATE TABLE IF NOT EXISTS senrad01h (
                                         crowd_density_sq   BIGSERIAL PRIMARY KEY,
                                         crowd_density_id   BIGINT NOT NULL REFERENCES senrad01m(crowd_density_id),
    refl_intens        INTEGER NOT NULL,
    updated_at         TIMESTAMP NOT NULL,
    detect_cnt         INTEGER,
    avg_speed          INTEGER,
    status_level_code  VARCHAR(3),
    created_at         TIMESTAMP
    );

-- =========================================
-- 17. 시나리오
-- =========================================
CREATE TABLE IF NOT EXISTS simscnr01m (
                                          scenario_id       BIGSERIAL PRIMARY KEY,
                                          change_id         BIGINT REFERENCES entchan01h(change_id),
    scenario_name     VARCHAR(100) NOT NULL,
    market_id         BIGINT REFERENCES mrkaddr01m(market_id),
    virtual_config    TEXT NOT NULL,
    space_mod_data    JSONB,
    reg_datetime      TIMESTAMP NOT NULL,
    agent_count       INTEGER,
    policy_type_code  VARCHAR(3),
    created_at        TIMESTAMP
    );

-- =========================================
-- 18. 시나리오 예측 결과
-- =========================================
CREATE TABLE IF NOT EXISTS simrslt01d (
                                          result_id                 BIGSERIAL PRIMARY KEY,
                                          scenario_id                BIGINT NOT NULL REFERENCES simscnr01m(scenario_id),
    predicted_max_density      DECIMAL(6,2),
    predicted_density          DECIMAL(6,2),
    economic_effect_analysis   TEXT,
    generated_report_path      VARCHAR(1000),
    avg_stay_time              INTERVAL,
    flow_direction              JSONB,
    executed_at                TIMESTAMP NOT NULL
    );

-- =========================================
-- (선택) 동작 확인용 샘플 데이터
-- =========================================
INSERT INTO mrkaddr01m (market_name, latitude, longitude) VALUES
    ('테스트 전통시장', 37.5665, 126.9780);

INSERT INTO mrkaddr01d (market_id, zone_name, polygon_coordinates) VALUES
                                                                       (1, 'A구역', NULL),
                                                                       (1, 'B구역', NULL);