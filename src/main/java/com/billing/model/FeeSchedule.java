package com.billing.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record FeeSchedule(
        String scheduleId,
        String payerId,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        Map<String, BigDecimal> cptRates) {

    public FeeSchedule {
        Objects.requireNonNull(scheduleId, "Schedule ID is required");
        Objects.requireNonNull(payerId, "Payer ID is required");
        Objects.requireNonNull(effectiveFrom, "Effective-from date is required");
        Objects.requireNonNull(cptRates, "CPT rates are required");

        if (scheduleId.isBlank() || payerId.isBlank()) {
            throw new IllegalArgumentException("Schedule and payer IDs cannot be blank");
        }
        if (effectiveTo != null && effectiveTo.isBefore(effectiveFrom)) {
            throw new IllegalArgumentException("End date cannot precede start date");
        }

        cptRates.forEach((code, rate) -> {
            if (code == null || code.isBlank() || rate == null || rate.signum() < 0) {
                throw new IllegalArgumentException("CPT rates need valid codes and non-negative amounts");
            }
        });
        cptRates = Map.copyOf(cptRates);
    }

    public boolean covers(LocalDate date) {
        return !date.isBefore(effectiveFrom)
                && (effectiveTo == null || !date.isAfter(effectiveTo));
    }

    public Optional<BigDecimal> rateFor(String cptCode) {
        return Optional.ofNullable(cptRates.get(cptCode));
    }
}
