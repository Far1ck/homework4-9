package com.minibank.service;

import com.minibank.db.JpaUtil;
import com.minibank.dto.OpenAccountRequest;
import com.minibank.dto.RegisterClientRequest;
import com.minibank.dto.TransferRequest;
import com.minibank.entity.Transaction;
import com.minibank.exception.AccountNotFountException;
import com.minibank.exception.ClientNotFoundException;
import com.minibank.exception.InsufficientFundsException;
import com.minibank.exception.NegativeBalanceException;
import com.minibank.entity.Account;
import com.minibank.entity.Client;
import com.minibank.repository.AccountRepository;
import com.minibank.repository.ClientRepository;
import com.minibank.repository.TransactionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class BankService {

    private final ClientRepository clientRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public BankService(ClientRepository clientRepository,
                       AccountRepository accountRepository,
                       TransactionRepository transactionRepository) {
        this.clientRepository = clientRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    public Client registerClient(RegisterClientRequest request) {
        return clientRepository.save(new Client(null, request.name(), request.email()));
    }

    public List<Client> getClients() {
        return clientRepository.findAllClients();
    }

    public Account openAccount(OpenAccountRequest request) throws NegativeBalanceException, ClientNotFoundException {
        if (request.balance().compareTo(BigDecimal.ZERO) < 0) {
            throw new NegativeBalanceException("Creating an account with a negative balance is impossible");
        }
        Client client = clientRepository.findByClientId(request.clientId()).orElseThrow(() -> new ClientNotFoundException("Client not found: " + request.clientId()));
        return accountRepository.save(new Account(null, client, request.accountNumber(), request.balance()));
    }

    public Account getAccount(EntityManager em, Long accountId) throws AccountNotFountException {
        return accountRepository.findByIdForUpdate(em, accountId).orElseThrow(() -> new AccountNotFountException("Account not found: " + accountId));
    }

    public Account getAccount(Long accountId) throws AccountNotFountException {
        return accountRepository.findById(accountId).orElseThrow(() -> new AccountNotFountException("Account not found: " + accountId));
    }

    public Client getClient(Long clientId) throws ClientNotFoundException {
        return clientRepository.findByClientId(clientId).orElseThrow(() -> new ClientNotFoundException("Client not found: " + clientId));
    }

    public void transfer(TransferRequest request) throws AccountNotFountException, NegativeBalanceException, InsufficientFundsException {
        if (request.fromAccountId() == null || request.toAccountId() == null || request.amount() == null) {
            throw new IllegalArgumentException("The request cannot have null fields");
        }
        if (Objects.equals(request.fromAccountId(), request.toAccountId())) {
            throw new IllegalArgumentException("Cannot transfer money to the same account");
        }
        if (!(request.amount().compareTo(BigDecimal.ZERO) > 0)) {
            throw new NegativeBalanceException("The transfer amount must be greater than 0. Current amount: " + request.amount());
        }
        EntityManager em = JpaUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Long firstId = Math.min(request.fromAccountId(), request.toAccountId());
            Long secondId = Math.max(request.fromAccountId(), request.toAccountId());

            Account first = getAccount(em, firstId);
            Account second = getAccount(em, secondId);

            Account from = Objects.equals(firstId, request.fromAccountId()) ? first : second;
            Account to = Objects.equals(firstId, request.toAccountId()) ? first : second;


            if (from.getBalance().compareTo(request.amount()) < 0) {
                throw new InsufficientFundsException("Insufficient funds on account " + request.fromAccountId());
            }

            LocalDateTime now = LocalDateTime.now();

            from.setBalance(from.getBalance().subtract(request.amount()));
            recordTransaction(em, new Transaction(null, from, request.amount().negate(), "TRANSFER_OUT", now));
            to.setBalance(to.getBalance().add(request.amount()));
            recordTransaction(em, new Transaction(null, to, request.amount(), "TRANSFER_IN", now));

            tx.commit();
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    private void recordTransaction(EntityManager em, Transaction transaction) {
        transactionRepository.save(em, transaction);
    }
}
