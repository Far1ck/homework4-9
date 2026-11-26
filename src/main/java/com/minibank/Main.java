package com.minibank;

import com.minibank.controller.AccountController;
import com.minibank.controller.ClientController;
import com.minibank.controller.TransferController;
import com.minibank.db.JpaUtil;
import com.minibank.db.MigrationRunner;
import com.minibank.repository.AccountRepository;
import com.minibank.repository.ClientRepository;
import com.minibank.repository.TransactionRepository;
import com.minibank.service.BankService;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class Main {

    public static void main(String[] args) throws IOException {
        Runtime.getRuntime().addShutdownHook(new Thread(JpaUtil::close));
        MigrationRunner.runMigrations(
                "jdbc:postgresql://localhost:5432/minibank",
                "globus_student",
                "secret");
        JpaUtil.init();
        BankService bankService = new BankService(
                new ClientRepository(),
                new AccountRepository(),
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
