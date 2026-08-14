-- =========================================================================
-- V2 — 당직자 SMS 발송용 사용자 컬럼 추가 (2026-08-14)
--
-- "현재 당직인 사용자를 DB에서 찾아 문자를 보낸다"는 기능에 필요한 두 컬럼.
--
--   phone_number : 수신 번호. 기존 사용자는 값이 없으므로 NULL 허용.
--                  '010-1234-5678'처럼 하이픈을 포함해도 VARCHAR(20)이면 충분.
--   is_duty      : 당직 여부. 기존 행은 전부 "당직 아님"이면 되므로
--                  DEFAULT false와 함께 한 번에 NOT NULL로 붙인다.
--                  (Postgres 11+는 DEFAULT가 상수면 테이블 재작성 없이
--                   메타데이터만 갱신하므로 운영 RDS에서도 잠금이 짧다.
--                   approval_status처럼 NULL 백필 → SET NOT NULL 3단계로
--                   나눌 필요가 없는 이유.)
--
-- IF NOT EXISTS를 붙인 이유: 이미 손으로 컬럼을 추가해 둔 DB가 있어도 실패하지
-- 않고 넘어가게 하기 위함(중간 실패 후 재시도도 안전해진다).
-- =========================================================================

ALTER TABLE usrusrs01m ADD COLUMN IF NOT EXISTS phone_number VARCHAR(20);
ALTER TABLE usrusrs01m ADD COLUMN IF NOT EXISTS is_duty BOOLEAN NOT NULL DEFAULT false;
