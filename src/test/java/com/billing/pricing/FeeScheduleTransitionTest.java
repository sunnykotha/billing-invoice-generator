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

class FeeScheduleTransitionTest {

    @Test
    void selectsCorrectScheduleBeforeAndAfterMidPeriodChange() {
        LocalDate june30 = LocalDate.of(2026, 6, 30);
        LocalDate july1 = LocalDate.of(2026, 7, 1);

        Payer payer = new Payer(
                "PAYER-DATE", "Date Change Payer",
                new BigDecimal("1.00"), new BigDecimal("0.80"),
                2, RoundingMode.HALF_UP);

        Diagnosis diagnosis = new Diagnosis("J06.9", "Test diagnosis");
        ProcedureLine procedure = new ProcedureLine(
                "99213", new BigDecimal("100.00"),
                Set.of(diagnosis), List.of());

        FeeSchedule juneSchedule = new FeeSchedule(
                "FS-JUNE", "PAYER-DATE",
                june30.minusDays(30), june30,
                Map.of("99213", new BigDecimal("75.00")));

        FeeSchedule julySchedule = new FeeSchedule(
                "FS-JULY", "PAYER-DATE",
                july1, july1.plusDays(30),
                Map.of("99213", new BigDecimal("85.00")));

        FeeSchedulePricingService service = new FeeSchedulePricingService();

        FeeSchedulePricingService.PricingResult juneResult = service.price(
                new Encounter("ENC-JUNE", june30),
                procedure, payer,
                List.of(juneSchedule, julySchedule),
                new BigDecimal("100.00"));

        FeeSchedulePricingService.PricingResult julyResult = service.price(
                new Encounter("ENC-JULY", july1),
                procedure, payer,
                List.of(juneSchedule, julySchedule),
                new BigDecimal("100.00"));

        assertEquals("FS-JUNE", juneResult.sourceId());
        assertEquals(new BigDecimal("75.00"), juneResult.unitAllowedRate());

        assertEquals("FS-JULY", julyResult.sourceId());
        assertEquals(new BigDecimal("85.00"), julyResult.unitAllowedRate());
    }
}