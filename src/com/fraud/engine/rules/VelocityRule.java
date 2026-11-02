package com.fraud.engine.rules;

import com.fraud.model.CardholderProfile;
import com.fraud.model.Transaction;

/**
 * Flags sudden high-frequency transaction bursts (common in card testing or automated bots).
 */
public class VelocityRule implements FraudRule {
    private final int maxAllowedInTwoMinutes;
    private final int maxAllowedInTenMinutes;

    public VelocityRule(int maxAllowedInTwoMinutes, int maxAllowedInTenMinutes) {
        this.maxAllowedInTwoMinutes = maxAllowedInTwoMinutes;
        this.maxAllowedInTenMinutes = maxAllowedInTenMinutes;
    }

    public VelocityRule() {
        this(3, 6);
    }

    @Override
    public String getRuleName() {
        return "Transaction Velocity Rule";
    }

    @Override
    public double evaluate(Transaction transaction, CardholderProfile profile) {
        if (profile == null) {
            return 0.0;
        }

        int count2Min = profile.getTransactionCountInLastMinutes(transaction.getTimestamp(), 2);
        int count10Min = profile.getTransactionCountInLastMinutes(transaction.getTimestamp(), 10);

        if (count2Min >= maxAllowedInTwoMinutes + 1) {
            return 85.0; // Extreme burst (bot activity / card testing)
        } else if (count2Min >= maxAllowedInTwoMinutes) {
            return 55.0; // Suspicious rapid succession
        } else if (count10Min >= maxAllowedInTenMinutes) {
            return 40.0; // High frequency in 10-minute window
        }

        return 0.0;
    }

    @Override
    public String getViolationReason(Transaction transaction, CardholderProfile profile) {
        if (profile == null) {
            return null;
        }
        int count2Min = profile.getTransactionCountInLastMinutes(transaction.getTimestamp(), 2);
        int count10Min = profile.getTransactionCountInLastMinutes(transaction.getTimestamp(), 10);

        if (count2Min >= maxAllowedInTwoMinutes) {
            return String.format("Rapid transaction velocity: %d transactions observed in the last 2 minutes.", count2Min + 1);
        } else if (count10Min >= maxAllowedInTenMinutes) {
            return String.format("Elevated frequency: %d transactions observed in the last 10 minutes.", count10Min + 1);
        }
        return null;
    }
}
