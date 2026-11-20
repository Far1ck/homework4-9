package com.minibank;

import com.minibank.db.ConnectionPool;
import com.minibank.db.MigrationRunner;
import com.minibank.model.Account;
import com.minibank.model.Client;
import com.minibank.repository.AccountRepository;
import com.minibank.repository.ClientRepository;
import com.minibank.repository.TransactionRepository;
import com.minibank.service.BankService;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;

public class Main {

    public static void main(String[] args) throws SQLException {
        Runtime.getRuntime().addShutdownHook(new Thread(ConnectionPool::close));
        DataSource dataSource = ConnectionPool.getDataSource();
        MigrationRunner.runMigrations(dataSource);
        AccountRepository accountRepository = new AccountRepository(dataSource);
        BankService bank = new BankService(
                dataSource,
                new ClientRepository(dataSource),
                accountRepository,
                new TransactionRepository()
        );

        Client client = bank.registerClient("Ivan Petrov", "ivan@example.com");
        Account account1 = bank.openAccount(client.getId(), "ACC-001");
        Account account2 = bank.openAccount(client.getId(), "ACC-002");

        // Пополняем account1 "вручную" переводом с самого себя не получится —
        // для демо просто задаём баланс напрямую через первый перевод
        // от несуществующего служебного счёта нет смысла эмулировать,
        // поэтому просто выставим баланс через пакетный доступ в демо:
        account1.setBalance(new BigDecimal("1000.00"));
        try (Connection conn = dataSource.getConnection()) {
            accountRepository.updateBalance(conn, account1);
        }

        System.out.println("=== Обычный перевод ===");
        bank.transfer(account1.getId(), account2.getId(), new BigDecimal("100.00"));
        System.out.println("account1 balance: " + bank.getAccount(account1.getId()).getBalance());
        System.out.println("account2 balance: " + bank.getAccount(account2.getId()).getBalance());

        System.out.println();
        System.out.println("=== Перевод на несуществующий счёт (баг) ===");
        BigDecimal balanceBefore = bank.getAccount(account1.getId()).getBalance();
        try {
            bank.transfer(account1.getId(), 999L, new BigDecimal("50.00"));
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка (как и ожидалось): " + e.getMessage());
        }
        BigDecimal balanceAfter = bank.getAccount(account1.getId()).getBalance();
        System.out.println("Баланс до попытки перевода: " + balanceBefore);
        System.out.println("Баланс после попытки перевода: " + balanceAfter);
        System.out.println("Деньги пропали: " + (balanceAfter.compareTo(balanceBefore) < 0));
    }
}
