-- =========================================
-- 공통코드 시드 데이터 (COMCODE01M)
-- 규칙: 5자 고정 = [3자 도메인 접두사] + [2자 순번(01,02..) 또는 약어]
--   - 순번형: 값들 사이에 자연스러운 순서/등급이 있는 도메인
--   - 약어형: 값들이 대등한 범주라 순서가 없는 도메인
-- schema-init.sql 실행 후 아무 때나 실행 가능 (다른 테이블을 참조하지 않음)
-- =========================================

INSERT INTO comcode01m (code, code_name, describe, rmk) VALUES

-- ---------- ROL: 사용자 권한 (usrusrs01m.rules_code) - 순번형 (권한 등급 순) ----------
('ROL01', '관리자',     'usrusrs01m.rules_code - 시스템 전체 관리 권한',           '최고 권한'),
('ROL02', '관제요원',   'usrusrs01m.rules_code - 관제 대시보드 조회/알림 처리',     ''),
('ROL03', '조회자',     'usrusrs01m.rules_code - 조회 전용 권한',                  ''),

-- ---------- ORG: 소속 기관 (usrusrs01m.org_code) - 약어형 ----------
('ORGKT', 'KT',         'usrusrs01m.org_code - 참여기업(KT)',                      ''),
('ORGGV', '지자체',     'usrusrs01m.org_code - 지방자치단체/구청',                 ''),
('ORGMA', '상인회',     'usrusrs01m.org_code - 시장상인회',                        ''),

-- ---------- SEN: 센서 종류 (sensens01m.sensor_type_code) - 약어형 ----------
('SENLD', '라이다',     'sensens01m.sensor_type_code - LiDAR 센서',                'senlidr01m/h 연계'),
('SENCC', 'CCTV',       'sensens01m.sensor_type_code - Vision AI/CCTV 센서',       '향후 확장용'),

-- ---------- LVL: 위험도 레벨 (senlidr01m/h.status_level_code) - 순번형 ----------
('LVL01', 'LOW',        'status_level_code - 원활, 밀집도 0.72명/m2 미만',         'risk.py DENSITY_COMFORTABLE 이하'),
('LVL02', 'MEDIUM',     'status_level_code - 혼잡, 0.72~2.17명/m2',               'risk.py DENSITY_CAPACITY 미만'),
('LVL03', 'HIGH',       'status_level_code - 수용한계 초과, 2.17~5.0명/m2',       'risk.py DENSITY_DANGER~CRITICAL'),
('LVL04', 'CRITICAL',   'status_level_code - 압사 위험, 5.0명/m2 이상',           'risk.py DENSITY_CRITICAL 이상'),

-- ---------- POL: 시나리오 정책 유형 (simscnr01m.policy_type_code) - 약어형 ----------
('POLNO', '없음',       'simscnr01m.policy_type_code - 기본 이동 시나리오',        ''),
('POLFR', '화재',       'simscnr01m.policy_type_code - 화재 확산 시나리오',        ''),
('POLAC', '음향이상',   'simscnr01m.policy_type_code - 비명/충돌음 감지 시나리오',  ''),
('POLCB', '통로폐쇄',   'simscnr01m.policy_type_code - 통로 폐쇄 영향 시나리오',    '');
