-- 2026-08-14 추가: mrkbldg01m.pnu_code의 UNIQUE 제약을 기준 스키마에 반영한다.
--
-- V4는 이 테이블을 되찾으면서 "한 필지에 건물이 여러 동일 수 있으니 유니크를 걸지
-- 않는다"고 적었는데, 실제 DB에는 mrkbldg01m_pnu_code_key(UNIQUE)가 걸려 있었다.
-- 손으로 만든 테이블이라 저장소의 정의와 갈라져 있었던 것이고, 빈 DB로 띄우면
-- 운영과 제약이 다른 스키마가 만들어진다.
--
-- 제약을 없애는 대신 남기기로 한 이유:
--   1) 이 컬럼에는 이제 필지번호(19자리)가 아니라 건물관리번호(bd_mgt_sn, 25자리)를
--      넣는다. 건물마다 유일한 값이라 유니크가 성립하고, 같은 건물을 두 번 적재하는
--      사고를 DB가 막아준다.
--   2) 운영 DB의 망원시장 건물이 이 제약을 만족하는 상태로 들어가 있다. 제약을 없애는
--      쪽은 그 테이블에 손을 대는 일이라 더 위험하다.
--
-- ⚠️ V4를 고치지 않고 새 파일로 만든 이유: V4는 이미 적용 완료로 기록돼 있어서
--    내용을 바꾸면 Flyway 체크섬 검증(validate-on-migrate)에 걸려 기동이 실패한다.

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'mrkbldg01m_pnu_code_key'
    ) THEN
        ALTER TABLE mrkbldg01m ADD CONSTRAINT mrkbldg01m_pnu_code_key UNIQUE (pnu_code);
    END IF;
END $$;

COMMENT ON COLUMN mrkbldg01m.pnu_code IS
    '건물관리번호(bd_mgt_sn, 25자리). 앞 19자리가 PNU(필지고유번호)다. '
    '건물 하나가 여러 조각(MultiPolygon)으로 오면 조각마다 -1, -2를 붙여 구분한다.';
