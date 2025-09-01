--liquibase formatted sql
--changeset leo:20250831-002
--comment: create CARD_MOVES to track per-column dwell times

CREATE TABLE CARD_MOVES (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  card_id BIGINT NOT NULL,
  board_column_id BIGINT NOT NULL,
  entered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  left_at TIMESTAMP NULL,
  CONSTRAINT card_moves__cards_fk FOREIGN KEY (card_id) REFERENCES CARDS(id) ON DELETE CASCADE,
  CONSTRAINT card_moves__columns_fk FOREIGN KEY (board_column_id) REFERENCES BOARDS_COLUMNS(id) ON DELETE CASCADE,
  INDEX idx_card_moves_card (card_id),
  INDEX idx_card_moves_column (board_column_id),
  INDEX idx_card_moves_open (card_id, left_at)
);

--rollback DROP TABLE CARD_MOVES;
