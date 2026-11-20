package com.minibank.controller.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minibank.exception.InvalidPathException;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

public class HttpUtils {
    public static final ObjectMapper MAPPER = new ObjectMapper();


    public static <T> T readJson(HttpExchange exchange, Class<T> clazz) throws IOException {
        return MAPPER.readValue(exchange.getRequestBody(), clazz);
    }

    public static void sendJson(HttpExchange exchange, int statusCode, Object data) throws IOException {
        byte[] bytes = MAPPER.writeValueAsBytes(data);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    public static void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        sendJson(exchange, statusCode, Map.of("error", message));
    }

    public static boolean isExactPath(HttpExchange exchange, String basePath) {
        String path = exchange.getRequestURI().getPath();
        return path.equals(basePath) || path.equals(basePath + "/");
    }

    public static Long extractId(HttpExchange exchange, String basePath) throws InvalidPathException {
        String path = exchange.getRequestURI().getPath();
        if (!path.matches(basePath + "/\\d+")) {
            throw new InvalidPathException("Incorrect ID or path");
        }
        String idPart = path.substring(basePath.length() + 1);
        return Long.parseLong(idPart);
    }
}
