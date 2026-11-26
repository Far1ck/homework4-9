package com.minibank.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.minibank.controller.utils.HttpUtils;
import com.minibank.dto.TransferRequest;
import com.minibank.exception.AccountNotFountException;
import com.minibank.exception.InsufficientFundsException;
import com.minibank.exception.NegativeBalanceException;
import com.minibank.service.BankService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

public class TransferController implements HttpHandler {
    private static final Logger log = LoggerFactory.getLogger(TransferController.class);
    private static final String BASE_PATH = "/transfer";

    private final BankService bankService;

    public TransferController(BankService bankService) {
        this.bankService = bankService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if (!HttpUtils.isExactPath(exchange, BASE_PATH)) {
                HttpUtils.sendError(exchange, 404, "Endpoint not found");
                return;
            }

            if (!"POST".equals(exchange.getRequestMethod())) {
                HttpUtils.sendError(exchange, 405, "A POST request is expected");
                return;
            }

            TransferRequest req = HttpUtils.readJson(exchange, TransferRequest.class);
            bankService.transfer(req);
            HttpUtils.sendJson(exchange, 200, Map.of("status", "The funds have been successfully transferred"));

        } catch (InsufficientFundsException | NegativeBalanceException | IllegalArgumentException e) {
            log.warn(e.getMessage());
            HttpUtils.sendError(exchange, 400, e.getMessage());
        } catch (AccountNotFountException e) {
            log.warn(e.getMessage());
            HttpUtils.sendError(exchange, 404, e.getMessage());
        } catch (JsonProcessingException e) {
            log.warn("Invalid JSON: {}", e.getOriginalMessage());
            HttpUtils.sendError(exchange, 400, "Invalid JSON: " + e.getOriginalMessage());
        } catch (Exception e) {
            log.error("Unexpected error while handling transfer request", e);
            HttpUtils.sendError(exchange, 500, "Internal Server Error");
        } finally {
            exchange.close();
        }
    }
}
