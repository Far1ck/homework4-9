package com.minibank.repository;

import com.minibank.model.Client;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class ClientRepository {
    private final DataSource dataSource;

    public ClientRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Client save(String name, String email) throws SQLException {
        String sql = "INSERT INTO clients (name, email) VALUES (?, ?) RETURNING id";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Client(
                            rs.getLong("id"),
                            name,
                            email
                    );
                }
                throw new SQLException("Не удалось сохранить клиента");
            }
        }
    }

    public Optional<Client> findByClientId(long clientId) throws SQLException {
        String sql = "SELECT id, name, email FROM clients WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, clientId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Client(
                            rs.getLong("id"),
                            rs.getString("name"),
                            rs.getString("email")
                    ));
                }
            }
        }
        return Optional.empty();
    }

    public boolean clientIsExist(long clientId) throws SQLException {
        String sql = "SELECT id, name, email FROM clients WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, clientId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }
}
