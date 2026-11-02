package com.fraud.model;

import java.util.Collections;
import java.util.List;

/**
 * Encapsulates the evaluation outcome produced by the Fraud Detection Engine.
 */
public class DetectionResult {
    private final String transactionId;
    private final double riskScore; // 0.0 to 100.0
    private final RiskLevel riskLevel;
    private final double mlProbability; // 0.0 to 1.0
    private final double ruleScore; // 0.0 to 100.0
    private final List<String> triggeredRules;
    private final String decision; // APPROVED, FLAGGED, REJECTED

    public DetectionResult(String transactionId, double riskScore, RiskLevel riskLevel,
                           double mlProbability, double ruleScore, List<String> triggeredRules,
                           String decision) {
        this.transactionId = transactionId;
        this.riskScore = Math.max(0.0, Math.min(100.0, riskScore));
        this.riskLevel = riskLevel;
        this.mlProbability = mlProbability;
        this.ruleScore = ruleScore;
        this.triggeredRules = triggeredRules != null ? triggeredRules : Collections.emptyList();
        this.decision = decision;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public double getRiskScore() {
        return riskScore;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public double getMlProbability() {
        return mlProbability;
    }

    public double getRuleScore() {
        return ruleScore;
    }

    public List<String> getTriggeredRules() {
        return Collections.unmodifiableList(triggeredRules);
    }

    public String getDecision() {
        return decision;
    }

    public boolean isFraud() {
        return riskLevel == RiskLevel.HIGH || riskScore >= 70.0;
    }

    @Override
    public String toString() {
        return String.format("Result[Tx: %s | Score: %.1f/100 | Level: %s | Decision: %s | Rules Triggered: %d]",
                transactionId, riskScore, riskLevel, decision, triggeredRules.size());
    }
}
