package br.com.dio.service;

import br.com.dio.dto.BoardColumnInfoDTO;
import br.com.dio.exception.CardBlockedException;
import br.com.dio.exception.CardFinishedException;
import br.com.dio.exception.EntityNotFoundException;
import br.com.dio.persistence.dao.BlockDAO;
import br.com.dio.persistence.dao.CardDAO;
import br.com.dio.persistence.entity.CardEntity;
import lombok.AllArgsConstructor;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static br.com.dio.persistence.entity.BoardColumnKindEnum.CANCEL;
import static br.com.dio.persistence.entity.BoardColumnKindEnum.FINAL;


@AllArgsConstructor
public class CardService {

    private final Connection connection;

    public CardEntity create(final CardEntity entity) throws SQLException {
        try {
            var dao = new CardDAO(connection);
            dao.insert(entity);
            connection.commit();
            return entity;
        } catch (SQLException ex){
            connection.rollback();
            throw ex;
        }
    }

    // NOVO: criar card já abrindo o movimento inicial
public Long create(final String title,
                   final String description,
                   final Long initialColumnId) throws SQLException {
    try {
        var card = new CardEntity();
        card.setTitle(title);
        card.setDescription(description);

        var col = new br.com.dio.persistence.entity.BoardColumnEntity();
        col.setId(initialColumnId);
        card.setBoardColumn(col);

        var cardDAO = new br.com.dio.persistence.dao.CardDAO(connection);
        var moveDAO = new br.com.dio.persistence.dao.CardMoveDAO(connection);

        // 1) insere e obtém o ID gerado
        Long cardId = cardDAO.insertReturningId(card);

        // 2) abre o movimento inicial (entered_at = now)
        moveDAO.openMove(cardId, initialColumnId);

        connection.commit();
        return cardId;
    } catch (SQLException ex) {
        connection.rollback();
        throw ex;
    }
}


    public void moveToNextColumn(final Long cardId,
                             final List<br.com.dio.dto.BoardColumnInfoDTO> boardColumnsInfo) throws SQLException {
    try {
        var dao = new br.com.dio.persistence.dao.CardDAO(connection);
        var optional = dao.findById(cardId);
        var dto = optional.orElseThrow(
                () -> new br.com.dio.exception.EntityNotFoundException(
                        "O card de id %s não foi encontrado".formatted(cardId))
        );
        if (dto.blocked()) {
            var message = "O card %s está bloqueado, é necesário desbloquea-lo para mover".formatted(cardId);
            throw new br.com.dio.exception.CardBlockedException(message);
        }

        var currentColumn = boardColumnsInfo.stream()
                .filter(bc -> bc.id().equals(dto.columnId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("O card informado pertence a outro board"));

        if (currentColumn.kind().equals(br.com.dio.persistence.entity.BoardColumnKindEnum.FINAL)) {
            throw new br.com.dio.exception.CardFinishedException("O card já foi finalizado");
        }

        var nextColumn = boardColumnsInfo.stream()
                .filter(bc -> bc.order() == currentColumn.order() + 1)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("O card está cancelado"));

        // move no banco
        dao.moveToColumn(nextColumn.id(), cardId);

        // FECHA/ABRE movimentos
        var moveDAO = new br.com.dio.persistence.dao.CardMoveDAO(connection);
        moveDAO.closeOpenMove(cardId);             // left_at = now na coluna anterior
        moveDAO.openMove(cardId, nextColumn.id()); // entered_at = now na nova coluna

        connection.commit();
    } catch (SQLException ex) {
        connection.rollback();
        throw ex;
    }
}

public void cancel(final Long cardId,
                   final Long cancelColumnId,
                   final List<br.com.dio.dto.BoardColumnInfoDTO> boardColumnsInfo) throws SQLException {
    try {
        var dao = new br.com.dio.persistence.dao.CardDAO(connection);
        var optional = dao.findById(cardId);
        var dto = optional.orElseThrow(
                () -> new br.com.dio.exception.EntityNotFoundException(
                        "O card de id %s não foi encontrado".formatted(cardId))
        );
        if (dto.blocked()) {
            var message = "O card %s está bloqueado, é necesário desbloquea-lo para mover".formatted(cardId);
            throw new br.com.dio.exception.CardBlockedException(message);
        }

        var currentColumn = boardColumnsInfo.stream()
                .filter(bc -> bc.id().equals(dto.columnId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("O card informado pertence a outro board"));

        if (currentColumn.kind().equals(br.com.dio.persistence.entity.BoardColumnKindEnum.FINAL)) {
            throw new br.com.dio.exception.CardFinishedException("O card já foi finalizado");
        }

        // sanity check: existe uma próxima coluna (evita cancelar a partir da final, etc.)
        boardColumnsInfo.stream()
                .filter(bc -> bc.order() == currentColumn.order() + 1)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("O card está cancelado"));

        // move no banco
        dao.moveToColumn(cancelColumnId, cardId);

        // FECHA/ABRE movimentos
        var moveDAO = new br.com.dio.persistence.dao.CardMoveDAO(connection);
        moveDAO.closeOpenMove(cardId);                 // fecha o período aberto
        moveDAO.openMove(cardId, cancelColumnId);      // abre entrada na coluna de cancelamento

        connection.commit();
    } catch (SQLException ex) {
        connection.rollback();
        throw ex;
   

    public void block(final Long id, final String reason, final List<BoardColumnInfoDTO> boardColumnsInfo) throws SQLException {
        try{
            var dao = new CardDAO(connection);
            var optional = dao.findById(id);
            var dto = optional.orElseThrow(
                    () -> new EntityNotFoundException("O card de id %s não foi encontrado".formatted(id))
            );
            if (dto.blocked()){
                var message = "O card %s já está bloqueado".formatted(id);
                throw new CardBlockedException(message);
            }
            var currentColumn = boardColumnsInfo.stream()
                    .filter(bc -> bc.id().equals(dto.columnId()))
                    .findFirst()
                    .orElseThrow();
            if (currentColumn.kind().equals(FINAL) || currentColumn.kind().equals(CANCEL)){
                var message = "O card está em uma coluna do tipo %s e não pode ser bloqueado"
                        .formatted(currentColumn.kind());
                throw new IllegalStateException(message);
            }
            var blockDAO = new BlockDAO(connection);
            blockDAO.block(reason, id);
            connection.commit();
        }catch (SQLException ex) {
            connection.rollback();
            throw ex;
        }
    }

    public void unblock(final Long id, final String reason) throws SQLException {
        try{
            var dao = new CardDAO(connection);
            var optional = dao.findById(id);
            var dto = optional.orElseThrow(
                    () -> new EntityNotFoundException("O card de id %s não foi encontrado".formatted(id))
            );
            if (!dto.blocked()){
                var message = "O card %s não está bloqueado".formatted(id);
                throw new CardBlockedException(message);
            }
            var blockDAO = new BlockDAO(connection);
            blockDAO.unblock(reason, id);
            connection.commit();
        }catch (SQLException ex) {
            connection.rollback();
            throw ex;
        }
    }

}
