-- =====================================================================================================
-- V2: FinSphere Nexus 2.0 finance changes
--   * audit columns + optimistic locking on every business table (AuditableEntity)
--   * timestamps become TIMESTAMPTZ. 1.x stored local wall-clock times, so they are read in the zone the
--     1.x services ran in: Flyway placeholder legacy-time-zone (env FSN_LEGACY_TIME_ZONE, default Asia/Kolkata)
--   * NOT NULL + defaults for flags and amounts that 1.x left nullable
--   * integrity rules the database can enforce: one cycle per (plan, month), one active member per slot,
--     amount checks (NOT VALID: enforced for new rows, existing rows are not re-checked)
--   * indexes matching the application's queries
--   * loans keep the interest deduction and leave type they were created with
-- =====================================================================================================

-- ---------------------------------------------------------------- chit_plans
ALTER TABLE chit_plans ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE '${legacy-time-zone}';
ALTER TABLE chit_plans ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE '${legacy-time-zone}';
UPDATE chit_plans SET created_at = now() WHERE created_at IS NULL;
ALTER TABLE chit_plans ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE chit_plans ADD COLUMN IF NOT EXISTS updated_by BIGINT;
ALTER TABLE chit_plans ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE chit_plans ADD COLUMN IF NOT EXISTS payment_window_days INTEGER;
UPDATE chit_plans SET is_active = TRUE WHERE is_active IS NULL;
ALTER TABLE chit_plans ALTER COLUMN is_active SET DEFAULT TRUE;
ALTER TABLE chit_plans ALTER COLUMN is_active SET NOT NULL;
ALTER TABLE chit_plans ADD CONSTRAINT ck_chit_plans_values CHECK (
    total_value > 0 AND monthly_installment > 0 AND max_bid_limit >= 0 AND max_bid_limit <= total_value
    AND commission_percentage BETWEEN 0 AND 100 AND total_members >= 2 AND duration_months >= 1) NOT VALID;
CREATE INDEX IF NOT EXISTS idx_chit_plans_active_start ON chit_plans (is_active, start_date DESC);

-- ---------------------------------------------------------------- chit_monthly_cycles
ALTER TABLE chit_monthly_cycles ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE '${legacy-time-zone}';
UPDATE chit_monthly_cycles SET created_at = now() WHERE created_at IS NULL;
ALTER TABLE chit_monthly_cycles ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE chit_monthly_cycles ADD COLUMN IF NOT EXISTS created_by BIGINT;
ALTER TABLE chit_monthly_cycles ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ;
ALTER TABLE chit_monthly_cycles ADD COLUMN IF NOT EXISTS updated_by BIGINT;
ALTER TABLE chit_monthly_cycles ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
UPDATE chit_monthly_cycles SET auction_winner_bid = 0 WHERE auction_winner_bid IS NULL;
UPDATE chit_monthly_cycles SET dividend_per_member = 0 WHERE dividend_per_member IS NULL;
UPDATE chit_monthly_cycles SET is_cycle_closed = FALSE WHERE is_cycle_closed IS NULL;
ALTER TABLE chit_monthly_cycles ALTER COLUMN auction_winner_bid SET DEFAULT 0;
ALTER TABLE chit_monthly_cycles ALTER COLUMN auction_winner_bid SET NOT NULL;
ALTER TABLE chit_monthly_cycles ALTER COLUMN dividend_per_member SET DEFAULT 0;
ALTER TABLE chit_monthly_cycles ALTER COLUMN dividend_per_member SET NOT NULL;
ALTER TABLE chit_monthly_cycles ALTER COLUMN is_cycle_closed SET DEFAULT FALSE;
ALTER TABLE chit_monthly_cycles ALTER COLUMN is_cycle_closed SET NOT NULL;
-- One cycle per month of a plan (also serves plan_id lookups, so the single-column index is redundant)
CREATE UNIQUE INDEX IF NOT EXISTS ux_cycle_plan_month ON chit_monthly_cycles (plan_id, monthly_count);
DROP INDEX IF EXISTS idx_monthly_plan_count;
DROP INDEX IF EXISTS idx_monthly_plan_id;
CREATE INDEX IF NOT EXISTS idx_cycle_plan_winner ON chit_monthly_cycles (plan_id, winner_user_id)
    WHERE winner_user_id IS NOT NULL;

-- ---------------------------------------------------------------- chit_enrollments
ALTER TABLE chit_enrollments ALTER COLUMN joined_date TYPE TIMESTAMPTZ USING joined_date AT TIME ZONE '${legacy-time-zone}';
UPDATE chit_enrollments SET joined_date = now() WHERE joined_date IS NULL;
ALTER TABLE chit_enrollments ALTER COLUMN joined_date SET NOT NULL;
ALTER TABLE chit_enrollments ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ;
UPDATE chit_enrollments SET created_at = joined_date WHERE created_at IS NULL;
ALTER TABLE chit_enrollments ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE chit_enrollments ADD COLUMN IF NOT EXISTS created_by BIGINT;
ALTER TABLE chit_enrollments ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ;
ALTER TABLE chit_enrollments ADD COLUMN IF NOT EXISTS updated_by BIGINT;
ALTER TABLE chit_enrollments ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
UPDATE chit_enrollments SET is_active = TRUE WHERE is_active IS NULL;
ALTER TABLE chit_enrollments ALTER COLUMN is_active SET DEFAULT TRUE;
ALTER TABLE chit_enrollments ALTER COLUMN is_active SET NOT NULL;
ALTER TABLE chit_enrollments ADD CONSTRAINT ck_enrollment_slot CHECK (slot_number >= 1) NOT VALID;

-- 1.x never stopped two members from taking the same slot. The earliest enrolment keeps the slot, later ones are
-- deactivated and listed in enrollment_slot_conflicts so an administrator can move those members to free slots.
CREATE TABLE IF NOT EXISTS enrollment_slot_conflicts (
    enrollment_id      BIGINT PRIMARY KEY,
    plan_id            BIGINT      NOT NULL,
    slot_number        INTEGER     NOT NULL,
    user_id            BIGINT      NOT NULL,
    kept_enrollment_id BIGINT      NOT NULL,
    detected_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
INSERT INTO enrollment_slot_conflicts (enrollment_id, plan_id, slot_number, user_id, kept_enrollment_id)
SELECT id, plan_id, slot_number, user_id, kept_id
FROM (SELECT id, plan_id, slot_number, user_id,
             first_value(id) OVER (PARTITION BY plan_id, slot_number ORDER BY joined_date, id) AS kept_id
      FROM chit_enrollments
      WHERE is_active) ranked
WHERE id <> kept_id
ON CONFLICT (enrollment_id) DO NOTHING;
UPDATE chit_enrollments SET is_active = FALSE
WHERE is_active AND id IN (SELECT enrollment_id FROM enrollment_slot_conflicts);
DO $$
DECLARE
    conflicts INTEGER;
BEGIN
    SELECT count(*) INTO conflicts FROM enrollment_slot_conflicts;
    IF conflicts > 0 THEN
        RAISE WARNING '% enrolment(s) shared an already-taken slot and were deactivated. Review table enrollment_slot_conflicts.', conflicts;
    END IF;
END
$$;

-- At most one active member per slot; two concurrent enrolments for the same slot cannot both succeed
CREATE UNIQUE INDEX IF NOT EXISTS ux_enrollment_plan_slot_active ON chit_enrollments (plan_id, slot_number)
    WHERE is_active;
CREATE INDEX IF NOT EXISTS idx_enrollment_plan_user_active ON chit_enrollments (plan_id, user_id) WHERE is_active;
DROP INDEX IF EXISTS idx_enrollment_plan_id;

-- ---------------------------------------------------------------- loans
ALTER TABLE loans ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE '${legacy-time-zone}';
UPDATE loans SET created_at = now() WHERE created_at IS NULL;
ALTER TABLE loans ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE loans ADD COLUMN IF NOT EXISTS created_by BIGINT;
ALTER TABLE loans ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ;
ALTER TABLE loans ADD COLUMN IF NOT EXISTS updated_by BIGINT;
ALTER TABLE loans ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE loans ADD COLUMN IF NOT EXISTS interest_deduction NUMERIC(38, 2);
ALTER TABLE loans ADD COLUMN IF NOT EXISTS leave_type VARCHAR(10) NOT NULL DEFAULT 'NONE';
UPDATE loans SET is_active = TRUE WHERE is_active IS NULL;
ALTER TABLE loans ALTER COLUMN is_active SET DEFAULT TRUE;
ALTER TABLE loans ALTER COLUMN is_active SET NOT NULL;
ALTER TABLE loans ADD CONSTRAINT ck_loans_values CHECK (
    principal_amount > 0 AND disbursed_amount > 0 AND daily_installment > 0 AND tenure_days >= 1) NOT VALID;
ALTER TABLE loans ADD CONSTRAINT ck_loans_leave_type CHECK (leave_type IN ('NONE', 'SUNDAY', 'WEEKEND'));
CREATE INDEX IF NOT EXISTS idx_loans_user_start ON loans (user_id, start_date DESC);
DROP INDEX IF EXISTS idx_loan_user_id;

-- ---------------------------------------------------------------- user_mirror
ALTER TABLE user_mirror ALTER COLUMN last_updated_at TYPE TIMESTAMPTZ USING last_updated_at AT TIME ZONE '${legacy-time-zone}';
UPDATE user_mirror SET last_updated_at = now() WHERE last_updated_at IS NULL;
ALTER TABLE user_mirror ALTER COLUMN last_updated_at SET NOT NULL;
UPDATE user_mirror SET is_active = TRUE WHERE is_active IS NULL;
ALTER TABLE user_mirror ALTER COLUMN is_active SET DEFAULT TRUE;
ALTER TABLE user_mirror ALTER COLUMN is_active SET NOT NULL;
CREATE INDEX IF NOT EXISTS idx_user_mirror_active_name ON user_mirror (is_active, full_name);
