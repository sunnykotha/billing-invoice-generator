package com.billing.model;

import java.math.BigDecimal;
import java.util.Objects;

public record Adjustment(
        String adjustmentCode,
        String description,
        BigDecimal amount,
        AdjustmentType type) {

    public Adjustment {
        Objects.requireNonNull(adjustmentCode, "Adjustment code is required");
        Objects.requireNonNull(description, "Adjustment description is required");
        Objects.requireNonNull(amount, "Adjustment amount is required");
        Objects.requireNonNull(type, "Adjustment type is required");

        if (adjustmentCode.isBlank() || description.isBlank()) {
            throw new IllegalArgumentException("Adjustment fields cannot be blank");
        }
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("Adjustment amount cannot be negative");
        }
    }

    public enum AdjustmentType {
        CONTRACTUAL_WRITE_OFF,
        BUNDLING,
        OTHER
    }
}
