package com.fraud.engine;

import com.fraud.engine.ml.DataPoint;
import com.fraud.engine.ml.FeatureExtractor;
import com.fraud.engine.ml.RandomForest;
import com.fraud.engine.rules.*;
import com.fraud.model.CardholderProfile;
import com.fraud.model.DetectionResult;
import com.fraud.model.RiskLevel;
import com.fraud.model.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core Orchestrator implementing the Hybrid Fraud Detection architecture.
 * Synthesizes Rule-Based Heuristics with Machine Learning ensemble predictions.
 */
public class FraudDetectionEngine {
    private final List<FraudRule> rules;
    private final RandomForest mlModel;
    private final Map<String, CardholderProfile> profileCache;

    public FraudDetectionEngine(RandomForest mlModel) {
        this.mlModel = mlModel != null ? mlModel : new RandomForest();
        this.rules = new ArrayList<>();
        this.profileCache = new ConcurrentHashMap<>();

        // Register default heuristic rules
        rules.add(new VelocityRule());
        rules.add(new GeolocationRule());
        rules.add(new AmountAnomalyRule());
        rules.add(new MerchantRiskRule());
        rules.add(new UnusualTimeRule());
    }

    public void registerProfile(CardholderProfile profile) {
        if (profile != null && profile.getCardNumber() != null) {
            profileCache.put(profile.getCardNumber(), profile);
        }
    }

    public CardholderProfile getProfile(String cardNumber) {
        return profileCache.get(cardNumber);
    }

    public CardholderProfile getOrCreateProfile(String cardNumber, double defaultAvgAmount,
                                               double defaultLat, double defaultLon) {
        return profileCache.computeIfAbsent(cardNumber,
                k -> new CardholderProfile(k, defaultAvgAmount, 25.0, defaultLat, defaultLon));
    }

    /**
     * Evaluates a transaction through both the Rule Heuristics and the ML Model.
     */
    public DetectionResult evaluate(Transaction tx) {
        CardholderProfile profile = profileCache.get(tx.getCardNumber());

        // 1. Evaluate Rule-Based Heuristics
        List<String> triggeredRules = new ArrayList<>();
        double maxRulePenalty = 0.0;
        double sumRulePenalty = 0.0;
        int activeRuleCount = 0;

        for (FraudRule rule : rules) {
            double penalty = rule.evaluate(tx, profile);
            if (penalty > 0.0) {
                activeRuleCount++;
                sumRulePenalty += penalty;
                if (penalty > maxRulePenalty) {
                    maxRulePenalty = penalty;
                }
                String reason = rule.getViolationReason(tx, profile);
                if (reason != null && !reason.isEmpty()) {
                    triggeredRules.add("[" + rule.getRuleName() + "] " + reason);
                }
            }
        }

        // Rule score combines highest severity trigger with cumulative signals
        double ruleScore = 0.0;
        if (activeRuleCount > 0) {
            ruleScore = Math.min(100.0, (maxRulePenalty * 0.70) + ((sumRulePenalty / activeRuleCount) * 0.30));
        }

        // 2. Evaluate Machine Learning Model
        DataPoint dataPoint = FeatureExtractor.toDataPoint(tx, profile);
        double mlProb = mlModel.predictProbability(dataPoint);
        double mlScore = mlProb * 100.0;

        // 3. Hybrid Fusion
        // If an acute critical rule triggers (e.g. impossible travel speed > 900 km/h), rule score takes precedence
        double compositeScore;
        if (maxRulePenalty >= 85.0) {
            compositeScore = Math.max(ruleScore, (0.60 * ruleScore + 0.40 * mlScore));
        } else {
            compositeScore = (0.50 * ruleScore) + (0.50 * mlScore);
        }

        compositeScore = Math.max(0.0, Math.min(100.0, compositeScore));

        // 4. Decision Tiers
        RiskLevel riskLevel;
        String decision;

        if (compositeScore >= 70.0) {
            riskLevel = RiskLevel.HIGH;
            decision = "REJECTED";
        } else if (compositeScore >= 35.0) {
            riskLevel = RiskLevel.MEDIUM;
            decision = "FLAGGED (2FA / OTP Required)";
        } else {
            riskLevel = RiskLevel.LOW;
            decision = "APPROVED";
        }

        // 5. Update Profile if transaction was processed
        if (profile != null) {
            profile.recordTransaction(tx);
        }

        return new DetectionResult(tx.getTransactionId(), compositeScore, riskLevel,
                mlProb, ruleScore, triggeredRules, decision);
    }

    public List<FraudRule> getRules() {
        return rules;
    }

    public RandomForest getMlModel() {
        return mlModel;
    }
}
