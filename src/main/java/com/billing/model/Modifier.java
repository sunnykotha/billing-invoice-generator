package com.billing.model;

public enum Modifier {
    BILATERAL_50("50"),
    MULTIPLE_PROCEDURE_51("51"),
    DISTINCT_PROCEDURAL_SERVICE_59;

    private final String code;

    Modifier() {
        this.code = "59";
    }

    Modifier(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
