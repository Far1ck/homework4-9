package com.minibank.repository;

import com.minibank.model.Account;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class AccountRepository {
    private final DataSource dataSource;

    public AccountRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Account save(long clientId, String accountNumber, BigDecimal balance) throws SQLException {
        String sql = "INSERT INTO accounts (client_id, account_number, balance) VALUES (?, ?, ?) RETURNING id";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, clientId);
            stmt.setString(2, accountNumber);
            stmt.setBigDecimal(3, balance);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Account(
                            rs.getLong("id"),
                            clientId,
                            accountNumber,
                            balance
                    );
                }
                throw new SQLException("Не удалось сохранить счет");
            }
        }
    }

    public Account updateBalance(Connection conn, Account account) throws SQLException {
        String sql = "UPDATE accounts SET balance = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(2, account.getId());
            stmt.setBigDecimal(1, account.getBalance());
            int rs = stmt.executeUpdate();
            if (rs == 1) {
                return account;
            }
            throw new SQLException("Не удалось обновить счет");
        }
    }

    public boolean accountIsExist(Connection conn, long accountId) throws SQLException {
        String sql = "SELECT id, client_id, account_number, balance FROM accounts WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, accountId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public Optional<Account> findByAccountId(Connection conn, long accountId) throws SQLException {
        String sql = "SELECT id, client_id, account_number, balance FROM accounts WHERE id = ? FOR UPDATE";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, accountId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Account(
                            rs.getLong("id"),
                            rs.getLong("client_id"),
                            rs.getString("account_number"),
                            rs.getBigDecimal("balance")
                    ));
                }
            }
        }
        return Optional.empty();
    }
}
