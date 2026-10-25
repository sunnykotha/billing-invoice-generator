package com.billing.billing;

import com.billing.model.*;
import com.billing.pricing.*;
import com.billing.validation.BillingValidator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class InvoiceGenerationServiceTest {
    private final LocalDate date = LocalDate.of(2026, 10, 24);
    private final Payer payer = new Payer(
            "PAYER-1", "Test Payer",
            new BigDecimal("1.00"), new BigDecimal("0.80"),
            2, RoundingMode.HALF_UP);

    private BillingValidator validValidator() {
        BillingValidator validator = new BillingValidator();
        validator.registerCpt("99213", date.minusYears(1), date.plusYears(1));
        validator.registerCpt("93000", date.minusYears(1), date.plusYears(1));
        validator.registerIcd10("J06.9", date.minusYears(1), date.plusYears(1));
        validator.allowDiagnosisForCpt("99213", "J06.9");
        validator.allowDiagnosisForCpt("93000", "J06.9");
        return validator;
    }

    private InvoiceGenerationService service(BillingValidator validator) {
        BundlingAndModifierService modifiers = new BundlingAndModifierService();
        return new InvoiceGenerationService(
                validator,
                new FeeSchedulePricingService(),
                modifiers,
                new BillingCalculationService());
    }

    @Test
    void generatesInvoiceWithExpectedAllowedAndResponsibilityTotals() {
        Diagnosis diagnosis = new Diagnosis("J06.9", "Test diagnosis");
        ProcedureLine first = new ProcedureLine(
                "99213", new BigDecimal("1000.00"),
                Set.of(diagnosis), List.of());
        ProcedureLine second = new ProcedureLine(
                "93000", new BigDecimal("600.00"),
                Set.of(diagnosis), List.of(Modifier.MULTIPLE_PROCEDURE_51));

        Encounter encounter = new Encounter("ENC-STEP11", date);
        FeeSchedule schedule = new FeeSchedule(
                "FS-1", "PAYER-1",
                date.minusDays(30), date.plusDays(30),
                Map.of("99213", new BigDecimal("800.00"),
                       "93000", new BigDecimal("480.00")));

        InvoiceGenerationService service = service(validValidator());
        Invoice invoice = service.generate(
                "INV-STEP11", encounter, payer,
                List.of(first, second), List.of(schedule),
                Map.of("99213", new BigDecimal("1000.00"),
                       "93000", new BigDecimal("600.00")),
                BigDecimal.ZERO);

        assertEquals(new BigDecimal("1600.00"), invoice.billedTotal());
        assertEquals(new BigDecimal("1040.00"), invoice.allowedTotal());
        assertEquals(new BigDecimal("832.00"), invoice.payerResponsibility());
        assertEquals(new BigDecimal("208.00"), invoice.patientResponsibility());
        assertEquals(new BigDecimal("560.00"),
                invoice.adjustments().stream()
                        .filter(a -> a.type() == Adjustment.AdjustmentType.CONTRACTUAL_WRITE_OFF)
                        .map(Adjustment::amount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    @Test
    void rejectsInvalidProceduresBeforeCreatingInvoice() {
        Diagnosis diagnosis = new Diagnosis("J06.9", "Test diagnosis");
        ProcedureLine invalid = new ProcedureLine(
                "99999", new BigDecimal("100.00"),
                Set.of(diagnosis), List.of());

        Encounter encounter = new Encounter("ENC-INVALID", date);
        InvoiceGenerationService service = service(validValidator());

        assertThrows(IllegalArgumentException.class, () ->
                service.generate(
                        "INV-INVALID", encounter, payer,
                        List.of(invalid), List.of(),
                        Map.of("99999", new BigDecimal("100.00")),
                        BigDecimal.ZERO));
    }
}