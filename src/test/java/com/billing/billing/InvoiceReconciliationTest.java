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

class InvoiceReconciliationTest {

    @Test
    void bundledInvoiceAdjustmentsReconcileWithoutDoubleCounting() {
        LocalDate date = LocalDate.of(2026, 10, 24);
        Diagnosis diagnosis = new Diagnosis("J06.9", "Test diagnosis");

        ProcedureLine primary = new ProcedureLine(
                "99213", new BigDecimal("1000.00"),
                Set.of(diagnosis), List.of());

        ProcedureLine component = new ProcedureLine(
                "93000", new BigDecimal("600.00"),
                Set.of(diagnosis), List.of());

        Payer payer = new Payer(
                "PAYER-1", "Test Payer",
                new BigDecimal("1.00"), new BigDecimal("0.80"),
                2, RoundingMode.HALF_UP);

        Encounter encounter = new Encounter("ENC-RECON", date);

        BillingValidator validator = new BillingValidator();
        validator.registerCpt("99213", date.minusYears(1), date.plusYears(1));
        validator.registerCpt("93000", date.minusYears(1), date.plusYears(1));
        validator.registerIcd10("J06.9", date.minusYears(1), date.plusYears(1));
        validator.allowDiagnosisForCpt("99213", "J06.9");
        validator.allowDiagnosisForCpt("93000", "J06.9");

        FeeSchedule schedule = new FeeSchedule(
                "FS-1", "PAYER-1",
                date.minusDays(30), date.plusDays(30),
                Map.of(
                        "99213", new BigDecimal("800.00"),
                        "93000", new BigDecimal("480.00")));

        BundlingAndModifierService modifierService =
                new BundlingAndModifierService();
        modifierService.registerBundledComponent("99213", "93000");

        InvoiceGenerationService service = new InvoiceGenerationService(
                validator,
                new FeeSchedulePricingService(),
                modifierService,
                new BillingCalculationService());

        Invoice invoice = service.generate(
                "INV-RECON",
                encounter,
                payer,
                List.of(primary, component),
                List.of(schedule),
                Map.of(
                        "99213", new BigDecimal("1000.00"),
                        "93000", new BigDecimal("600.00")),
                BigDecimal.ZERO);

        BigDecimal adjustmentTotal = invoice.adjustments().stream()
                .map(Adjustment::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(new BigDecimal("1600.00"), invoice.billedTotal());
        assertEquals(new BigDecimal("800.00"), invoice.allowedTotal());
        assertEquals(new BigDecimal("800.00"), adjustmentTotal);
        assertEquals(
                invoice.billedTotal().subtract(invoice.allowedTotal()),
                adjustmentTotal);

        BigDecimal bundlingTotal = invoice.adjustments().stream()
                .filter(a -> a.type() == Adjustment.AdjustmentType.BUNDLING)
                .map(Adjustment::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(new BigDecimal("480.00"), bundlingTotal);
    }
}