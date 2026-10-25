package com.billing.pricing;

import com.billing.model.Diagnosis;
import com.billing.model.Encounter;
import com.billing.model.Modifier;
import com.billing.model.Payer;
import com.billing.model.ProcedureLine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BundlingAndModifierServiceTest {

    private BundlingAndModifierService service;
    private Encounter encounter;
    private Payer payer;
    private Diagnosis diagnosis;

    @BeforeEach
    void setUp() {
        service = new BundlingAndModifierService();
        service.registerBundledComponent("A", "B");

        encounter = new Encounter("ENC-100", LocalDate.of(2026, 10, 24));
        payer = new Payer(
                "PAYER-1",
                "Test Payer",
                new BigDecimal("1.00"),
                new BigDecimal("0.80"),
                2,
                RoundingMode.HALF_UP);
        diagnosis = new Diagnosis("Z00.00", "General examination");
    }

    private ProcedureLine procedure(String cpt, Modifier... modifiers) {
        return new ProcedureLine(
                cpt,
                new BigDecimal("100.00"),
                Set.of(diagnosis),
                List.of(modifiers));
    }

    @Test
    void componentIsBundledWhenPrimaryIsPresent() {
        List<ProcedureLine> procedures = List.of(
                procedure("A"),
                procedure("B"));

        List<BundlingAndModifierService.LineResult> results = service.apply(
                encounter,
                procedures,
                List.of(new BigDecimal("800.00"), new BigDecimal("480.00")),
                payer);

        assertEquals(new BigDecimal("800.00"), results.get(0).adjustedAllowedAmount());
        assertTrue(results.get(1).bundled());
        assertEquals(new BigDecimal("0.00"), results.get(1).adjustedAllowedAmount());
        assertEquals("BUNDLING", results.get(1).adjustments().get(0).adjustmentCode());
        assertEquals(new BigDecimal("480.00"), results.get(1).adjustments().get(0).amount());
    }

    @Test
    void modifier59OverridesBundling() {
        List<ProcedureLine> procedures = List.of(
                procedure("A"),
                procedure("B", Modifier.DISTINCT_PROCEDURAL_SERVICE_59));

        List<BundlingAndModifierService.LineResult> results = service.apply(
                encounter,
                procedures,
                List.of(new BigDecimal("800.00"), new BigDecimal("480.00")),
                payer);

        assertFalse(results.get(1).bundled());
        assertEquals(new BigDecimal("480.00"), results.get(1).adjustedAllowedAmount());
        assertTrue(results.get(1).adjustments().isEmpty());
    }

    @Test
    void bilateralModifierPays150Percent() {
        List<BundlingAndModifierService.LineResult> results = service.apply(
                encounter,
                List.of(procedure("C", Modifier.BILATERAL_50)),
                List.of(new BigDecimal("100.00")),
                payer);

        assertEquals(new BigDecimal("150.00"), results.get(0).adjustedAllowedAmount());
    }

    @Test
    void modifier51ReducesSecondEligibleProcedureByHalf() {
        List<BundlingAndModifierService.LineResult> results = service.apply(
                encounter,
                List.of(
                        procedure("A"),
                        procedure("C", Modifier.MULTIPLE_PROCEDURE_51)),
                List.of(new BigDecimal("800.00"), new BigDecimal("480.00")),
                payer);

        assertEquals(new BigDecimal("800.00"), results.get(0).adjustedAllowedAmount());
        assertEquals(new BigDecimal("240.00"), results.get(1).adjustedAllowedAmount());
    }

    @Test
    void modifier51DoesNotReduceFirstEligibleProcedure() {
        List<BundlingAndModifierService.LineResult> results = service.apply(
                encounter,
                List.of(procedure("A", Modifier.MULTIPLE_PROCEDURE_51)),
                List.of(new BigDecimal("100.00")),
                payer);

        assertEquals(new BigDecimal("100.00"), results.get(0).adjustedAllowedAmount());
    }

    @Test
    void payerSpecificRoundingIsApplied() {
        Payer roundingPayer = new Payer(
                "PAYER-2",
                "Down Rounding Payer",
                new BigDecimal("1.00"),
                new BigDecimal("0.80"),
                2,
                RoundingMode.DOWN);

        List<BundlingAndModifierService.LineResult> results = service.apply(
                encounter,
                List.of(procedure("C", Modifier.BILATERAL_50)),
                List.of(new BigDecimal("100.005")),
                roundingPayer);

        assertEquals(new BigDecimal("150.00"), results.get(0).adjustedAllowedAmount());
    }

    @Test
    void rejectsDifferentProcedureAndAmountCounts() {
        assertThrows(IllegalArgumentException.class, () -> service.apply(
                encounter,
                List.of(procedure("A")),
                List.of(),
                payer));
    }

    @Test
    void rejectsNegativeBaseAllowedAmount() {
        assertThrows(IllegalArgumentException.class, () -> service.apply(
                encounter,
                List.of(procedure("A")),
                List.of(new BigDecimal("-1.00")),
                payer));
    }
}