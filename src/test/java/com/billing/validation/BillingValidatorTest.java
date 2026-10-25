package com.billing.validation;

import com.billing.model.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class BillingValidatorTest {
    private static final LocalDate SERVICE_DATE = LocalDate.of(2026, 5, 15);

    private BillingValidator configuredValidator() {
        BillingValidator validator = new BillingValidator();
        validator.registerCpt("99213", LocalDate.of(2026, 1, 1), null);
        validator.registerIcd10("M54.5", LocalDate.of(2026, 1, 1), null);
        validator.allowDiagnosisForCpt("99213", "M54.5");
        return validator;
    }

    private Encounter encounter() {
        return new Encounter("ENC-TEST", SERVICE_DATE);
    }

    private ProcedureLine procedure(String cpt, String icd) {
        return new ProcedureLine(cpt, new BigDecimal("100.00"),
                Set.of(new Diagnosis(icd, "Sample diagnosis")), List.of());
    }

    @Test
    void acceptsRegisteredCodesAndApprovedPairing() {
        assertTrue(configuredValidator().validate(encounter(),
                List.of(procedure("99213", "M54.5"))).isEmpty());
    }

    @Test
    void rejectsUnregisteredCpt() {
        List<String> errors = configuredValidator().validate(encounter(),
                List.of(procedure("99999", "M54.5")));
        assertTrue(errors.stream().anyMatch(e -> e.contains("CPT code is not registered")));
    }

    @Test
    void rejectsUnregisteredIcd10() {
        List<String> errors = configuredValidator().validate(encounter(),
                List.of(procedure("99213", "Z99.9")));
        assertTrue(errors.stream().anyMatch(e -> e.contains("ICD-10 code Z99.9 is not registered")));
    }

    @Test
    void rejectsUnapprovedPairing() {
        BillingValidator validator = configuredValidator();
        validator.registerIcd10("R51.9", LocalDate.of(2026, 1, 1), null);

        List<String> errors = validator.validate(encounter(),
                List.of(procedure("99213", "R51.9")));
        assertTrue(errors.stream().anyMatch(e -> e.contains("pairing is not approved")));
    }

    @Test
    void rejectsExpiredCpt() {
        BillingValidator validator = new BillingValidator();
        validator.registerCpt("99213", LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 4, 30));
        validator.registerIcd10("M54.5", LocalDate.of(2026, 1, 1), null);
        validator.allowDiagnosisForCpt("99213", "M54.5");

        List<String> errors = validator.validate(encounter(),
                List.of(procedure("99213", "M54.5")));
        assertTrue(errors.stream().anyMatch(e -> e.contains("CPT code is invalid")));
    }

    @Test
    void rejectsExpiredIcd10() {
        BillingValidator validator = new BillingValidator();
        validator.registerCpt("99213", LocalDate.of(2026, 1, 1), null);
        validator.registerIcd10("M54.5", LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 4, 30));
        validator.allowDiagnosisForCpt("99213", "M54.5");

        List<String> errors = validator.validate(encounter(),
                List.of(procedure("99213", "M54.5")));
        assertTrue(errors.stream().anyMatch(e -> e.contains("ICD-10 code M54.5 is invalid")));
    }

    @Test
    void rejectsEncounterWithoutProcedures() {
        List<String> errors = configuredValidator().validate(encounter(), List.of());
        assertTrue(errors.stream().anyMatch(e -> e.contains("at least one procedure")));
    }
}