-- =========================================
-- 공통코드 시드 데이터 (COMCODE01M)
-- 2026-07-24: code_cob(공통코드분류) 컬럼 추가로 PK가 (code_cob, code) 복합키로 변경됨.
-- code 자체는 이전과 동일하게 5자 고정 = [3자 도메인 접두사] + [2자 순번/약어] 규칙을
-- 유지하고, code_cob에는 그 3자 도메인 접두사를 그대로 명시적으로 채움
--   - 순번형: 값들 사이에 자연스러운 순서/등급이 있는 도메인
--   - 약어형: 값들이 대등한 범주라 순서가 없는 도메인
-- schema-init.sql 실행 후 아무 때나 실행 가능 (다른 테이블을 참조하지 않음)
-- =========================================
DELETE FROM comcode01m;

INSERT INTO comcode01m (code_cob, code, code_name, describe, rmk) VALUES

-- ---------- ROL: 사용자 권한 (usrusrs01m.rules_code) - 순번형 (권한 등급 순) ----------
('ROL', 'ROL01', '관리자',     'usrusrs01m.rules_code - 시스템 전체 관리 권한',           '최고 권한'),
('ROL', 'ROL02', '관제요원',   'usrusrs01m.rules_code - 관제 대시보드 조회/알림 처리',     ''),
('ROL', 'ROL03', '조회자',     'usrusrs01m.rules_code - 조회 전용 권한',                  ''),

-- ---------- ORG: 소속 기관 (usrusrs01m.org_code) - 약어형 ----------
('ORG', 'ORGKT', 'KT',         'usrusrs01m.org_code - 참여기업(KT)',                      ''),
('ORG', 'ORGGV', '지자체',     'usrusrs01m.org_code - 지방자치단체/구청',                 ''),
('ORG', 'ORGMA', '상인회',     'usrusrs01m.org_code - 시장상인회',                        ''),

-- ---------- SEN 도메인(sensens01m.sensor_type_code용) 삭제 안내 ----------
-- 2026-07-27: sensens01m/senlidr01m/senlidr01h/crddnst01m/crddnst01h 5개 테이블이
-- ERD 최종본에서 빠지면서 함께 제거함(schema-init.sql의 DROP TABLE 참고).
-- 기존 DB에서는 schema-init.sql의 DELETE FROM comcode01m WHERE code_cob = 'SEN'가
-- 처리하므로, 여기서는 처음부터 INSERT하지 않음(신선한 DB 기준).

-- ---------- LVL: 위험도 레벨 (senlidr01m/h.status_level_code) - 순번형 ----------
('LVL', 'LVL01', 'LOW',        'status_level_code - 원활, 밀집도 0.72명/m2 미만',         'risk.py DENSITY_COMFORTABLE 이하'),
('LVL', 'LVL02', 'MEDIUM',     'status_level_code - 혼잡, 0.72~2.17명/m2',               'risk.py DENSITY_CAPACITY 미만'),
('LVL', 'LVL03', 'HIGH',       'status_level_code - 수용한계 초과, 2.17~5.0명/m2',       'risk.py DENSITY_DANGER~CRITICAL'),
('LVL', 'LVL04', 'CRITICAL',   'status_level_code - 압사 위험, 5.0명/m2 이상',           'risk.py DENSITY_CRITICAL 이상'),

-- ---------- POL: 시나리오 정책 유형 (simscnr01m.policy_type_code) - 약어형 ----------
('POL', 'POLNO', '없음',       'simscnr01m.policy_type_code - 기본 이동 시나리오',        ''),
('POL', 'POLFR', '화재',       'simscnr01m.policy_type_code - 화재 확산 시나리오',        ''),
('POL', 'POLAC', '음향이상',   'simscnr01m.policy_type_code - 비명/충돌음 감지 시나리오',  ''),
('POL', 'POLCB', '통로폐쇄',   'simscnr01m.policy_type_code - 통로 폐쇄 영향 시나리오',    ''),

-- ---------- MKT: 담당 시장 (usrusrs01m.market_code, brdpsts01m.market_code) - 약어형 ----------
-- 2026-07-24 추가(게시판): 사용자의 담당 시장 + 게시글 노출 범위 판정 기준.
-- 지금은 seed-market-data.sql에 실제 데이터가 있는 망원시장 1개뿐이라 이 값 하나만 등록.
-- 시장이 늘어나면 mrkaddr01m에 새 시장을 추가할 때 여기도 함께 추가할 것.
('MKT', 'MKTMW', '망원시장',   'usrusrs01m.market_code / brdpsts01m.market_code - 담당 시장 구분', ''),
('MKT', 'MKTHD', '해운대시장',   'usrusrs01m.market_code / brdpsts01m.market_code - 담당 시장 구분', ''),

-- ---------- BCT: 게시판 카테고리 (brdpsts01m.category_code) - 약어형 ----------
-- 2026-07-24 추가(UI 설계서 반영): 게시판 상단 카테고리 탭. "전체" 탭은 필터 없음을
-- 뜻하는 UI 상태라 코드로 존재하지 않음. BCTNT(공지사항)는 관리자만 작성 가능(AuthService
-- 아님, PostService에서 검증) - is_notice(상단 고정)와는 별개 개념.
-- 2026-07-25 변경: 카테고리를 공지사항/자유게시판 2개로 축소(질문과 답변/제안 삭제,
-- 자유게시판으로 통합). 기존 DB에 이미 BCTQA/BCTSG로 반영돼 있다면 아래 마이그레이션
-- 블록을 먼저 실행할 것(이 INSERT문만 다시 실행하면 안 됨 - PK 중복 오류 발생).
('BCT', 'BCTNT', '공지사항',   'brdpsts01m.category_code - 공지사항 카테고리 (관리자만 작성 가능)', ''),
('BCT', 'BCTFR', '자유게시판', 'brdpsts01m.category_code - 자유게시판 카테고리',                    '');

-- ---------- ENT 도메인(mrkexit01d.entrance_type_code용) 삭제 안내 ----------
-- 2026-07-27: mrkexit01d 테이블 자체를 만든 당일 삭제(mrkfcts01m의 GATE 시설로
-- 출입구를 통합 관리하기로 결정)했으므로 ENT 도메인 코드는 처음부터 추가하지 않음.

-- =========================================
-- 마이그레이션(2026-07-25): 카테고리 공지사항/자유게시판 2개로 축소
-- 이미 comcode-seed.sql을 한 번 실행해서 BCTQA/BCTSG가 이미 들어가 있는 기존 DB에서만
-- 아래를 실행할 것 (신규 DB는 위 INSERT문에 이미 BCTFR만 반영돼 있어 실행 불필요).
-- =========================================
-- 기존에 BCTQA(질문과 답변)/BCTSG(제안)로 작성된 게시글을 BCTFR(자유게시판)로 이관
UPDATE brdpsts01m SET category_code = 'BCTFR' WHERE category_code IN ('BCTQA', 'BCTSG');
-- 더 이상 쓰지 않는 카테고리 공통코드 삭제
DELETE FROM comcode01m WHERE code_cob = 'BCT' AND code IN ('BCTQA', 'BCTSG');
-- 자유게시판 코드가 아직 없다면 추가 (이미 있으면 건너뜀)
INSERT INTO comcode01m (code_cob, code, code_name, describe, rmk)
VALUES ('BCT', 'BCTFR', '자유게시판', 'brdpsts01m.category_code - 자유게시판 카테고리', '')
ON CONFLICT (code_cob, code) DO NOTHING;
