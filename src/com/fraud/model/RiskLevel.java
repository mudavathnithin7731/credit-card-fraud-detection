package com.fraud.model;

/**
 * Represents the risk classification level of a credit card transaction.
 */
public enum RiskLevel {
    LOW("Approved", "#28a745"),
    MEDIUM("Flagged for Verification", "#ffc107"),
    HIGH("High Risk / Blocked", "#dc3545");

    private final String displayLabel;
    private final String colorHex;

    RiskLevel(String displayLabel, String colorHex) {
        this.displayLabel = displayLabel;
        this.colorHex = colorHex;
    }

    public String getDisplayLabel() {
        return displayLabel;
    }

    public String getColorHex() {
        return colorHex;
    }
}
