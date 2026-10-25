package com.billing.billing;

import com.billing.model.*;
import com.billing.pricing.FeeSchedulePricingService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BillingEdgeCaseTest {
    private final LocalDate date = LocalDate.of(2026, 10, 24);

    private final Payer payer = new Payer(
            "PAYER-EDGE", "Edge Case Payer",
            new BigDecimal("1.00"), new BigDecimal("0.80"),
            2, RoundingMode.HALF_UP);

    @Test
    void overpaymentCreatesCreditInsteadOfNegativePatientBalance() {
        BillingCalculationService service = new BillingCalculationService();

        BillingCalculationService.BillingResult result = service.calculate(
                List.of(new BigDecimal("100.00")),
                List.of(new BigDecimal("100.00")),
                payer,
                new BigDecimal("50.00"));

        assertEquals(new BigDecimal("20.00"), result.patientResponsibility());
        assertEquals(new BigDecimal("0.00"), result.patientBalanceDue());
        assertEquals(new BigDecimal("30.00"), result.creditBalance());
    }

    @Test
    void exactFeeScheduleEffectiveDateIsAccepted() {
        Diagnosis diagnosis = new Diagnosis("J06.9", "Test diagnosis");
        ProcedureLine procedure = new ProcedureLine(
                "99213", new BigDecimal("100.00"),
                Set.of(diagnosis), List.of());
        Encounter encounter = new Encounter("ENC-BOUNDARY", date);

        FeeSchedule schedule = new FeeSchedule(
                "FS-BOUNDARY", "PAYER-EDGE",
                date, date,
                Map.of("99213", new BigDecimal("80.00")));

        FeeSchedulePricingService service = new FeeSchedulePricingService();
        FeeSchedulePricingService.PricingResult result = service.price(
                encounter, procedure, payer,
                List.of(schedule), new BigDecimal("100.00"));

        assertEquals(new BigDecimal("80.00"), result.unitAllowedRate());
        assertEquals("FS-BOUNDARY", result.sourceId());
        assertFalse(result.usedHospitalStandardCharge());
    }

    @Test
    void zeroPriorPaymentsProduceNoCreditBalance() {
        BillingCalculationService service = new BillingCalculationService();

        BillingCalculationService.BillingResult result = service.calculate(
                List.of(new BigDecimal("100.00")),
                List.of(new BigDecimal("80.00")),
                payer,
                BigDecimal.ZERO);

        assertEquals(new BigDecimal("64.00"), result.payerResponsibility());
        assertEquals(new BigDecimal("16.00"), result.patientResponsibility());
        assertEquals(new BigDecimal("16.00"), result.patientBalanceDue());
        assertEquals(new BigDecimal("0.00"), result.creditBalance());
    }
}