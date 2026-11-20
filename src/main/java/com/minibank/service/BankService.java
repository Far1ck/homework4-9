package com.minibank.service;

import com.minibank.dto.OpenAccountRequest;
import com.minibank.dto.RegisterClientRequest;
import com.minibank.dto.TransferRequest;
import com.minibank.exception.AccountNotFountException;
import com.minibank.exception.ClientNotFoundException;
import com.minibank.exception.InsufficientFundsException;
import com.minibank.exception.NegativeBalanceException;
import com.minibank.model.Account;
import com.minibank.model.Client;
import com.minibank.repository.AccountRepository;
import com.minibank.repository.ClientRepository;
import com.minibank.repository.TransactionRepository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;

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

    public Client registerClient(RegisterClientRequest request) throws SQLException {
        return clientRepository.save(request.name(), request.email());
    }

    public Account openAccount(OpenAccountRequest request) throws SQLException, NegativeBalanceException, ClientNotFoundException {
        if (request.balance().compareTo(BigDecimal.ZERO) < 0) {
            throw new NegativeBalanceException("Creating an account with a negative balance is impossible");
        }
        if (!clientRepository.clientIsExist(request.clientId())) {
            throw new ClientNotFoundException("Client not found: " + request.clientId());
        }
        return accountRepository.save(request.clientId(), request.accountNumber(), request.balance());
    }

    public Account getAccount(Connection conn, Long accountId) throws SQLException, AccountNotFountException {
        return accountRepository.findByAccountId(conn, accountId).orElseThrow(() -> new AccountNotFountException("Account not found: " + accountId));
    }

    public Account getAccount(Long accountId) throws SQLException, AccountNotFountException {
        try (Connection conn = dataSource.getConnection()) {
            return accountRepository.findByAccountId(conn, accountId).orElseThrow(() -> new AccountNotFountException("Account not found: " + accountId));
        }
    }

    public Client getClient(Long clientId) throws SQLException, ClientNotFoundException {
        return clientRepository.findByClientId(clientId).orElseThrow(() -> new ClientNotFoundException("Client not found: " + clientId));
    }

    public void transfer(TransferRequest request) throws AccountNotFountException, NegativeBalanceException, InsufficientFundsException {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Account from;
                Account to;
                if (request.fromAccountId() < request.toAccountId()) {
                    from = getAccount(conn, request.fromAccountId());
                    to = getAccount(conn, request.toAccountId());
                } else {
                    to = getAccount(conn, request.toAccountId());
                    from = getAccount(conn, request.fromAccountId());
                }
                if (!(request.amount().compareTo(BigDecimal.ZERO) > 0)) {
                    throw new NegativeBalanceException("The transfer amount must be greater than 0. Current amount: " + request.amount());
                }
                if (from.getBalance().compareTo(request.amount()) < 0) {
                    throw new InsufficientFundsException("Insufficient funds on account " + request.fromAccountId());
                }
                from.setBalance(from.getBalance().subtract(request.amount()));
                accountRepository.updateBalance(conn, from);
                recordTransaction(conn, request.fromAccountId(), request.amount().negate(), "TRANSFER_OUT");

                to.setBalance(to.getBalance().add(request.amount()));
                accountRepository.updateBalance(conn, to);
                recordTransaction(conn, request.toAccountId(), request.amount(), "TRANSFER_IN");
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
