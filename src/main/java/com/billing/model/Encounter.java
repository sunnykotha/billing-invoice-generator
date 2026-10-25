package com.billing.model;

import java.time.LocalDate;
import java.util.Objects;

public record Encounter(String encounterId, LocalDate serviceDate) {
    public Encounter {
        Objects.requireNonNull(encounterId, "Encounter ID is required");
        Objects.requireNonNull(serviceDate, "Service date is required");
        if (encounterId.isBlank()) {
            throw new IllegalArgumentException("Encounter ID cannot be blank");
        }
    }
}
