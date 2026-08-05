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
    -- 2026-07-27 추가(ERD 반영): 시장 구분 코드(comcode01m MKT 도메인). 지금은
    -- 시장이 망원시장(MKTMW) 1개뿐이라 값이 하나뿐이지만, 시장이 늘어나면 이
    -- 코드로 구분한다.
    market_code  VARCHAR(5) NOT NULL DEFAULT 'MKTMW'
);

-- 이미 생성되어 있던 DB에도 반영되도록 (2026-07-27 추가)
-- DEFAULT를 잠깐 넣어서 기존 행을 안전하게 채운 뒤, 이후 신규 INSERT는 반드시
-- 명시적으로 값을 넣도록 DEFAULT를 다시 제거한다(신규 시장 추가 시 실수로
-- 망원시장 코드가 재사용되는 것 방지).
ALTER TABLE mrkaddr01m ADD COLUMN IF NOT EXISTS market_code VARCHAR(5);
UPDATE mrkaddr01m SET market_code = 'MKTMW' WHERE market_code IS NULL;
ALTER TABLE mrkaddr01m ALTER COLUMN market_code SET NOT NULL;
ALTER TABLE mrkaddr01m ALTER COLUMN market_code DROP DEFAULT;

-- =========================================
-- 5. 시장 구역 위치 좌표
-- =========================================
-- 2026-07-27: distance_m/path_coordinates/path_width를 잠깐 추가했었으나(같은 개념이
-- mrkadjc01m에 이미 있어 중복), 재재님이 mrkadjc01m을 정본으로 유지하기로 확정하면서
-- 여기서는 다시 제거함(아래 마이그레이션 DROP COLUMN 참고).
CREATE TABLE IF NOT EXISTS mrkaddr01d (
    zone_id              BIGSERIAL PRIMARY KEY,
    market_id            BIGINT NOT NULL REFERENCES mrkaddr01m(market_id),
    zone_name            VARCHAR(50) NOT NULL,
    polygon_coordinates  TEXT
);

-- 이미 생성되어 있던 DB에 반영 (2026-07-27 추가했다가 같은 날 제거)
ALTER TABLE mrkaddr01d DROP COLUMN IF EXISTS distance_m;
ALTER TABLE mrkaddr01d DROP COLUMN IF EXISTS path_coordinates;
ALTER TABLE mrkaddr01d DROP COLUMN IF EXISTS path_width;

-- =========================================
-- 5-1. 시장 구역 출입구 (2026-07-27 신규 → 같은 날 삭제)
-- =========================================
-- MRKEXIT01D를 만들었었으나, MRKFCTS01M(시설)에 이미 facility_type='GATE'로
-- 출입구 데이터가 관리되고 있어 재재님이 그쪽으로 통합하기로 결정. 이 테이블은
-- 만든 당일 삭제해서 애초에 실제 DB에 생성된 적이 없으므로(seed도 없었음)
-- 별도 DROP/마이그레이션이 필요 없음. 혹시 지난 zip을 이미 한 번 실행해서
-- mrkexit01d가 이미 만들어져 있는 경우를 대비해 안전하게 DROP만 걸어둠(데이터 없음).
DROP TABLE IF EXISTS mrkexit01d;

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
    -- 2026-08-04 추가: 층/위치 메모 등 비고
    rmk            VARCHAR(500),
    updated_at     TIMESTAMP
);

-- 이미 생성되어 있던 DB(신규 컬럼 없이)에도 반영되도록 별도 ALTER도 함께 실행
ALTER TABLE mrkfcts01m ADD COLUMN IF NOT EXISTS weight DOUBLE PRECISION DEFAULT 1.0;
ALTER TABLE mrkfcts01m ADD COLUMN IF NOT EXISTS footprint_radius_m DOUBLE PRECISION;
-- 2026-08-04 추가(시설 관리 화면): 층/위치 메모 등 자유 텍스트를 담는 비고란.
-- 별도 컬럼을 새로 만들지 않고 comcode01m.rmk와 같은 이름/성격의 범용 비고 컬럼으로 둠.
ALTER TABLE mrkfcts01m ADD COLUMN IF NOT EXISTS rmk VARCHAR(500);

-- =========================================
-- 7. 위험 점수 (⚠️ 2026-07-27 ERD 변경: 그레인이 "구역 단위"에서 "CCTV 프레임의
--    좌표 1건 단위(coord_id)"로 바뀜 - 완전히 다른 의미의 테이블이 됨)
-- =========================================
-- 기존 zone_id 기반 mrkrisk01m 데이터를 보존하기 위해 테이블명을 바꿔서 남겨둠
-- (DROP하지 않음 - 데이터 유실 없이 마이그레이션하라는 요청 반영).
-- 새 그레인의 mrkrisk01m은 pedaggr01h(20-3번 섹션) 정의 이후에 새로 만듦.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'mrkrisk01m')
       AND NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'mrkrisk01m_zone_legacy') THEN
        ALTER TABLE mrkrisk01m RENAME TO mrkrisk01m_zone_legacy;
    END IF;
END $$;

-- 2026-07-27 추가: mrkrisk01m_zone_legacy는 더 이상 "예전 데이터 보존용"에만
-- 그치지 않고, SIM의 구역 기반 실시간 위험도 저장(insert_risk_results, CCTV
-- 파이프라인이 완전히 붙기 전까지의 과도기 경로)이 계속 이 테이블에 씀. 그래서
-- 기존 mrkrisk01m이 없던 신선한 DB에서도(위 DO 블록만으로는 안 만들어짐) 이
-- 테이블이 항상 존재하도록 fallback CREATE TABLE을 추가함.
CREATE TABLE IF NOT EXISTS mrkrisk01m_zone_legacy (
    risk_id      BIGSERIAL PRIMARY KEY,
    zone_id      BIGINT NOT NULL REFERENCES mrkaddr01d(zone_id),
    risk_score   REAL NOT NULL,
    risk_level   VARCHAR(10) NOT NULL,
    reason_code  VARCHAR(200) NOT NULL,
    detected_at  TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_mrkrisk01m_zone_legacy_detected_at ON mrkrisk01m_zone_legacy(detected_at);

-- =========================================
-- 8~14. 인구 밀집도/센서/라이다 5개 테이블 (2026-07-27 삭제 확정 - ERD 최종본에
-- 없음. CCTV 파이프라인(PEDAGGR01H -> MRKRISK01M)으로 대체됨)
-- =========================================
-- ⚠️ SIM의 fetch_crowd_density()/fetch_adjacency() 등 파이썬 코드가 아직
-- crddnst01m을 읽고 있어서(대시보드 MIRROR 모드), 이 DROP을 실행하면 SIM 코드를
-- 같이 고치기 전까지 대시보드 실시간 조회가 깨진다 - 재재님이 "지금은 테이블만
-- 삭제, SIM 구조 변경은 나중에 별도 안내"로 확인 후 진행.
DROP TABLE IF EXISTS senlidr01h CASCADE;
DROP TABLE IF EXISTS senlidr01m CASCADE;
DROP TABLE IF EXISTS sensens01m CASCADE;
DROP TABLE IF EXISTS crddnst01h CASCADE;
DROP TABLE IF EXISTS crddnst01m CASCADE;

-- 더 이상 안 쓰는 SEN 도메인(센서 종류: 라이다/CCTV) 공통코드도 정리
DELETE FROM comcode01m WHERE code_cob = 'SEN';

-- =========================================
-- 17. 대안 시나리오 (파이프라인 B: 사용자 지정 시뮬레이션 요청)
-- =========================================
-- 2026-07-27 변경: change_id(entchan01h 참조) 제거, user_id(usrusrs01m 참조)로 교체.
-- entchan01h는 승인 워크플로우 테이블이라 시나리오와 실질적 연관이 없었음(대화에서
-- 안내드린 이유 그대로).
CREATE TABLE IF NOT EXISTS simscnr01m (
    scenario_id       BIGSERIAL PRIMARY KEY,
    user_id           BIGINT REFERENCES usrusrs01m(user_id),
    scenario_name     VARCHAR(100) NOT NULL,
    market_id         BIGINT REFERENCES mrkaddr01m(market_id),
    virtual_config    TEXT NOT NULL,
    space_mod_data    JSONB,
    reg_datetime      TIMESTAMP NOT NULL,
    agent_count       INTEGER NOT NULL,
    policy_type_code  VARCHAR(5) NOT NULL,
    created_at        TIMESTAMP
);

-- 이미 change_id 컬럼으로 생성되어 있던 기존 DB에 대한 마이그레이션
ALTER TABLE simscnr01m ADD COLUMN IF NOT EXISTS user_id BIGINT REFERENCES usrusrs01m(user_id);
ALTER TABLE simscnr01m DROP COLUMN IF EXISTS change_id;

-- =========================================
-- 17-1. 현행안 (2026-07-27 신규 - ERD 반영)
-- =========================================
-- 시나리오(대안)와 달리, 현행안은 "지금 실제로 배치돼 있는 시설(mrkfcts01m)/외부요인
-- (extfctr01h)을 그대로 반영해서" 돌리는 기준선(baseline) 시뮬레이션. 실행 시점에
-- 그 시장의 활성화된 시설/외부요인을 조회해서 반영하는 방식이라 이 테이블 자체가
-- mrkfcts01m/extfctr01h를 직접 FK로 참조하진 않음(런타임 조인).
CREATE TABLE IF NOT EXISTS simbsln01m (
    baseline_id       BIGSERIAL PRIMARY KEY,
    market_id         BIGINT NOT NULL REFERENCES mrkaddr01m(market_id),
    baseline_name     VARCHAR(100),
    virtual_config    TEXT NOT NULL,
    policy_type_code  VARCHAR(5) NOT NULL DEFAULT 'POLNO',
    is_active         BOOLEAN NOT NULL DEFAULT TRUE,
    reg_datetime      TIMESTAMP NOT NULL,
    created_at        TIMESTAMP
);

-- =========================================
-- 17-2. 현행안 예측 결과 (2026-07-27 신규 - ERD 반영)
-- =========================================
CREATE TABLE IF NOT EXISTS simbsln01d (
    baseline_result_id    BIGSERIAL PRIMARY KEY,
    baseline_id           BIGINT NOT NULL REFERENCES simbsln01m(baseline_id),
    agent_count            INTEGER NOT NULL,
    predicted_max_density   DECIMAL(6,2),
    predicted_density       DECIMAL(6,2) NOT NULL,
    predicted_risk_score    INTEGER,
    max_density_zone_id     BIGINT REFERENCES mrkaddr01d(zone_id),
    max_density_zone_name   VARCHAR(50),
    evacuated_count         INTEGER,
    executed_at             TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_simbsln01d_baseline_id ON simbsln01d(baseline_id);

-- =========================================
-- 18. 대안 시나리오 예측 결과
-- =========================================
-- 2026-07-27 변경(ERD 반영): baseline_result_id(FK)로 같은 시장의 현행안 결과와
-- 직접 짝지어서, "대안이 현행안 대비 얼마나 나아졌는지" 비교 보고서를 만들 수 있게 함.
-- max_density_zone_id/name, evacuated_count, report_title도 함께 추가.
CREATE TABLE IF NOT EXISTS simrslt01d (
    result_id                 BIGSERIAL PRIMARY KEY,
    scenario_id                BIGINT NOT NULL REFERENCES simscnr01m(scenario_id),
    baseline_result_id         BIGINT REFERENCES simbsln01d(baseline_result_id),
    predicted_max_density      DECIMAL(6,2),
    predicted_density          DECIMAL(6,2) NOT NULL,
    predicted_risk_score       INTEGER,
    economic_effect_analysis   TEXT,
    generated_report_path      VARCHAR(1000),
    report_title                VARCHAR(200),
    avg_stay_time              INTERVAL,
    flow_direction              JSONB,
    max_density_zone_id         BIGINT REFERENCES mrkaddr01d(zone_id),
    max_density_zone_name       VARCHAR(50),
    evacuated_count              INTEGER,
    executed_at                TIMESTAMP NOT NULL
);

-- 이미 생성되어 있던 DB에도 반영되도록 (2026-07-27 추가)
ALTER TABLE simrslt01d ADD COLUMN IF NOT EXISTS baseline_result_id BIGINT REFERENCES simbsln01d(baseline_result_id);
ALTER TABLE simrslt01d ADD COLUMN IF NOT EXISTS report_title VARCHAR(200);
ALTER TABLE simrslt01d ADD COLUMN IF NOT EXISTS max_density_zone_id BIGINT REFERENCES mrkaddr01d(zone_id);
ALTER TABLE simrslt01d ADD COLUMN IF NOT EXISTS max_density_zone_name VARCHAR(50);
ALTER TABLE simrslt01d ADD COLUMN IF NOT EXISTS evacuated_count INTEGER;

-- =========================================
-- 19. 시장 구역 인접 관계 (통로 연결 그래프)
-- =========================================
-- 2026-07-27 변경: market_id 컬럼 제거(from_zone_id/to_zone_id가 이미 어느 구역인지
-- 알려주므로, 그 구역의 market_id는 mrkaddr01d를 거쳐 알 수 있어 중복이었음).
-- created_at 추가.
-- (2026-07-27: 한때 mrkadjc01m -> mrkadjs01m 리네임을 검토했으나 오기로 확인되어
-- mrkadjc01m을 최종 테이블명으로 확정. DB는 재재님이 직접 정리 완료.)
CREATE TABLE IF NOT EXISTS mrkadjc01m (
    adjacency_id  BIGSERIAL PRIMARY KEY,
    from_zone_id  BIGINT NOT NULL REFERENCES mrkaddr01d(zone_id),
    to_zone_id    BIGINT NOT NULL REFERENCES mrkaddr01d(zone_id),
    path_width    DECIMAL(4,2),
    distance_m    DECIMAL(6,2),
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    -- 2026-07-24 추가: 통로 중심선(GeoJSON LineString, WGS84 [경도,위도] 순서).
    -- 레이아웃 에디터로 실제 통로를 따라 그린 선. NULL이면 SIM이 두 구역이 맞닿은
    -- 경계 중점 1개로 근사해서 대체한다 (fallback, 정확도는 떨어짐).
    path_coordinates TEXT,
    created_at    TIMESTAMP,
    CONSTRAINT uq_mrkadjc01m_edge UNIQUE (from_zone_id, to_zone_id),
    CONSTRAINT ck_mrkadjc01m_no_self CHECK (from_zone_id <> to_zone_id)
);

ALTER TABLE mrkadjc01m ADD COLUMN IF NOT EXISTS path_coordinates TEXT;
ALTER TABLE mrkadjc01m ADD COLUMN IF NOT EXISTS created_at TIMESTAMP;
ALTER TABLE mrkadjc01m DROP COLUMN IF EXISTS market_id;

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
-- 23. CCTV 분석 외부요인 (2026-07-27 신규 - ERD 반영)
-- =========================================
-- 시장+날짜 단위 외부요인(날씨/행사 등). video_id는 특정 영상 1건과 연결하고 싶을
-- 때만 채우는 선택 필드(같은 시장/날짜에 영상이 여러 개면 어느 영상을 가리키는지
-- 애매해질 수 있어 용도 확정 필요 - 대화에서 안내드린 부분).
CREATE TABLE IF NOT EXISTS extfctr01h (
    factor_id          BIGSERIAL PRIMARY KEY,
    market_id           BIGINT NOT NULL REFERENCES mrkaddr01m(market_id),
    target_date         DATE NOT NULL,
    weather_condition   VARCHAR(50),
    temperature          DECIMAL(4,1),
    event_category       VARCHAR(50),
    event_name           VARCHAR(200),
    updated_at           TIMESTAMP NOT NULL,
    video_id             BIGINT
);

CREATE INDEX IF NOT EXISTS idx_extfctr01h_market_date ON extfctr01h(market_id, target_date);

-- =========================================
-- 24. CCTV 영상 업로드 관리 (2026-07-27 신규 - ERD 반영)
-- =========================================
CREATE TABLE IF NOT EXISTS vdoclip01m (
    clip_id           BIGSERIAL PRIMARY KEY,
    zone_id            BIGINT NOT NULL REFERENCES mrkaddr01d(zone_id),
    factor_id          BIGINT REFERENCES extfctr01h(factor_id),
    -- 'TEMP'(임시 업로드) | 'RISK'(위험 이벤트로 보존)
    clip_type          VARCHAR(20) NOT NULL,
    s3_clip_url        TEXT NOT NULL,
    start_time         TIMESTAMP NOT NULL,
    end_time           TIMESTAMP NOT NULL,
    is_downloaded      BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted         BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at         TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_vdoclip01m_zone_id ON vdoclip01m(zone_id);
CREATE INDEX IF NOT EXISTS idx_vdoclip01m_expires_at ON vdoclip01m(expires_at);

-- =========================================
-- 25. CCTV 보행자 좌표이력 (2026-07-27 신규 - ERD 반영)
-- =========================================
-- coord_id 단독 PK + (clip_id, frame_id) UNIQUE 제약으로 "같은 클립의 같은 프레임
-- 번호가 중복 적재되는 것"을 막음(대화에서 안내드린 A안). frame_id는 자동증가가
-- 아니라 영상 파이프라인이 실제로 넘겨주는 프레임 번호를 그대로 저장.
CREATE TABLE IF NOT EXISTS pedaggr01h (
    coord_id       BIGSERIAL PRIMARY KEY,
    clip_id         BIGINT NOT NULL REFERENCES vdoclip01m(clip_id),
    frame_id        INTEGER NOT NULL,
    video_id        BIGINT,
    -- {"person_1":{"x":..,"y":..}, "person_2":{...}} 형태의 JSON 객체(배열 아님).
    -- person_N은 프레임 간 지속되는 추적 ID (실제 적재 데이터로 확인됨, 2026-07-31).
    pixels_json     JSONB,
    bev_xyz_json    JSONB,
    captured_at     TIMESTAMP NOT NULL,
    CONSTRAINT uq_pedaggr01h_clip_frame UNIQUE (clip_id, frame_id)
);

CREATE INDEX IF NOT EXISTS idx_pedaggr01h_clip_id ON pedaggr01h(clip_id);
CREATE INDEX IF NOT EXISTS idx_pedaggr01h_captured_at ON pedaggr01h(captured_at);

-- =========================================
-- 26. 위험 점수 (새 그레인: CCTV 프레임 좌표 1건 = coord_id 단위)
-- =========================================
-- 7번 섹션에서 zone_id 기반 옛 mrkrisk01m을 mrkrisk01m_zone_legacy로 보존해뒀음.
-- 이 테이블이 ERD가 정의한 새 mrkrisk01m(프레임 단위 위험도)이다.
CREATE TABLE IF NOT EXISTS mrkrisk01m (
    risk_id      BIGSERIAL PRIMARY KEY,
    coord_id     BIGINT NOT NULL REFERENCES pedaggr01h(coord_id),
    risk_score   REAL NOT NULL,
    -- comcode01m LVL 도메인(LVL01~04) 재사용
    risk_level   VARCHAR(10) NOT NULL,
    reason_code  VARCHAR(200) NOT NULL,
    detected_at  TIMESTAMP NOT NULL,
    total_count  INTEGER
);

CREATE INDEX IF NOT EXISTS idx_mrkrisk01m_detected_at ON mrkrisk01m(detected_at);
CREATE INDEX IF NOT EXISTS idx_mrkrisk01m_coord_id ON mrkrisk01m(coord_id);

-- =========================================
-- 27. CCTV 긴급 신고 이력 (2026-07-27 신규 - ERD 반영)
-- =========================================
CREATE TABLE IF NOT EXISTS emgalrt01h (
    alert_id      BIGSERIAL PRIMARY KEY,
    zone_id        BIGINT NOT NULL REFERENCES mrkaddr01d(zone_id),
    alert_type     VARCHAR(50),
    is_resolved    BOOLEAN NOT NULL DEFAULT FALSE,
    alerted_at     TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_emgalrt01h_zone_id ON emgalrt01h(zone_id);
CREATE INDEX IF NOT EXISTS idx_emgalrt01h_is_resolved ON emgalrt01h(is_resolved);

-- =========================================
-- 28. CCTV 사후 분석 보고서 (2026-07-27 신규 - ERD 반영)
-- =========================================
CREATE TABLE IF NOT EXISTS pstrprt01h (
    report_id      BIGSERIAL PRIMARY KEY,
    alert_id        BIGINT NOT NULL REFERENCES emgalrt01h(alert_id),
    target_date     DATE NOT NULL,
    llm_summary     TEXT,
    s3_pdf_url      TEXT,
    created_at      TIMESTAMP NOT NULL,
    video_id        BIGINT
);

CREATE INDEX IF NOT EXISTS idx_pstrprt01h_alert_id ON pstrprt01h(alert_id);

-- =========================================
-- 29. 시설 외관 사진 (2026-08-04 신규 - 상점 외관 직접 촬영 데이터 수집 파이프라인)
-- =========================================
-- 흐름: 사진 업로드 -> BE가 EXIF에서 GPS/촬영일시 추출(exif_*) -> 사용자가 지도에서
-- 위치를 손으로 보정(corrected_*, 항상 NOT NULL - 스마트폰 GPS 오차 5~15m 및 좁은
-- 골목 반사 오차 때문에 EXIF 값을 그대로 신뢰하지 않고 항상 사람 보정을 거침) ->
-- 방향(동서남북)은 EXIF 방향값(신뢰 불가)을 쓰지 않고 촬영자가 직접 라벨링.
-- 시설 하나당 같은 방향이라도 재촬영 시 새 행으로 누적(과거 사진 보존, 나중에 3D
-- 트윈 텍스처를 만들 때 최신 사진만 골라 쓰거나 시계열로 비교하는 용도 모두 가능).
CREATE TABLE IF NOT EXISTS mrkfcph01d (
    photo_id             BIGSERIAL PRIMARY KEY,
    facility_id          BIGINT NOT NULL REFERENCES mrkfcts01m(facility_id) ON DELETE CASCADE,
    -- comcode01m DIR 도메인(DIRNO/DIREA/DIRSO/DIRWE) - 촬영자가 직접 라벨링, FK 제약은
    -- 다른 comcode 참조 컬럼(rules_code/market_code 등)과 동일하게 애플리케이션에서 검증
    direction_code       VARCHAR(5) NOT NULL,
    s3_key               VARCHAR(500) NOT NULL,
    original_name        VARCHAR(255) NOT NULL,
    -- EXIF에서 추출된 원본 GPS. EXIF에 GPS 태그 자체가 없는 사진도 많아 NULL 허용
    exif_latitude         DECIMAL(10,8),
    exif_longitude         DECIMAL(11,8),
    -- 지도에서 사람이 보정한 최종 좌표. 항상 사용자 확인을 거치므로 NOT NULL
    corrected_latitude    DECIMAL(10,8) NOT NULL,
    corrected_longitude   DECIMAL(11,8) NOT NULL,
    -- EXIF DateTimeOriginal. 없는 사진도 있어 NULL 허용
    captured_at           TIMESTAMP,
    uploaded_by           BIGINT NOT NULL REFERENCES usrusrs01m(user_id),
    created_at            TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_mrkfcph01d_facility_id ON mrkfcph01d(facility_id);
CREATE INDEX IF NOT EXISTS idx_mrkfcph01d_direction_code ON mrkfcph01d(direction_code);

-- =========================================
-- 실제 시장 공간 데이터는 seed-market-data.sql 로 분리되어 있음
-- 이 파일(schema-init.sql) 실행 후 seed-market-data.sql 을 이어서 실행할 것
-- =========================================
