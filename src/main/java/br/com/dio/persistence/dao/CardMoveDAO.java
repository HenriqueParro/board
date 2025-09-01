package br.com.dio.persistence.dao;

import br.com.dio.persistence.entity.CardMoveEntity;
import lombok.AllArgsConstructor;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.OffsetDateTime;

import static br.com.dio.persistence.converter.OffsetDateTimeConverter.toOffsetDateTime;
import static br.com.dio.persistence.converter.OffsetDateTimeConverter.toTimestamp;

@AllArgsConstructor
public class CardMoveDAO {

    private final Connection connection;

    // Abre um período de permanência do card na coluna (entered_at = now)
    public void openMove(final Long cardId, final Long boardColumnId) throws SQLException {
        var sql = """
                INSERT INTO CARD_MOVES(card_id, board_column_id, entered_at)
                VALUES(?, ?, ?)
                """;
        try (var st = connection.prepareStatement(sql)) {
            st.setLong(1, cardId);
            st.setLong(2, boardColumnId);
            st.setTimestamp(3, toTimestamp(OffsetDateTime.now()));
            st.executeUpdate();
        }
    }

    // Fecha o período “em aberto” do card (left_at = now)
    public void closeOpenMove(final Long cardId) throws SQLException {
        var sql = """
                UPDATE CARD_MOVES
                   SET left_at = ?
                 WHERE card_id = ?
                   AND left_at IS NULL
                """;
        try (var st = connection.prepareStatement(sql)) {
            st.setTimestamp(1, toTimestamp(OffsetDateTime.now()));
            st.setLong(2, cardId);
            st.executeUpdate();
        }
    }
}
