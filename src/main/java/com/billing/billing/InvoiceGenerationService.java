package com.billing.billing;

import com.billing.model.Adjustment;
import com.billing.model.Encounter;
import com.billing.model.FeeSchedule;
import com.billing.model.Invoice;
import com.billing.model.Payer;
import com.billing.model.ProcedureLine;
import com.billing.pricing.BundlingAndModifierService;
import com.billing.pricing.FeeSchedulePricingService;
import com.billing.validation.BillingValidator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class InvoiceGenerationService {
    private final BillingValidator validator;
    private final FeeSchedulePricingService pricingService;
    private final BundlingAndModifierService modifierService;
    private final BillingCalculationService calculationService;

    public InvoiceGenerationService(
            BillingValidator validator,
            FeeSchedulePricingService pricingService,
            BundlingAndModifierService modifierService,
            BillingCalculationService calculationService) {
        this.validator = Objects.requireNonNull(validator);
        this.pricingService = Objects.requireNonNull(pricingService);
        this.modifierService = Objects.requireNonNull(modifierService);
        this.calculationService = Objects.requireNonNull(calculationService);
    }

    public Invoice generate(
            String invoiceId,
            Encounter encounter,
            Payer payer,
            List<ProcedureLine> procedures,
            List<FeeSchedule> feeSchedules,
            Map<String, BigDecimal> hospitalStandardCharges,
            BigDecimal priorPayments) {

        Objects.requireNonNull(encounter, "Encounter is required");
        Objects.requireNonNull(payer, "Payer is required");
        Objects.requireNonNull(procedures, "Procedures are required");
        Objects.requireNonNull(feeSchedules, "Fee schedules are required");
        Objects.requireNonNull(hospitalStandardCharges, "Hospital charges are required");
        Objects.requireNonNull(priorPayments, "Prior payments are required");

        List<String> errors = validator.validate(encounter, procedures);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(
                    "Invoice validation failed: " + String.join("; ", errors));
        }

        if (invoiceId == null || invoiceId.isBlank()) {
            throw new IllegalArgumentException("Invoice ID is required");
        }

        List<BigDecimal> billedCharges = new ArrayList<>();
        List<BigDecimal> baseAllowedAmounts = new ArrayList<>();

        for (ProcedureLine procedure : procedures) {
            BigDecimal standardCharge = hospitalStandardCharges.get(procedure.cptCode());
            if (standardCharge == null) {
                throw new IllegalArgumentException(
                        "Missing hospital standard charge for CPT " + procedure.cptCode());
            }

            FeeSchedulePricingService.PricingResult priced = pricingService.price(
                    encounter, procedure, payer, feeSchedules, standardCharge);

            billedCharges.add(procedure.billedCharge());
            baseAllowedAmounts.add(priced.unitAllowedRate());
        }

        List<BundlingAndModifierService.LineResult> lineResults =
                modifierService.apply(encounter, procedures, baseAllowedAmounts, payer);

        List<BigDecimal> finalAllowedAmounts = lineResults.stream()
                .map(BundlingAndModifierService.LineResult::adjustedAllowedAmount)
                .toList();

        BillingCalculationService.BillingResult totals =
                calculationService.calculate(
                        billedCharges, finalAllowedAmounts, payer, priorPayments);

        List<Adjustment> invoiceAdjustments = new ArrayList<>();

        for (int i = 0; i < procedures.size(); i++) {
            BigDecimal billed = billedCharges.get(i);
            BigDecimal baseAllowed = baseAllowedAmounts.get(i);
            BigDecimal finalAllowed = finalAllowedAmounts.get(i);
            BundlingAndModifierService.LineResult line = lineResults.get(i);

            // For bundled lines, separate the bundling reduction from the
            // contractual difference so the adjustment ledger does not double-count.
            BigDecimal contractualBasis = line.bundled() ? baseAllowed : finalAllowed;
            BigDecimal writeOff = billed.subtract(contractualBasis);

            if (writeOff.signum() > 0) {
                invoiceAdjustments.add(new Adjustment(
                        "CONTRACTUAL",
                        "Contractual write-off for CPT " + procedures.get(i).cptCode(),
                        writeOff,
                        Adjustment.AdjustmentType.CONTRACTUAL_WRITE_OFF));
            }

            invoiceAdjustments.addAll(line.adjustments());
        }

        return new Invoice(
                invoiceId,
                encounter,
                payer,
                List.copyOf(procedures),
                invoiceAdjustments,
                totals.billedTotal(),
                totals.allowedTotal(),
                totals.payerResponsibility(),
                totals.patientResponsibility(),
                totals.priorPayments(),
                totals.patientBalanceDue(),
                totals.creditBalance());
    }
}