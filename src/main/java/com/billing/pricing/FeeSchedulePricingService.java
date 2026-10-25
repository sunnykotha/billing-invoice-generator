package com.billing.pricing;

import com.billing.model.Encounter;
import com.billing.model.FeeSchedule;
import com.billing.model.Payer;
import com.billing.model.ProcedureLine;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class FeeSchedulePricingService {

    public PricingResult price(
            Encounter encounter,
            ProcedureLine procedure,
            Payer payer,
            List<FeeSchedule> feeSchedules,
            BigDecimal hospitalStandardCharge) {

        Objects.requireNonNull(encounter, "Encounter is required");
        Objects.requireNonNull(procedure, "Procedure is required");
        Objects.requireNonNull(payer, "Payer is required");
        Objects.requireNonNull(feeSchedules, "Fee schedules are required");
        Objects.requireNonNull(hospitalStandardCharge, "Standard charge is required");

        if (hospitalStandardCharge.signum() < 0) {
            throw new IllegalArgumentException("Standard charge cannot be negative.");
        }

        LocalDate serviceDate = encounter.serviceDate();

        Optional<FeeSchedule> matchingSchedule = feeSchedules.stream()
                .filter(schedule -> schedule.payerId().equals(payer.payerId()))
                .filter(schedule -> schedule.covers(serviceDate))
                .filter(schedule -> schedule.rateFor(procedure.cptCode()).isPresent())
                .max((first, second) ->
                        first.effectiveFrom().compareTo(second.effectiveFrom()));

        if (matchingSchedule.isPresent()) {
            FeeSchedule schedule = matchingSchedule.get();
            BigDecimal rate = schedule.rateFor(procedure.cptCode()).orElseThrow();

            return new PricingResult(
                    procedure.cptCode(),
                    rate,
                    schedule.scheduleId(),
                    false
            );
        }

        return new PricingResult(
                procedure.cptCode(),
                hospitalStandardCharge,
                "HOSPITAL_STANDARD",
                true
        );
    }

    public record PricingResult(
            String cptCode,
            BigDecimal unitAllowedRate,
            String sourceId,
            boolean usedHospitalStandardCharge) {

        public PricingResult {
            Objects.requireNonNull(cptCode, "CPT code is required");
            Objects.requireNonNull(unitAllowedRate, "Rate is required");
            Objects.requireNonNull(sourceId, "Source is required");

            if (unitAllowedRate.signum() < 0) {
                throw new IllegalArgumentException("Rate cannot be negative.");
            }
        }
    }
}