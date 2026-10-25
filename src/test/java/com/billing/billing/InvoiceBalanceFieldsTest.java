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

import static org.junit.jupiter.api.Assertions.assertEquals;

class InvoiceBalanceFieldsTest {

    @Test
    void invoiceExposesCreditWhenPriorPaymentsExceedPatientResponsibility() {
        LocalDate date = LocalDate.of(2026, 10, 24);

        Payer payer = new Payer(
                "PAYER-1", "Test Payer",
                new BigDecimal("1.00"), new BigDecimal("0.80"),
                2, RoundingMode.HALF_UP);

        BillingValidator validator = new BillingValidator();
        validator.registerCpt("99213", date.minusYears(1), date.plusYears(1));
        validator.registerIcd10("J06.9", date.minusYears(1), date.plusYears(1));
        validator.allowDiagnosisForCpt("99213", "J06.9");

        InvoiceGenerationService service = new InvoiceGenerationService(
                validator,
                new FeeSchedulePricingService(),
                new BundlingAndModifierService(),
                new BillingCalculationService());

        Diagnosis diagnosis = new Diagnosis("J06.9", "Test diagnosis");
        ProcedureLine procedure = new ProcedureLine(
                "99213", new BigDecimal("1000.00"),
                Set.of(diagnosis), List.of());

        FeeSchedule schedule = new FeeSchedule(
                "FS-1", "PAYER-1",
                date.minusDays(30), date.plusDays(30),
                Map.of("99213", new BigDecimal("800.00")));

        Invoice invoice = service.generate(
                "INV-CREDIT",
                new Encounter("ENC-CREDIT", date),
                payer,
                List.of(procedure),
                List.of(schedule),
                Map.of("99213", new BigDecimal("1000.00")),
                new BigDecimal("200.00"));

        assertEquals(new BigDecimal("160.00"), invoice.patientResponsibility());
        assertEquals(new BigDecimal("200.00"), invoice.priorPayments());
        assertEquals(new BigDecimal("0.00"), invoice.patientBalanceDue());
        assertEquals(new BigDecimal("40.00"), invoice.creditBalance());
    }
}