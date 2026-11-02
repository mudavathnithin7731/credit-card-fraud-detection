package com.fraud.engine.rules;

import com.fraud.model.CardholderProfile;
import com.fraud.model.Transaction;

import java.util.HashMap;
import java.util.Map;

/**
 * Assesses risk weights associated with merchant trade categories and high-risk verticals.
 */
public class MerchantRiskRule implements FraudRule {
    private static final Map<String, Double> CATEGORY_RISK_MAP = new HashMap<>();

    static {
        // Industry baseline risk multipliers
        CATEGORY_RISK_MAP.put("CRYPTO", 45.0);
        CATEGORY_RISK_MAP.put("GAMBLING", 50.0);
        CATEGORY_RISK_MAP.put("CASINO", 50.0);
        CATEGORY_RISK_MAP.put("WIRE_TRANSFER", 40.0);
        CATEGORY_RISK_MAP.put("LUXURY_JEWELRY", 35.0);
        CATEGORY_RISK_MAP.put("UNKNOWN", 30.0);
        CATEGORY_RISK_MAP.put("ELECTRONICS", 20.0);
        CATEGORY_RISK_MAP.put("TRAVEL", 15.0);
        CATEGORY_RISK_MAP.put("RETAIL", 5.0);
        CATEGORY_RISK_MAP.put("GROCERY", 2.0);
        CATEGORY_RISK_MAP.put("DINING", 2.0);
        CATEGORY_RISK_MAP.put("UTILITIES", 1.0);
    }

    @Override
    public String getRuleName() {
        return "Merchant Category Risk Rule";
    }

    @Override
    public double evaluate(Transaction transaction, CardholderProfile profile) {
        String category = transaction.getMerchantCategory();
        if (category == null) {
            return 20.0;
        }

        String normalized = category.trim().toUpperCase();
        double baseRisk = CATEGORY_RISK_MAP.getOrDefault(normalized, 10.0);

        // High value purchase in a high-risk category escalates penalty
        if (baseRisk >= 35.0 && transaction.getAmount() > 1000.0) {
            return Math.min(85.0, baseRisk + 35.0);
        } else if (baseRisk >= 35.0 && transaction.getAmount() > 400.0) {
            return Math.min(70.0, baseRisk + 20.0);
        }

        return baseRisk >= 35.0 ? baseRisk : 0.0; // Only flag if significant
    }

    @Override
    public String getViolationReason(Transaction transaction, CardholderProfile profile) {
        String category = transaction.getMerchantCategory();
        if (category == null) {
            return "Unclassified merchant category.";
        }
        String normalized = category.trim().toUpperCase();
        double baseRisk = CATEGORY_RISK_MAP.getOrDefault(normalized, 10.0);

        if (baseRisk >= 35.0) {
            return String.format("Elevated merchant risk category '%s' with amount $%.2f.",
                    normalized, transaction.getAmount());
        }
        return null;
    }
}
