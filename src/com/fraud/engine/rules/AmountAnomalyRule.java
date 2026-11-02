package com.fraud.engine.rules;

import com.fraud.model.CardholderProfile;
import com.fraud.model.Transaction;

/**
 * Detects statistical anomalies in transaction spending amounts relative to
 * the cardholder's historical spending profile (Z-Score and ratio deviations).
 */
public class AmountAnomalyRule implements FraudRule {
    private final double zScoreThreshold;

    public AmountAnomalyRule(double zScoreThreshold) {
        this.zScoreThreshold = zScoreThreshold;
    }

    public AmountAnomalyRule() {
        this(3.5); // Standard outlier threshold (3.5 sigma)
    }

    @Override
    public String getRuleName() {
        return "Amount Outlier & Anomaly Rule";
    }

    @Override
    public double evaluate(Transaction transaction, CardholderProfile profile) {
        if (profile == null) {
            // General high-value fallback without profile
            if (transaction.getAmount() > 5000.0) {
                return 40.0;
            }
            return 0.0;
        }

        double amount = transaction.getAmount();
        double avg = profile.getAverageAmount();
        double stdDev = profile.getStandardDeviation();

        // 1. Check for card testing micro-transactions ($0.01 - $1.00)
        if (amount <= 1.00 && avg >= 40.0) {
            return 35.0; // Suspicious micro-authorization probe
        }

        // 2. Statistical Z-Score deviation
        double zScore = (amount - avg) / stdDev;

        if (zScore >= 6.0 || (avg > 0 && amount >= avg * 10.0)) {
            return 80.0; // Extreme spending spike
        } else if (zScore >= zScoreThreshold || (avg > 0 && amount >= avg * 5.0)) {
            return 50.0; // Significant outlier
        } else if (zScore >= 2.5 && amount >= avg * 3.0) {
            return 25.0; // Moderate spike
        }

        return 0.0;
    }

    @Override
    public String getViolationReason(Transaction transaction, CardholderProfile profile) {
        if (profile == null) {
            if (transaction.getAmount() > 5000.0) {
                return String.format("High transaction amount ($%.2f) exceeds general safe threshold.", transaction.getAmount());
            }
            return null;
        }

        double amount = transaction.getAmount();
        double avg = profile.getAverageAmount();
        double stdDev = profile.getStandardDeviation();

        if (amount <= 1.00 && avg >= 40.0) {
            return String.format("Potential micro-probing: $%.2f transaction on a card with $%.2f average spend.", amount, avg);
        }

        double zScore = (amount - avg) / stdDev;
        if (zScore >= zScoreThreshold || amount >= avg * 5.0) {
            return String.format("Severe amount anomaly: $%.2f is %.1fx cardholder average ($%.2f, Z-score: +%.2fσ).",
                    amount, amount / Math.max(1.0, avg), avg, zScore);
        } else if (zScore >= 2.5 && amount >= avg * 3.0) {
            return String.format("Moderate amount anomaly: $%.2f is %.1fx cardholder average ($%.2f).",
                    amount, amount / Math.max(1.0, avg), avg);
        }

        return null;
    }
}
