package com.billing.validation;

import com.billing.model.Encounter;
import com.billing.model.ProcedureLine;
import java.time.LocalDate;
import java.util.*;

public final class BillingValidator {
    private final Map<String, CodeValidity> cptCodes = new HashMap<>();
    private final Map<String, CodeValidity> icd10Codes = new HashMap<>();
    private final Map<String, Set<String>> allowedDiagnosesByCpt = new HashMap<>();

    public void registerCpt(String code, LocalDate validFrom, LocalDate validTo) {
        cptCodes.put(requireCode(code), new CodeValidity(validFrom, validTo));
    }

    public void registerIcd10(String code, LocalDate validFrom, LocalDate validTo) {
        icd10Codes.put(requireCode(code), new CodeValidity(validFrom, validTo));
    }

    public void allowDiagnosisForCpt(String cptCode, String icd10Code) {
        allowedDiagnosesByCpt.computeIfAbsent(requireCode(cptCode), k -> new HashSet<>())
                .add(requireCode(icd10Code));
    }

    public List<String> validate(Encounter encounter, List<ProcedureLine> procedures) {
        Objects.requireNonNull(encounter, "Encounter is required");
        Objects.requireNonNull(procedures, "Procedure list is required");

        List<String> errors = new ArrayList<>();
        LocalDate serviceDate = encounter.serviceDate();

        if (procedures.isEmpty()) {
            errors.add("Encounter must contain at least one procedure.");
            return List.copyOf(errors);
        }

        for (int i = 0; i < procedures.size(); i++) {
            ProcedureLine procedure = procedures.get(i);
            String cpt = procedure.cptCode();
            String prefix = "Procedure " + (i + 1) + " (" + cpt + "): ";

            CodeValidity cptValidity = cptCodes.get(cpt);
            if (cptValidity == null) {
                errors.add(prefix + "CPT code is not registered.");
            } else if (!cptValidity.isValidOn(serviceDate)) {
                errors.add(prefix + "CPT code is invalid on the encounter service date.");
            }

            if (procedure.diagnoses().isEmpty()) {
                errors.add(prefix + "At least one diagnosis is required.");
                continue;
            }

            Set<String> allowed = allowedDiagnosesByCpt.get(cpt);
            for (var diagnosis : procedure.diagnoses()) {
                String icd10 = diagnosis.icd10Code();
                CodeValidity icdValidity = icd10Codes.get(icd10);

                if (icdValidity == null) {
                    errors.add(prefix + "ICD-10 code " + icd10 + " is not registered.");
                } else if (!icdValidity.isValidOn(serviceDate)) {
                    errors.add(prefix + "ICD-10 code " + icd10
                            + " is invalid on the encounter service date.");
                }

                if (allowed == null || !allowed.contains(icd10)) {
                    errors.add(prefix + "CPT-to-ICD-10 pairing is not approved: "
                            + cpt + " / " + icd10 + ".");
                }
            }
        }

        return List.copyOf(errors);
    }

    private static String requireCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Code cannot be blank.");
        }
        return code.trim();
    }

    private record CodeValidity(LocalDate validFrom, LocalDate validTo) {
        private CodeValidity {
            Objects.requireNonNull(validFrom, "validFrom is required");
            if (validTo != null && validTo.isBefore(validFrom)) {
                throw new IllegalArgumentException("validTo cannot be before validFrom.");
            }
        }

        private boolean isValidOn(LocalDate date) {
            return !date.isBefore(validFrom)
                    && (validTo == null || !date.isAfter(validTo));
        }
    }
}