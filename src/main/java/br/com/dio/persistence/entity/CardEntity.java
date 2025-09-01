package br.com.dio.persistence.entity;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class CardEntity {

    private Long id;
    private String title;
    private String description;

    // coluna atual onde o card está
    private BoardColumnEntity boardColumn = new BoardColumnEntity();

    // novo: data/hora de criação do card (preenchido pelo BD via DEFAULT CURRENT_TIMESTAMP)
    private OffsetDateTime createdAt;
}
