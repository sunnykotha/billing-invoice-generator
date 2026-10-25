package com.billing.billing;

import com.billing.model.Adjustment;
import com.billing.model.Payer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BillingCalculationService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    public BillingResult calculate(
            List<BigDecimal> billedCharges,
            List<BigDecimal> allowedAmounts,
            Payer payer,
            BigDecimal priorPayments) {

        Objects.requireNonNull(billedCharges, "Billed charges are required");
        Objects.requireNonNull(allowedAmounts, "Allowed amounts are required");
        Objects.requireNonNull(payer, "Payer is required");
        Objects.requireNonNull(priorPayments, "Prior payments are required");

        if (billedCharges.isEmpty()) {
            throw new IllegalArgumentException("At least one billed charge is required");
        }

        if (billedCharges.size() != allowedAmounts.size()) {
            throw new IllegalArgumentException(
                    "Each billed charge must have a corresponding allowed amount");
        }

        if (priorPayments.signum() < 0) {
            throw new IllegalArgumentException("Prior payments cannot be negative");
        }

        int scale = payer.currencyScale();
        var rounding = payer.roundingMode();

        BigDecimal billedTotal = ZERO.setScale(scale, rounding);
        BigDecimal allowedTotal = ZERO.setScale(scale, rounding);
        BigDecimal payerResponsibility = ZERO.setScale(scale, rounding);
        BigDecimal contractualWriteOff = ZERO.setScale(scale, rounding);

        List<Adjustment> adjustments = new ArrayList<>();

        for (int i = 0; i < billedCharges.size(); i++) {
            BigDecimal billed = billedCharges.get(i);
            BigDecimal allowed = allowedAmounts.get(i);

            if (billed == null || allowed == null) {
                throw new IllegalArgumentException(
                        "Billed and allowed amounts cannot be null");
            }
            if (billed.signum() < 0 || allowed.signum() < 0) {
                throw new IllegalArgumentException(
                        "Billed and allowed amounts cannot be negative");
            }

            billed = billed.setScale(scale, rounding);
            allowed = allowed.setScale(scale, rounding);

            billedTotal = billedTotal.add(billed);
            allowedTotal = allowedTotal.add(allowed);

            BigDecimal writeOff = billed.subtract(allowed);
            if (writeOff.signum() > 0) {
                writeOff = writeOff.setScale(scale, rounding);
                contractualWriteOff = contractualWriteOff.add(writeOff);

                adjustments.add(new Adjustment(
                        "CONTRACTUAL_WRITE_OFF",
                        "Difference between billed charge and allowed amount; not patient-billable",
                        writeOff,
                        Adjustment.AdjustmentType.CONTRACTUAL_WRITE_OFF));
            }
        }

        billedTotal = billedTotal.setScale(scale, rounding);
        allowedTotal = allowedTotal.setScale(scale, rounding);
        contractualWriteOff = contractualWriteOff.setScale(scale, rounding);

        payerResponsibility = allowedTotal
                .multiply(payer.payerShareRate())
                .setScale(scale, rounding);

        if (payerResponsibility.compareTo(allowedTotal) > 0) {
            payerResponsibility = allowedTotal;
        }

        BigDecimal patientResponsibility = allowedTotal
                .subtract(payerResponsibility)
                .setScale(scale, rounding);

        BigDecimal payments = priorPayments.setScale(scale, rounding);

        BigDecimal patientBalanceDue = patientResponsibility
                .subtract(payments);

        BigDecimal creditBalance = ZERO.setScale(scale, rounding);

        if (patientBalanceDue.signum() < 0) {
            creditBalance = patientBalanceDue.negate()
                    .setScale(scale, rounding);
            patientBalanceDue = ZERO.setScale(scale, rounding);
        } else {
            patientBalanceDue = patientBalanceDue.setScale(scale, rounding);
        }

        return new BillingResult(
                billedTotal,
                allowedTotal,
                contractualWriteOff,
                payerResponsibility,
                patientResponsibility,
                payments,
                patientBalanceDue,
                creditBalance,
                List.copyOf(adjustments));
    }

    public record BillingResult(
            BigDecimal billedTotal,
            BigDecimal allowedTotal,
            BigDecimal contractualWriteOff,
            BigDecimal payerResponsibility,
            BigDecimal patientResponsibility,
            BigDecimal priorPayments,
            BigDecimal patientBalanceDue,
            BigDecimal creditBalance,
            List<Adjustment> adjustments) {

        public BillingResult {
            Objects.requireNonNull(billedTotal);
            Objects.requireNonNull(allowedTotal);
            Objects.requireNonNull(contractualWriteOff);
            Objects.requireNonNull(payerResponsibility);
            Objects.requireNonNull(patientResponsibility);
            Objects.requireNonNull(priorPayments);
            Objects.requireNonNull(patientBalanceDue);
            Objects.requireNonNull(creditBalance);
            adjustments = List.copyOf(adjustments);
        }
    }
}