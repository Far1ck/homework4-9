package com.minibank;

import com.minibank.controller.AccountController;
import com.minibank.controller.ClientController;
import com.minibank.controller.TransferController;
import com.minibank.db.ConnectionPool;
import com.minibank.db.MigrationRunner;
import com.minibank.repository.AccountRepository;
import com.minibank.repository.ClientRepository;
import com.minibank.repository.TransactionRepository;
import com.minibank.service.BankService;
import com.sun.net.httpserver.HttpServer;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class Main {

    public static void main(String[] args) throws IOException {
        Runtime.getRuntime().addShutdownHook(new Thread(ConnectionPool::close));
        DataSource dataSource = ConnectionPool.getDataSource();
        MigrationRunner.runMigrations(dataSource);
        BankService bankService = new BankService(
                dataSource,
                new ClientRepository(dataSource),
                new AccountRepository(dataSource),
                new TransactionRepository()
        );

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/client", new ClientController(bankService));
        server.createContext("/account", new AccountController(bankService));
        server.createContext("/transfer", new TransferController(bankService));

        server.setExecutor(Executors.newCachedThreadPool());
        server.start();

        System.out.println("The server is running on port 8080");
    }
}
