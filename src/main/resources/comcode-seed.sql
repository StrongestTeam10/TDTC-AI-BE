-- =========================================
-- 공통코드 시드 데이터 (COMCODE01M)
-- 2026-07-24: code_cob(공통코드분류) 컬럼 추가로 PK가 (code_cob, code) 복합키로 변경됨.
-- code 자체는 이전과 동일하게 5자 고정 = [3자 도메인 접두사] + [2자 순번/약어] 규칙을
-- 유지하고, code_cob에는 그 3자 도메인 접두사를 그대로 명시적으로 채움
--   - 순번형: 값들 사이에 자연스러운 순서/등급이 있는 도메인
--   - 약어형: 값들이 대등한 범주라 순서가 없는 도메인
-- schema-init.sql 실행 후 아무 때나 실행 가능 (다른 테이블을 참조하지 않음)
-- =========================================

INSERT INTO comcode01m (code_cob, code, code_name, describe, rmk) VALUES

-- ---------- ROL: 사용자 권한 (usrusrs01m.rules_code) - 순번형 (권한 등급 순) ----------
('ROL', 'ROL01', '관리자',     'usrusrs01m.rules_code - 시스템 전체 관리 권한',           '최고 권한'),
('ROL', 'ROL02', '관제요원',   'usrusrs01m.rules_code - 관제 대시보드 조회/알림 처리',     ''),
('ROL', 'ROL03', '조회자',     'usrusrs01m.rules_code - 조회 전용 권한',                  ''),

-- ---------- ORG: 소속 기관 (usrusrs01m.org_code) - 약어형 ----------
('ORG', 'ORGKT', 'KT',         'usrusrs01m.org_code - 참여기업(KT)',                      ''),
('ORG', 'ORGGV', '지자체',     'usrusrs01m.org_code - 지방자치단체/구청',                 ''),
('ORG', 'ORGMA', '상인회',     'usrusrs01m.org_code - 시장상인회',                        ''),

-- ---------- SEN: 센서 종류 (sensens01m.sensor_type_code) - 약어형 ----------
('SEN', 'SENLD', '라이다',     'sensens01m.sensor_type_code - LiDAR 센서',                'senlidr01m/h 연계'),
('SEN', 'SENCC', 'CCTV',       'sensens01m.sensor_type_code - Vision AI/CCTV 센서',       '향후 확장용'),

-- ---------- LVL: 위험도 레벨 (senlidr01m/h.status_level_code) - 순번형 ----------
('LVL', 'LVL01', 'LOW',        'status_level_code - 원활, 밀집도 0.72명/m2 미만',         'risk.py DENSITY_COMFORTABLE 이하'),
('LVL', 'LVL02', 'MEDIUM',     'status_level_code - 혼잡, 0.72~2.17명/m2',               'risk.py DENSITY_CAPACITY 미만'),
('LVL', 'LVL03', 'HIGH',       'status_level_code - 수용한계 초과, 2.17~5.0명/m2',       'risk.py DENSITY_DANGER~CRITICAL'),
('LVL', 'LVL04', 'CRITICAL',   'status_level_code - 압사 위험, 5.0명/m2 이상',           'risk.py DENSITY_CRITICAL 이상'),

-- ---------- POL: 시나리오 정책 유형 (simscnr01m.policy_type_code) - 약어형 ----------
('POL', 'POLNO', '없음',       'simscnr01m.policy_type_code - 기본 이동 시나리오',        ''),
('POL', 'POLFR', '화재',       'simscnr01m.policy_type_code - 화재 확산 시나리오',        ''),
('POL', 'POLAC', '음향이상',   'simscnr01m.policy_type_code - 비명/충돌음 감지 시나리오',  ''),
('POL', 'POLCB', '통로폐쇄',   'simscnr01m.policy_type_code - 통로 폐쇄 영향 시나리오',    '');
