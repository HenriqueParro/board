--liquibase formatted sql
--changeset leo:20250831-001
--comment: add created_at to CARDS

ALTER TABLE CARDS
  ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

--rollback ALTER TABLE CARDS DROP COLUMN created_at;
