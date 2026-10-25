package com.billing.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Payer(String payerId, String name, BigDecimal allowedChargeRate,
                    BigDecimal payerShareRate, int currencyScale,
                    RoundingMode roundingMode) {

    public Payer {
        Objects.requireNonNull(payerId, "Payer ID is required");
        Objects.requireNonNull(name, "Payer name is required");
        Objects.requireNonNull(allowedChargeRate, "Allowed charge rate is required");
        Objects.requireNonNull(payerShareRate, "Payer share rate is required");
        Objects.requireNonNull(roundingMode, "Rounding mode is required");

        if (payerId.isBlank() || name.isBlank()) {
            throw new IllegalArgumentException("Payer ID and name cannot be blank");
        }
        if (allowedChargeRate.signum() < 0 || allowedChargeRate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Allowed charge rate must be between 0 and 1");
        }
        if (payerShareRate.signum() < 0 || payerShareRate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Payer share rate must be between 0 and 1");
        }
        if (currencyScale < 0 || currencyScale > 6) {
            throw new IllegalArgumentException("Currency scale must be between 0 and 6");
        }
    }
}
