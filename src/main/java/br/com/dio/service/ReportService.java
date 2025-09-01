package br.com.dio.service;

import lombok.AllArgsConstructor;

import java.sql.Connection;
import java.sql.SQLException;

@AllArgsConstructor
public class ReportService {

    private final Connection connection;

    // Relatório 1+2: tempos por coluna e tempo total até conclusão (somente cards concluídos)
    public void printLeadTimeReport(final Long boardId) throws SQLException {
        // Para cada card concluído, calculamos:
        // - tempo total = entered_at(FINAL) - entered_at(INITIAL)
        // - por coluna = SUM(left_at - entered_at) para colunas antes da FINAL
        var sqlCardsFinalizados = """
            SELECT c.id AS card_id, c.title,
                   MIN(CASE WHEN bc.kind='INITIAL' THEN cm.entered_at END) AS initial_entered_at,
                   MIN(CASE WHEN bc.kind='FINAL' THEN cm.entered_at END)   AS final_entered_at
              FROM CARDS c
              JOIN BOARDS_COLUMNS bc ON bc.id = c.board_column_id OR bc.board_id = bc.board_id
              JOIN BOARDS b ON b.id = bc.board_id
              JOIN CARD_MOVES cm ON cm.card_id = c.id
             WHERE bc.board_id = ?
               AND c.id IN (
                   SELECT cm2.card_id
                     FROM CARD_MOVES cm2
                     JOIN BOARDS_COLUMNS bc2 ON bc2.id = cm2.board_column_id
                    WHERE bc2.board_id = ?
                      AND bc2.kind = 'FINAL'
               )
             GROUP BY c.id, c.title
             ORDER BY c.id
        """;

        try (var cardSt = connection.prepareStatement(sqlCardsFinalizados)) {
            cardSt.setLong(1, boardId);
            cardSt.setLong(2, boardId);
            var rs = cardSt.executeQuery();
            while (rs.next()) {
                var cardId = rs.getLong("card_id");
                var title  = rs.getString("title");
                var initial = rs.getTimestamp("initial_entered_at");
                var fin     = rs.getTimestamp("final_entered_at");

                System.out.printf("\nCARD %d - %s\n", cardId, title);
                if (initial != null && fin != null) {
                    long totalSecs = (fin.getTime() - initial.getTime()) / 1000;
                    System.out.printf("Tempo total até conclusão: %ds\n", totalSecs);
                } else {
                    System.out.println("Dados insuficientes para calcular tempo total.");
                }

                // Quebra por coluna (antes da FINAL)
                var sqlPorColuna = """
                    SELECT bc.name, bc.kind,
                           SUM(TIMESTAMPDIFF(SECOND, cm.entered_at, COALESCE(cm.left_at, cm.entered_at))) AS secs
                      FROM CARD_MOVES cm
                      JOIN BOARDS_COLUMNS bc ON bc.id = cm.board_column_id
                     WHERE cm.card_id = ?
                       AND bc.board_id = ?
                       AND bc.kind <> 'FINAL'
                     GROUP BY bc.name, bc.kind
                     ORDER BY MIN(bc.`order`)
                """;
                try (var st2 = connection.prepareStatement(sqlPorColuna)) {
                    st2.setLong(1, cardId);
                    st2.setLong(2, boardId);
                    var rs2 = st2.executeQuery();
                    System.out.println("Tempo por coluna (s):");
                    while (rs2.next()) {
                        System.out.printf("- %s (%s): %d\n",
                                rs2.getString("name"),
                                rs2.getString("kind"),
                                rs2.getLong("secs"));
                    }
                }
            }
        }
    }

    // Relatório 3: bloqueios (duração e justificativas)
    public void printBlocksReport(final Long boardId) throws SQLException {
        var sql = """
            SELECT c.id AS card_id, c.title,
                   b.blocked_at, b.block_reason,
                   b.unblocked_at, b.unblock_reason,
                   TIMESTAMPDIFF(SECOND, b.blocked_at, COALESCE(b.unblocked_at, NOW())) AS secs
              FROM CARDS c
              JOIN BOARDS_COLUMNS bc ON bc.id = c.board_column_id
              JOIN BLOCKS b ON b.card_id = c.id
             WHERE bc.board_id = ?
             ORDER BY c.id, b.blocked_at
        """;
        try (var st = connection.prepareStatement(sql)) {
            st.setLong(1, boardId);
            var rs = st.executeQuery();

            long currentCard = -1;
            long totalSecs = 0;
            while (rs.next()) {
                var cardId = rs.getLong("card_id");
                if (cardId != currentCard) {
                    if (currentCard != -1) {
                        System.out.printf("Total bloqueado (s): %d\n\n", totalSecs);
                        totalSecs = 0;
                    }
                    currentCard = cardId;
                    System.out.printf("\nCARD %d - %s\n", cardId, rs.getString("title"));
                }
                var secs = rs.getLong("secs");
                totalSecs += secs;
                System.out.printf("- %s -> %s | %ds | motivo: %s | desbloqueio: %s\n",
                        rs.getTimestamp("blocked_at"),
                        rs.getTimestamp("unblocked_at"),
                        secs,
                        rs.getString("block_reason"),
                        rs.getString("unblock_reason"));
            }
            if (currentCard != -1) {
                System.out.printf("Total bloqueado (s): %d\n\n", totalSecs);
            }
        }
    }
}
