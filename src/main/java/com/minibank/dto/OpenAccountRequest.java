package com.minibank.dto;

import java.math.BigDecimal;

public record OpenAccountRequest(Long clientId, String accountNumber, BigDecimal balance) {
}
