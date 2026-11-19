package com.minibank.service;

import com.minibank.model.Account;
import com.minibank.model.Client;
import com.minibank.repository.AccountRepository;
import com.minibank.repository.ClientRepository;
import com.minibank.repository.TransactionRepository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * MiniBank — "MVP", который наспех собрал предыдущий разработчик:
 * вся логика в одном классе, все данные — в HashMap, персистентности
 * нет вообще (перезапуск процесса — и всё пропало).
 *
 * ПРОБЛЕМА (найдёшь и исправишь, когда дойдёт очередь — модуль 7.1,
 * когда появится @Transactional): в transfer() проверка существования
 * счёта-получателя происходит ПОСЛЕ списания денег со счёта-
 * отправителя. Если счёт-получатель не существует, деньги уже спишутся
 * с первого счёта и "исчезнут" — их некуда вернуть, потому что метод
 * прерывается исключением до зачисления.
 *
 * Эта проблема не решается на HashMap "в лоб" без искусственных
 * костылей (можно вручную откатывать баланс в catch, но это не
 * настоящее решение) — она по-настоящему решается только когда
 * появится транзакционность (сначала в БД — ACID, модуль 4, а затем
 * в коде — @Transactional, модуль 7.1). До этого момента баг просто
 * существует и это нормально: то, что появится в модуле 7.1, — как
 * раз тот инструмент, который проблему полностью закрывает.
 */
public class BankService {

    private final DataSource dataSource;
    private final ClientRepository clientRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public BankService(DataSource dataSource,
                       ClientRepository clientRepository,
                       AccountRepository accountRepository,
                       TransactionRepository transactionRepository) {
        this.dataSource = dataSource;
        this.clientRepository = clientRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    public Client registerClient(String name, String email) throws SQLException {
        return clientRepository.save(name, email);
    }

    public Account openAccount(Long clientId, String accountNumber) throws SQLException {
        if (!clientRepository.clientIsExist(clientId)) {
            throw new IllegalArgumentException("Client not found: " + clientId);
        }
        return accountRepository.save(clientId, accountNumber, BigDecimal.ZERO);
    }

    public Account getAccount(Connection conn, Long accountId) throws SQLException {
        Account account = accountRepository.findByAccountId(conn, accountId).orElse(null);
        if (account == null) {
            throw new IllegalArgumentException("Account not found: " + accountId);
        }
        return account;
    }

    public Account getAccount(Long accountId) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            Account account = accountRepository.findByAccountId(conn, accountId).orElse(null);
            if (account == null) {
                throw new IllegalArgumentException("Account not found: " + accountId);
            }
            return account;
        }
    }

    public Client getClient(Long clientId) throws SQLException {
        Client client = clientRepository.findByClientId(clientId).orElse(null);
        if (client == null) {
            throw new IllegalArgumentException("Client not found: " + clientId);
        }
        return client;
    }

    public void transfer(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Account from = getAccount(conn, fromAccountId);

                // Списываем деньги ДО того, как проверили существование счёта-
                // получателя — если toAccountId не существует, деньги уже
                // списаны и потеряны.
                if (from.getBalance().compareTo(amount) < 0) {
                    throw new IllegalStateException("Insufficient funds on account " + fromAccountId);
                }
                from.setBalance(from.getBalance().subtract(amount));
                accountRepository.updateBalance(conn, from);
                recordTransaction(conn, fromAccountId, amount.negate(), "TRANSFER_OUT");

                Account to = getAccount(conn, toAccountId); // если бросит исключение — деньги уже пропали
                to.setBalance(to.getBalance().add(amount));
                accountRepository.updateBalance(conn, to);
                recordTransaction(conn, toAccountId, amount, "TRANSFER_IN");
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error during transfer", e);
        }
    }

    private void recordTransaction(Connection connection, Long accountId, BigDecimal amount, String type) throws SQLException {
        transactionRepository.save(connection, accountId, amount, type);
    }
}
