-- market-digital-twin-backend 전체 스키마 (18개 테이블, 실제 ERD 기준 2026-07-20 공유분)
-- Supabase SQL Editor에서 실행
-- 주의: 테이블명은 대소문자 폴딩(자동 소문자화) 문제를 피하기 위해 따옴표 없이 작성함
--       (Hibernate가 기본적으로 식별자를 quoting하지 않으므로 DDL도 동일하게 유지)

-- =========================================
-- 1. 공통코드
-- =========================================
CREATE TABLE IF NOT EXISTS comcode01m (
    code_cob   VARCHAR(3) NOT NULL,   -- 2026-07-24 추가: 공통코드분류
    code       VARCHAR(5) NOT NULL,
    code_name  VARCHAR(50) NOT NULL UNIQUE,
    describe   VARCHAR(200),
    rmk        VARCHAR(500),
    PRIMARY KEY (code_cob, code)
);

-- 2026-07-24 추가: 기존에 이미 생성돼 있던 DB 마이그레이션
-- (code_cob 없이 code 단독 PK였던 것을 (code_cob, code) 복합키로 변경)
ALTER TABLE comcode01m ADD COLUMN IF NOT EXISTS code_cob VARCHAR(3);
-- 기존 행은 code의 앞 3자(도메인 접두사 규칙)를 그대로 code_cob으로 채움
UPDATE comcode01m SET code_cob = substring(code from 1 for 3) WHERE code_cob IS NULL;
ALTER TABLE comcode01m ALTER COLUMN code_cob SET NOT NULL;
-- 기존 PK(code 단독) 제약을 복합키로 교체. 제약명은 CREATE TABLE에서 인라인
-- PRIMARY KEY로 생성했을 때 Postgres 기본 명명 규칙(<table>_pkey)을 따름
ALTER TABLE comcode01m DROP CONSTRAINT IF EXISTS comcode01m_pkey;
ALTER TABLE comcode01m ADD PRIMARY KEY (code_cob, code);

-- =========================================
-- 2. 사용자
-- =========================================
CREATE TABLE IF NOT EXISTS usrusrs01m (
    user_id     BIGSERIAL PRIMARY KEY,
    login_id    VARCHAR(30) NOT NULL UNIQUE,
    password    VARCHAR(64) NOT NULL,
    name        VARCHAR(50) NOT NULL,
    rules_code  VARCHAR(5) NOT NULL,
    org_code    VARCHAR(5) NOT NULL,
    created_at  TIMESTAMP NOT NULL,
    created_ip  VARCHAR(16) NOT NULL,
    updated_at  TIMESTAMP,
    updated_ip  VARCHAR(16),
    -- 2026-07-24 추가: 회원가입 화면의 개인정보 동의 이력(필수 2개 + 선택 1개)
    agree_terms_at      TIMESTAMP,
    agree_privacy_at    TIMESTAMP,
    agree_marketing_at  TIMESTAMP,
    -- 2026-07-24 추가(게시판): 사용자가 담당하는 시장 코드(comcode01m MKT 도메인).
    -- 게시판 목록 조회 시 "본인 담당 시장 게시글만 노출"의 기준 컬럼.
    -- nullable인 이유: 관리자(ROL01)는 시장 제한 없이 전체를 보므로 시장 소속이
    -- 필수가 아님. 회원가입 화면에서 org_code와 동일하게 select로 입력받음.
    market_code VARCHAR(5)
);

-- 기존에 이미 생성돼 있던 DB에도 반영되도록 (2026-07-24 추가)
ALTER TABLE usrusrs01m ADD COLUMN IF NOT EXISTS agree_terms_at TIMESTAMP;
ALTER TABLE usrusrs01m ADD COLUMN IF NOT EXISTS agree_privacy_at TIMESTAMP;
ALTER TABLE usrusrs01m ADD COLUMN IF NOT EXISTS agree_marketing_at TIMESTAMP;
ALTER TABLE usrusrs01m ADD COLUMN IF NOT EXISTS market_code VARCHAR(5);

-- =========================================
-- 3. 현장 변경 (승인/변경 이력)
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
    latitude     DECIMAL(10,8),
    longitude    DECIMAL(11,8),
    -- 2026-07-27 추가: 시장/구역별 권한 분리(상인회·지자체는 본인 담당 시장만 조회,
    -- 관리자는 전체 조회 + 시장 전환)용. usrusrs01m.market_code와 동일한 comcode01m
    -- MKT 도메인 코드.
    market_code  VARCHAR(5)
);

-- 이미 생성되어 있던 DB(신규 컬럼 없이)에도 반영되도록 별도 ALTER도 함께 실행
ALTER TABLE mrkaddr01m ADD COLUMN IF NOT EXISTS market_code VARCHAR(5);

-- =========================================
-- 5. 시장 구역 위치 좌표
-- =========================================
CREATE TABLE IF NOT EXISTS mrkaddr01d (
    zone_id              BIGSERIAL PRIMARY KEY,
    market_id            BIGINT NOT NULL REFERENCES mrkaddr01m(market_id),
    zone_name            VARCHAR(50) NOT NULL,
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
    latitude       DECIMAL(10,8),
    longitude      DECIMAL(11,8),
    is_active      BOOLEAN DEFAULT TRUE,
    -- 2026-07-24 추가: GATE=유입 가중치, STALL 등=매력도 가중치로 쓰이는 범용 weight
    weight         DOUBLE PRECISION DEFAULT 1.0,
    -- 2026-07-24 추가: 오브젝트(매대/푸드트럭 등) 실제 점유 반경(m). SIM 장애물 회피용.
    footprint_radius_m DOUBLE PRECISION,
    updated_at     TIMESTAMP
);

-- 이미 생성되어 있던 DB(신규 컬럼 없이)에도 반영되도록 별도 ALTER도 함께 실행
ALTER TABLE mrkfcts01m ADD COLUMN IF NOT EXISTS weight DOUBLE PRECISION DEFAULT 1.0;
ALTER TABLE mrkfcts01m ADD COLUMN IF NOT EXISTS footprint_radius_m DOUBLE PRECISION;

-- =========================================
-- 7. 위험 점수
-- =========================================
CREATE TABLE IF NOT EXISTS mrkrisk01m (
    risk_id      BIGSERIAL PRIMARY KEY,
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
    zone_id           BIGINT NOT NULL REFERENCES mrkaddr01d(zone_id),
    visitor_count     INTEGER DEFAULT 0,
    density_score     DECIMAL(4,2),
    status_level      VARCHAR(10),
    captured_at       TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_crddnst01m_captured_at ON crddnst01m(captured_at);
CREATE INDEX IF NOT EXISTS idx_crddnst01m_zone_id ON crddnst01m(zone_id);

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
    zone_id            BIGINT NOT NULL REFERENCES mrkaddr01d(zone_id),
    sensor_type_code   VARCHAR(5) NOT NULL,
    ip_address         VARCHAR(50)
);

-- =========================================
-- (11-12. 음향 이벤트 분석/로그 테이블 - audevnt01m/01h 제거됨)
-- =========================================

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
    status_level_code  VARCHAR(5),
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
    status_level_code  VARCHAR(5),
    density_score      INTEGER,
    created_at         TIMESTAMP
);

-- =========================================
-- (15-16. 레이더 센서 데이터/로그 테이블 - senradr01m/01h 제거됨)
-- =========================================

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
    agent_count       INTEGER NOT NULL,
    policy_type_code  VARCHAR(5) NOT NULL,
    created_at        TIMESTAMP
);

-- =========================================
-- 18. 시나리오 예측 결과
-- =========================================
CREATE TABLE IF NOT EXISTS simrslt01d (
    result_id                 BIGSERIAL PRIMARY KEY,
    scenario_id                BIGINT NOT NULL REFERENCES simscnr01m(scenario_id),
    predicted_max_density      DECIMAL(6,2),
    predicted_density          DECIMAL(6,2) NOT NULL,
    predicted_risk_score       INTEGER,
    economic_effect_analysis   TEXT,
    generated_report_path      VARCHAR(1000),
    avg_stay_time              INTERVAL,
    flow_direction              JSONB,
    executed_at                TIMESTAMP NOT NULL
);

-- =========================================
-- 19. 시장 구역 인접 관계 (통로 연결 그래프)
-- =========================================
CREATE TABLE IF NOT EXISTS mrkadjc01m (
    adjacency_id  BIGSERIAL PRIMARY KEY,
    market_id     BIGINT NOT NULL REFERENCES mrkaddr01m(market_id),
    from_zone_id  BIGINT NOT NULL REFERENCES mrkaddr01d(zone_id),
    to_zone_id    BIGINT NOT NULL REFERENCES mrkaddr01d(zone_id),
    path_width    DECIMAL(4,2),
    distance_m    DECIMAL(6,2),
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    -- 2026-07-24 추가: 통로 중심선(GeoJSON LineString, WGS84 [경도,위도] 순서).
    -- 레이아웃 에디터로 실제 통로를 따라 그린 선. NULL이면 SIM이 두 구역이 맞닿은
    -- 경계 중점 1개로 근사해서 대체한다 (fallback, 정확도는 떨어짐).
    path_coordinates TEXT,
    CONSTRAINT uq_mrkadjc01m_edge UNIQUE (from_zone_id, to_zone_id),
    CONSTRAINT ck_mrkadjc01m_no_self CHECK (from_zone_id <> to_zone_id)
);

-- 이미 생성되어 있던 DB(신규 컬럼 없이)에도 반영되도록 별도 ALTER도 함께 실행
ALTER TABLE mrkadjc01m ADD COLUMN IF NOT EXISTS path_coordinates TEXT;

CREATE INDEX IF NOT EXISTS idx_mrkadjc01m_market_id ON mrkadjc01m(market_id);
CREATE INDEX IF NOT EXISTS idx_mrkadjc01m_from_zone ON mrkadjc01m(from_zone_id);

-- =========================================
-- 20. 게시글 (2026-07-24 추가 - 게시판 기능)
-- =========================================
-- 권한 규칙(서비스 레이어에서 강제):
--   - 작성: 로그인한 모든 사용자
--   - 수정/삭제: 관리자(ROL01)는 전체, 그 외는 본인 작성 글만
--   - 공지 고정(is_notice): 관리자만 true로 설정 가능
--   - 카테고리(category_code)의 BCTNT(공지사항)는 관리자만 선택 가능(그 외 BCTFR은 전체 허용)
--   - 목록 노출 범위: 관리자는 전체 시장, 그 외는 본인 market_code 글 + 공지(is_notice=true, 시장 무관 항상 노출)
CREATE TABLE IF NOT EXISTS brdpsts01m (
    post_id      BIGSERIAL PRIMARY KEY,
    -- 공지(is_notice=true)는 시장 무관 항상 노출되므로 market_code가 NULL이어도 됨.
    -- 일반 게시글(is_notice=false)은 작성자의 market_code를 그대로 저장(서비스에서 채움).
    market_code  VARCHAR(5),
    writer_id    BIGINT NOT NULL REFERENCES usrusrs01m(user_id),
    title        VARCHAR(200) NOT NULL,
    content      TEXT NOT NULL,
    is_notice    BOOLEAN NOT NULL DEFAULT FALSE,
    -- 2026-07-24 추가(UI 설계서 반영): 게시판 상단 카테고리 탭(전체/공지사항/자유게시판)
    -- 필터 기준. comcode01m BCT 도메인 코드(BCTNT/BCTFR) 중 하나. "전체" 탭은
    -- 저장값이 아니라 "필터 없음"을 의미하는 UI 상태라 여기 값으로 존재하지 않음.
    -- is_notice(관리자 상단 고정)와는 별개 개념 - 카테고리가 공지사항이 아니어도 고정될 수 있고,
    -- 공지사항 카테고리라고 자동으로 고정되지도 않음.
    -- 2026-07-25 변경: 카테고리를 공지사항/자유게시판 2개로 축소(질문과 답변/제안 삭제).
    category_code VARCHAR(5) NOT NULL DEFAULT 'BCTFR',
    view_count   INTEGER NOT NULL DEFAULT 0,
    like_count   INTEGER NOT NULL DEFAULT 0,
    created_at   TIMESTAMP NOT NULL,
    updated_at   TIMESTAMP
);

-- 기존에 이미 생성돼 있던 DB에도 반영되도록 (2026-07-24 추가)
ALTER TABLE brdpsts01m ADD COLUMN IF NOT EXISTS category_code VARCHAR(5) NOT NULL DEFAULT 'BCTFR';
-- 2026-07-25 추가: 컬럼이 이미 존재하는 DB는 위 ADD COLUMN IF NOT EXISTS가 아무 동작도
-- 안 하므로(컬럼이 이미 있으면 건너뜀), DEFAULT 값 자체를 별도로 갱신해줘야 함
-- (BCTQA(질문과 답변) -> BCTFR(자유게시판), 카테고리 축소 반영).
ALTER TABLE brdpsts01m ALTER COLUMN category_code SET DEFAULT 'BCTFR';

CREATE INDEX IF NOT EXISTS idx_brdpsts01m_market_code ON brdpsts01m(market_code);
CREATE INDEX IF NOT EXISTS idx_brdpsts01m_is_notice ON brdpsts01m(is_notice);
CREATE INDEX IF NOT EXISTS idx_brdpsts01m_category_code ON brdpsts01m(category_code);
CREATE INDEX IF NOT EXISTS idx_brdpsts01m_created_at ON brdpsts01m(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_brdpsts01m_writer_id ON brdpsts01m(writer_id);

-- =========================================
-- 21. 게시글 첨부파일 (2026-07-24 추가 - 게시판 기능)
-- =========================================
-- 실제 파일 바이너리는 S3에 저장하고, 이 테이블은 메타데이터 + S3 오브젝트 키만 보관.
CREATE TABLE IF NOT EXISTS brdattc01d (
    attachment_id  BIGSERIAL PRIMARY KEY,
    post_id        BIGINT NOT NULL REFERENCES brdpsts01m(post_id) ON DELETE CASCADE,
    original_name  VARCHAR(255) NOT NULL,
    s3_key         VARCHAR(500) NOT NULL,
    file_size      BIGINT NOT NULL,
    content_type   VARCHAR(100),
    created_at     TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_brdattc01d_post_id ON brdattc01d(post_id);

-- =========================================
-- 22. 게시글 좋아요 (2026-07-24 추가 - 게시판 기능)
-- =========================================
CREATE TABLE IF NOT EXISTS brdlike01d (
    like_id     BIGSERIAL PRIMARY KEY,
    post_id     BIGINT NOT NULL REFERENCES brdpsts01m(post_id) ON DELETE CASCADE,
    user_id     BIGINT NOT NULL REFERENCES usrusrs01m(user_id),
    created_at  TIMESTAMP NOT NULL,
    CONSTRAINT uq_brdlike01d_post_user UNIQUE (post_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_brdlike01d_post_id ON brdlike01d(post_id);

-- =========================================
-- 실제 시장 공간 데이터는 seed-market-data.sql 로 분리되어 있음
-- 이 파일(schema-init.sql) 실행 후 seed-market-data.sql 을 이어서 실행할 것
-- =========================================
