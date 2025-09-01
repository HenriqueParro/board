package br.com.dio.persistence.entity;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class CardMoveEntity {
    private Long id;
    private Long cardId;
    private Long boardColumnId;
    private OffsetDateTime enteredAt;
    private OffsetDateTime leftAt;
}
