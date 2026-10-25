package com.billing.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record ProcedureLine(
        String cptCode,
        BigDecimal billedCharge,
        Set<Diagnosis> diagnoses,
        List<Modifier> modifiers) {

    public ProcedureLine {
        Objects.requireNonNull(cptCode, "CPT code is required");
        Objects.requireNonNull(billedCharge, "Billed charge is required");
        Objects.requireNonNull(diagnoses, "Diagnoses collection is required");
        Objects.requireNonNull(modifiers, "Modifiers collection is required");

        if (cptCode.isBlank()) {
            throw new IllegalArgumentException("CPT code cannot be blank");
        }
        if (billedCharge.signum() < 0) {
            throw new IllegalArgumentException("Billed charge cannot be negative");
        }
        if (diagnoses.isEmpty()) {
            throw new IllegalArgumentException("Every procedure needs at least one diagnosis");
        }

        diagnoses = Set.copyOf(diagnoses);
        modifiers = List.copyOf(modifiers);
    }
}
