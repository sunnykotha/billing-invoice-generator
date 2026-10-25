package com.billing.model;

import java.util.Objects;

public record Diagnosis(String icd10Code, String description) {
    public Diagnosis {
        Objects.requireNonNull(icd10Code, "ICD-10 code is required");
        Objects.requireNonNull(description, "Diagnosis description is required");
        if (icd10Code.isBlank() || description.isBlank()) {
            throw new IllegalArgumentException("Diagnosis fields cannot be blank");
        }
    }
}
