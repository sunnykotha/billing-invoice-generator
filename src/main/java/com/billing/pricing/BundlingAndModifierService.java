package com.billing.pricing;

import com.billing.model.Adjustment;
import com.billing.model.Encounter;
import com.billing.model.Modifier;
import com.billing.model.Payer;
import com.billing.model.ProcedureLine;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class BundlingAndModifierService {

    private static final BigDecimal BILATERAL_FACTOR = new BigDecimal("1.50");
    private static final BigDecimal MULTIPLE_PROCEDURE_FACTOR = new BigDecimal("0.50");

    private final Map<String, Set<String>> bundledComponentsByPrimary = new HashMap<>();

    public void registerBundledComponent(String primaryCptCode, String componentCptCode) {
        if (primaryCptCode == null || primaryCptCode.isBlank()) {
            throw new IllegalArgumentException("Primary CPT code is required");
        }
        if (componentCptCode == null || componentCptCode.isBlank()) {
            throw new IllegalArgumentException("Component CPT code is required");
        }
        if (primaryCptCode.equals(componentCptCode)) {
            throw new IllegalArgumentException("A CPT code cannot bundle itself");
        }

        bundledComponentsByPrimary
                .computeIfAbsent(primaryCptCode, ignored -> new HashSet<>())
                .add(componentCptCode);
    }

    public List<LineResult> apply(
            Encounter encounter,
            List<ProcedureLine> procedures,
            List<BigDecimal> baseAllowedAmounts,
            Payer payer) {

        Objects.requireNonNull(encounter, "Encounter is required");
        Objects.requireNonNull(procedures, "Procedures are required");
        Objects.requireNonNull(baseAllowedAmounts, "Base allowed amounts are required");
        Objects.requireNonNull(payer, "Payer is required");

        if (procedures.isEmpty()) {
            throw new IllegalArgumentException("At least one procedure is required");
        }
        if (procedures.size() != baseAllowedAmounts.size()) {
            throw new IllegalArgumentException(
                    "Each procedure must have exactly one base allowed amount");
        }

        for (BigDecimal amount : baseAllowedAmounts) {
            if (amount == null || amount.signum() < 0) {
                throw new IllegalArgumentException(
                        "Base allowed amounts must be non-null and non-negative");
            }
        }

        List<LineResult> results = new ArrayList<>();
        int eligibleProcedureCount = 0;

        for (int i = 0; i < procedures.size(); i++) {
            ProcedureLine procedure = Objects.requireNonNull(
                    procedures.get(i), "Procedure cannot be null");

            BigDecimal baseAmount = baseAllowedAmounts.get(i)
                    .setScale(payer.currencyScale(), payer.roundingMode());

            boolean bundled = isBundled(procedure, procedures);
            List<String> messages = new ArrayList<>();
            List<Adjustment> adjustments = new ArrayList<>();

            BigDecimal adjustedAmount;

            if (bundled) {
                adjustedAmount = BigDecimal.ZERO
                        .setScale(payer.currencyScale(), payer.roundingMode());

                messages.add("Bundled component is not separately payable");

                if (baseAmount.signum() > 0) {
                    adjustments.add(new Adjustment(
                            "BUNDLING",
                            "Component procedure bundled with a primary procedure",
                            baseAmount,
                            Adjustment.AdjustmentType.BUNDLING));
                }
            } else {
                adjustedAmount = baseAmount;

                if (procedure.modifiers().contains(Modifier.BILATERAL_50)) {
                    adjustedAmount = adjustedAmount.multiply(BILATERAL_FACTOR);
                    messages.add("Modifier 50: rate increased to 150%");
                }

                if (procedure.modifiers().contains(Modifier.MULTIPLE_PROCEDURE_51)
                        && eligibleProcedureCount > 0) {
                    adjustedAmount = adjustedAmount.multiply(MULTIPLE_PROCEDURE_FACTOR);
                    messages.add("Modifier 51: rate reduced to 50%");
                }

                adjustedAmount = adjustedAmount.setScale(
                        payer.currencyScale(), payer.roundingMode());

                eligibleProcedureCount++;
            }

            results.add(new LineResult(
                    procedure.cptCode(),
                    baseAmount,
                    adjustedAmount,
                    bundled,
                    List.copyOf(messages),
                    List.copyOf(adjustments)));
        }

        return List.copyOf(results);
    }

    private boolean isBundled(
            ProcedureLine procedure,
            List<ProcedureLine> allProcedures) {

        if (procedure.modifiers().contains(Modifier.DISTINCT_PROCEDURAL_SERVICE_59)) {
            return false;
        }

        for (ProcedureLine possiblePrimary : allProcedures) {
            if (possiblePrimary == procedure) {
                continue;
            }

            Set<String> components =
                    bundledComponentsByPrimary.get(possiblePrimary.cptCode());

            if (components != null && components.contains(procedure.cptCode())) {
                return true;
            }
        }

        return false;
    }

    public record LineResult(
            String cptCode,
            BigDecimal baseAllowedAmount,
            BigDecimal adjustedAllowedAmount,
            boolean bundled,
            List<String> ruleMessages,
            List<Adjustment> adjustments) {

        public LineResult {
            Objects.requireNonNull(cptCode, "CPT code is required");
            Objects.requireNonNull(baseAllowedAmount, "Base amount is required");
            Objects.requireNonNull(adjustedAllowedAmount, "Adjusted amount is required");
            ruleMessages = List.copyOf(ruleMessages);
            adjustments = List.copyOf(adjustments);
        }
    }
}