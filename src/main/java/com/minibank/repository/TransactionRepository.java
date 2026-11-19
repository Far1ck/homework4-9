package com.minibank.repository;

import com.minibank.model.Transaction;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TransactionRepository {

    public Transaction save(Connection conn, long accountId, BigDecimal amount, String type) throws SQLException {
        String sql = "INSERT INTO transactions (account_id, amount, type) VALUES (?, ?, ?) RETURNING id, transaction_date";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, accountId);
            stmt.setBigDecimal(2, amount);
            stmt.setString(3, type);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Transaction(
                            rs.getLong("id"),
                            accountId,
                            amount,
                            type,
                            rs.getTimestamp("transaction_date").toLocalDateTime()
                    );
                }
                throw new SQLException("Не удалось сохранить транзакцию");
            }
        }
    }
}
