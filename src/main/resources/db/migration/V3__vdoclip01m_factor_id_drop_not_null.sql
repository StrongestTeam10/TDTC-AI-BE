-- =========================================================================
-- V3 — vdoclip01m.factor_id NOT NULL 해제 (2026-08-14)
--
-- 요청 사유: 당직자 SMS 기능을 테스트하는 과정에서 factor_id 없이 클립을
-- 저장해야 하는 경우가 있었다(2026-08-14 팀 요청).
--
-- 사유와 별개로, 이 변경은 원래 갈라져 있던 것을 맞추는 쪽에 가깝다.
--   - V1(기준 스키마)의 vdoclip01m.factor_id에는 NOT NULL이 없다.
--   - VideoClip 엔티티의 @Column(name = "factor_id")에도 nullable=false가 없다.
--   - 즉 NOT NULL이 실제로 걸려 있는 건 Flyway 도입 전에 손으로 만든
--     운영 RDS·개발 DB뿐이고, 코드/스키마 파일 기준으로는 이미 NULL 허용이다.
--
-- 그래서 빈 DB에서는 이 문장이 아무것도 하지 않고 지나간다
-- (Postgres는 걸려 있지 않은 NOT NULL을 DROP해도 오류가 아니다).
--
-- FK(extfctr01h.factor_id 참조)는 그대로 유지된다. NULL은 FK 검사 대상이
-- 아니므로, 값이 들어올 때만 위험요소 존재 여부가 검증된다.
-- =========================================================================

ALTER TABLE vdoclip01m ALTER COLUMN factor_id DROP NOT NULL;
