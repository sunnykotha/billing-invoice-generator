package com.billing.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public record Invoice(
        String invoiceId,
        Encounter encounter,
        Payer payer,
        List<ProcedureLine> procedures,
        List<Adjustment> adjustments,
        BigDecimal billedTotal,
        BigDecimal allowedTotal,
        BigDecimal payerResponsibility,
        BigDecimal patientResponsibility,
        BigDecimal priorPayments,
        BigDecimal patientBalanceDue,
        BigDecimal creditBalance) {

    public Invoice {
        Objects.requireNonNull(invoiceId, "Invoice ID is required");
        Objects.requireNonNull(encounter, "Encounter is required");
        Objects.requireNonNull(payer, "Payer is required");
        Objects.requireNonNull(procedures, "Procedures are required");
        Objects.requireNonNull(adjustments, "Adjustments are required");
        Objects.requireNonNull(billedTotal, "Billed total is required");
        Objects.requireNonNull(allowedTotal, "Allowed total is required");
        Objects.requireNonNull(payerResponsibility, "Payer responsibility is required");
        Objects.requireNonNull(patientResponsibility, "Patient responsibility is required");
        Objects.requireNonNull(priorPayments, "Prior payments are required");
        Objects.requireNonNull(patientBalanceDue, "Patient balance due is required");
        Objects.requireNonNull(creditBalance, "Credit balance is required");

        if (invoiceId.isBlank()) {
            throw new IllegalArgumentException("Invoice ID cannot be blank");
        }

        procedures = List.copyOf(procedures);
        adjustments = List.copyOf(adjustments);

        if (billedTotal.signum() < 0 || allowedTotal.signum() < 0
                || payerResponsibility.signum() < 0
                || patientResponsibility.signum() < 0 || priorPayments.signum() < 0
                || patientBalanceDue.signum() < 0 || creditBalance.signum() < 0) {
            throw new IllegalArgumentException("Invoice amounts cannot be negative");
        }
    }
}
