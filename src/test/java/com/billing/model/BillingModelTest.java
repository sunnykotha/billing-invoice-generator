package com.billing.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BillingModelTest {

    @Test
    void diagnosisRejectsBlankIcd10Code() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Diagnosis(" ", "Test diagnosis")
        );
    }

    @Test
    void encounterRequiresValidDetails() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Encounter("", LocalDate.of(2026, 1, 10))
        );

        assertDoesNotThrow(
                () -> new Encounter("ENC-001", LocalDate.of(2026, 1, 10))
        );
    }

    @Test
    void procedureRequiresAtLeastOneDiagnosis() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ProcedureLine(
                        "99213",
                        new BigDecimal("100.00"),
                        Set.of(),
                        List.of()
                )
        );
    }

    @Test
    void procedureRejectsNegativeCharge() {
        Diagnosis diagnosis = new Diagnosis("M54.5", "Low back pain");

        assertThrows(
                IllegalArgumentException.class,
                () -> new ProcedureLine(
                        "99213",
                        new BigDecimal("-10.00"),
                        Set.of(diagnosis),
                        List.of()
                )
        );
    }

    @Test
    void feeScheduleCoversBothEffectiveDateBoundaries() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 6, 30);

        FeeSchedule schedule = new FeeSchedule(
                "FS-001",
                "PAYER-001",
                start,
                end,
                Map.of("99213", new BigDecimal("85.00"))
        );

        assertTrue(schedule.covers(start));
        assertTrue(schedule.covers(end));
        assertTrue(schedule.covers(LocalDate.of(2026, 3, 15)));
        assertFalse(schedule.covers(start.minusDays(1)));
        assertFalse(schedule.covers(end.plusDays(1)));
    }

    @Test
    void feeScheduleReturnsConfiguredCptRate() {
        FeeSchedule schedule = new FeeSchedule(
                "FS-002",
                "PAYER-001",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                Map.of("99213", new BigDecimal("85.00"))
        );

        assertEquals(
                0,
                new BigDecimal("85.00").compareTo(schedule.rateFor("99213").orElseThrow())
        );
    }

    @Test
    void adjustmentRejectsNegativeAmount() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Adjustment(
                        "CO-1",
                        "Contractual adjustment",
                        new BigDecimal("-5.00"),
                        Adjustment.AdjustmentType.CONTRACTUAL_WRITE_OFF
                )
        );
    }
}