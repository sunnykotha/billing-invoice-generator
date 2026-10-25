package com.billing.pricing;

import com.billing.model.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FeeSchedulePricingServiceTest {

    private final FeeSchedulePricingService service = new FeeSchedulePricingService();

    private final Payer payer = new Payer(
            "PAYER-1", "Sample Payer",
            new BigDecimal("1.00"), new BigDecimal("0.80"),
            2, RoundingMode.HALF_UP
    );

    private final Encounter encounter = new Encounter(
            "ENC-1", LocalDate.of(2026, 5, 15)
    );

    private final ProcedureLine procedure = new ProcedureLine(
            "99213", new BigDecimal("100.00"),
            Set.of(new Diagnosis("M54.5", "Sample diagnosis")),
            List.of()
    );

    private final BigDecimal standardCharge = new BigDecimal("125.00");

    @Test
    void usesContractedRateWhenScheduleIsEffective() {
        FeeSchedule schedule = new FeeSchedule(
                "FS-1", "PAYER-1",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                Map.of("99213", new BigDecimal("85.00"))
        );

        var result = service.price(
                encounter, procedure, payer, List.of(schedule), standardCharge
        );

        assertEquals(0, new BigDecimal("85.00").compareTo(result.unitAllowedRate()));
        assertEquals("FS-1", result.sourceId());
        assertFalse(result.usedHospitalStandardCharge());
    }

    @Test
    void fallsBackWhenNoMatchingPayerScheduleExists() {
        FeeSchedule schedule = new FeeSchedule(
                "FS-OTHER", "OTHER-PAYER",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                Map.of("99213", new BigDecimal("85.00"))
        );

        var result = service.price(
                encounter, procedure, payer, List.of(schedule), standardCharge
        );

        assertEquals(0, standardCharge.compareTo(result.unitAllowedRate()));
        assertEquals("HOSPITAL_STANDARD", result.sourceId());
        assertTrue(result.usedHospitalStandardCharge());
    }

    @Test
    void fallsBackWhenScheduleHasNoRateForCpt() {
        FeeSchedule schedule = new FeeSchedule(
                "FS-2", "PAYER-1",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                Map.of("93000", new BigDecimal("70.00"))
        );

        var result = service.price(
                encounter, procedure, payer, List.of(schedule), standardCharge
        );

        assertEquals(0, standardCharge.compareTo(result.unitAllowedRate()));
        assertTrue(result.usedHospitalStandardCharge());
    }

    @Test
    void fallsBackWhenScheduleIsExpired() {
        FeeSchedule schedule = new FeeSchedule(
                "FS-OLD", "PAYER-1",
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 12, 31),
                Map.of("99213", new BigDecimal("75.00"))
        );

        var result = service.price(
                encounter, procedure, payer, List.of(schedule), standardCharge
        );

        assertEquals(0, standardCharge.compareTo(result.unitAllowedRate()));
        assertTrue(result.usedHospitalStandardCharge());
    }

    @Test
    void selectsNewestEffectiveScheduleWhenSchedulesOverlap() {
        FeeSchedule oldSchedule = new FeeSchedule(
                "FS-OLD", "PAYER-1",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                Map.of("99213", new BigDecimal("80.00"))
        );

        FeeSchedule newSchedule = new FeeSchedule(
                "FS-NEW", "PAYER-1",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 12, 31),
                Map.of("99213", new BigDecimal("90.00"))
        );

        var result = service.price(
                encounter, procedure, payer,
                List.of(oldSchedule, newSchedule), standardCharge
        );

        assertEquals(0, new BigDecimal("90.00").compareTo(result.unitAllowedRate()));
        assertEquals("FS-NEW", result.sourceId());
    }
}