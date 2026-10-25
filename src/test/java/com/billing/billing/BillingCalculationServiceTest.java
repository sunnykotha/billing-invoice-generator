package com.billing.billing;

import com.billing.model.Adjustment;
import com.billing.model.Payer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BillingCalculationServiceTest {

    private BillingCalculationService service;
    private Payer payer;

    @BeforeEach
    void setUp() {
        service = new BillingCalculationService();
        payer = new Payer(
                "PAYER-1",
                "Test Payer",
                new BigDecimal("1.00"),
                new BigDecimal("0.80"),
                2,
                RoundingMode.HALF_UP);
    }

    @Test
    void calculatesContractualWriteOffAndPatientAndPayerShares() {
        var result = service.calculate(
                List.of(new BigDecimal("1000.00"), new BigDecimal("600.00")),
                List.of(new BigDecimal("800.00"), new BigDecimal("240.00")),
                payer,
                new BigDecimal("0.00"));

        assertEquals(new BigDecimal("1600.00"), result.billedTotal());
        assertEquals(new BigDecimal("1040.00"), result.allowedTotal());
        assertEquals(new BigDecimal("560.00"), result.contractualWriteOff());
        assertEquals(new BigDecimal("832.00"), result.payerResponsibility());
        assertEquals(new BigDecimal("208.00"), result.patientResponsibility());
        assertEquals(new BigDecimal("208.00"), result.patientBalanceDue());
        assertEquals(new BigDecimal("0.00"), result.creditBalance());
    }

    @Test
    void createsAnAuditableWriteOffForEachLine() {
        var result = service.calculate(
                List.of(new BigDecimal("1000.00"), new BigDecimal("600.00")),
                List.of(new BigDecimal("800.00"), new BigDecimal("240.00")),
                payer,
                new BigDecimal("0.00"));

        assertEquals(2, result.adjustments().size());
        assertEquals("CONTRACTUAL_WRITE_OFF",
                result.adjustments().get(0).adjustmentCode());
        assertEquals(new BigDecimal("200.00"),
                result.adjustments().get(0).amount());
        assertEquals(Adjustment.AdjustmentType.CONTRACTUAL_WRITE_OFF,
                result.adjustments().get(0).type());
        assertEquals(new BigDecimal("360.00"),
                result.adjustments().get(1).amount());
    }

    @Test
    void priorPaymentsReducePatientBalanceDue() {
        var result = service.calculate(
                List.of(new BigDecimal("1000.00")),
                List.of(new BigDecimal("800.00")),
                payer,
                new BigDecimal("100.00"));

        assertEquals(new BigDecimal("160.00"), result.patientResponsibility());
        assertEquals(new BigDecimal("100.00"), result.priorPayments());
        assertEquals(new BigDecimal("60.00"), result.patientBalanceDue());
        assertEquals(new BigDecimal("0.00"), result.creditBalance());
    }

    @Test
    void overpaymentCreatesCreditInsteadOfNegativeBalance() {
        var result = service.calculate(
                List.of(new BigDecimal("1000.00")),
                List.of(new BigDecimal("800.00")),
                payer,
                new BigDecimal("200.00"));

        assertEquals(new BigDecimal("160.00"), result.patientResponsibility());
        assertEquals(new BigDecimal("0.00"), result.patientBalanceDue());
        assertEquals(new BigDecimal("40.00"), result.creditBalance());
    }

    @Test
    void allowedAmountAboveChargeDoesNotCreateNegativeWriteOff() {
        var result = service.calculate(
                List.of(new BigDecimal("100.00")),
                List.of(new BigDecimal("150.00")),
                payer,
                new BigDecimal("0.00"));

        assertEquals(new BigDecimal("150.00"), result.allowedTotal());
        assertEquals(new BigDecimal("0.00"), result.contractualWriteOff());
        assertTrue(result.adjustments().isEmpty());
    }

    @Test
    void payerShareCannotExceedAllowedTotal() {
        Payer fullCoveragePayer = new Payer(
                "PAYER-2",
                "Full Coverage Payer",
                new BigDecimal("1.00"),
                new BigDecimal("1.00"),
                2,
                RoundingMode.HALF_UP);

        var result = service.calculate(
                List.of(new BigDecimal("100.00")),
                List.of(new BigDecimal("80.00")),
                fullCoveragePayer,
                new BigDecimal("0.00"));

        assertEquals(new BigDecimal("80.00"), result.payerResponsibility());
        assertEquals(new BigDecimal("0.00"), result.patientResponsibility());
        assertEquals(new BigDecimal("0.00"), result.patientBalanceDue());
    }

    @Test
    void appliesPayerSpecificRounding() {
        Payer downRoundingPayer = new Payer(
                "PAYER-3",
                "Down Rounding Payer",
                new BigDecimal("1.00"),
                new BigDecimal("0.50"),
                2,
                RoundingMode.DOWN);

        var result = service.calculate(
                List.of(new BigDecimal("100.00")),
                List.of(new BigDecimal("10.01")),
                downRoundingPayer,
                new BigDecimal("0.00"));

        assertEquals(new BigDecimal("5.00"), result.payerResponsibility());
        assertEquals(new BigDecimal("5.01"), result.patientResponsibility());
    }

    @Test
    void rejectsMismatchedLineCounts() {
        assertThrows(IllegalArgumentException.class, () -> service.calculate(
                List.of(new BigDecimal("100.00")),
                List.of(),
                payer,
                BigDecimal.ZERO));
    }

    @Test
    void rejectsNegativePriorPayments() {
        assertThrows(IllegalArgumentException.class, () -> service.calculate(
                List.of(new BigDecimal("100.00")),
                List.of(new BigDecimal("80.00")),
                payer,
                new BigDecimal("-1.00")));
    }

    @Test
    void rejectsNegativeCharges() {
        assertThrows(IllegalArgumentException.class, () -> service.calculate(
                List.of(new BigDecimal("-100.00")),
                List.of(new BigDecimal("80.00")),
                payer,
                BigDecimal.ZERO));
    }
}