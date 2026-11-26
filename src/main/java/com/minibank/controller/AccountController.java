package com.minibank.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.minibank.controller.utils.HttpUtils;
import com.minibank.dto.OpenAccountRequest;
import com.minibank.exception.AccountNotFountException;
import com.minibank.exception.ClientNotFoundException;
import com.minibank.exception.InvalidPathException;
import com.minibank.entity.Account;
import com.minibank.service.BankService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class AccountController implements HttpHandler {
    private static final Logger log = LoggerFactory.getLogger(AccountController.class);
    private static final String BASE_PATH = "/account";
    private final BankService bankService;

    public AccountController(BankService bankService) {
        this.bankService = bankService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            String method = exchange.getRequestMethod();

            if ("POST".equals(method)) {
                if (!HttpUtils.isExactPath(exchange, BASE_PATH)) {
                    HttpUtils.sendError(exchange, 404, "Endpoint not found");
                    return;
                }

                OpenAccountRequest req = HttpUtils.readJson(exchange, OpenAccountRequest.class);
                Account account = bankService.openAccount(req);
                HttpUtils.sendJson(exchange, 201, account);

            } else if ("GET".equals(method)) {
                Long accountId = HttpUtils.extractId(exchange, BASE_PATH);
                Account account = bankService.getAccount(accountId);
                HttpUtils.sendJson(exchange, 200, account);
            } else {
                HttpUtils.sendError(exchange, 405, "Method not supported");
            }
        } catch (ClientNotFoundException | AccountNotFountException | InvalidPathException e) {
            log.warn(e.getMessage());
            HttpUtils.sendError(exchange, 404, e.getMessage());
        } catch (JsonProcessingException e) {
            log.warn("Invalid JSON: {}", e.getOriginalMessage());
            HttpUtils.sendError(exchange, 400, "Invalid JSON: " + e.getOriginalMessage());
        } catch (Exception e) {
            log.error("Unexpected error while handling transfer request", e);
            HttpUtils.sendError(exchange, 500, "Internal Server Error");
        }
    }
}
