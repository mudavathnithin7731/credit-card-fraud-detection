package com.fraud.engine.ml;

import com.fraud.engine.rules.GeolocationRule;
import com.fraud.model.CardholderProfile;
import com.fraud.model.Transaction;

import java.util.HashMap;
import java.util.Map;

/**
 * Transforms domain objects (Transaction + CardholderProfile) into numeric feature vectors for ML models.
 */
public class FeatureExtractor {
    public static final String[] FEATURE_NAMES = {
            "Transaction Amount",
            "Amount-to-Avg Ratio",
            "Amount Z-Score",
            "Normalized Hour (0-1)",
            "Distance from Home (km)",
            "Transit Speed (km/h)",
            "Velocity Count (2 Min)",
            "Velocity Count (10 Min)",
            "Merchant Risk Score"
    };

    private static final Map<String, Double> MERCHANT_SCORE_MAP = new HashMap<>();

    static {
        MERCHANT_SCORE_MAP.put("CRYPTO", 0.90);
        MERCHANT_SCORE_MAP.put("GAMBLING", 0.95);
        MERCHANT_SCORE_MAP.put("CASINO", 0.95);
        MERCHANT_SCORE_MAP.put("WIRE_TRANSFER", 0.85);
        MERCHANT_SCORE_MAP.put("LUXURY_JEWELRY", 0.70);
        MERCHANT_SCORE_MAP.put("UNKNOWN", 0.60);
        MERCHANT_SCORE_MAP.put("ELECTRONICS", 0.45);
        MERCHANT_SCORE_MAP.put("TRAVEL", 0.35);
        MERCHANT_SCORE_MAP.put("RETAIL", 0.15);
        MERCHANT_SCORE_MAP.put("GROCERY", 0.05);
        MERCHANT_SCORE_MAP.put("DINING", 0.05);
        MERCHANT_SCORE_MAP.put("UTILITIES", 0.02);
    }

    public static double[] extractFeatures(Transaction tx, CardholderProfile profile) {
        double[] features = new double[FEATURE_NAMES.length];

        double amount = tx.getAmount();
        double avg = profile != null ? profile.getAverageAmount() : 50.0;
        double stdDev = profile != null ? profile.getStandardDeviation() : 20.0;

        // 0: Transaction Amount
        features[0] = amount;

        // 1: Amount-to-Avg Ratio
        features[1] = amount / Math.max(1.0, avg);

        // 2: Amount Z-Score
        features[2] = (amount - avg) / Math.max(1.0, stdDev);

        // 3: Normalized Hour
        int hour = tx.getDateTime().getHour();
        features[3] = hour / 24.0;

        // 4: Distance from Home
        if (profile != null) {
            features[4] = GeolocationRule.calculateDistanceKm(
                    profile.getHomeLatitude(), profile.getHomeLongitude(),
                    tx.getLatitude(), tx.getLongitude()
            );
        } else {
            features[4] = 0.0;
        }

        // 5: Transit Speed from Last Transaction
        if (profile != null && profile.getLastTransactionTimestamp() > 0) {
            long timeDiffMillis = tx.getTimestamp() - profile.getLastTransactionTimestamp();
            double hours = Math.max(0.0001, timeDiffMillis / (1000.0 * 3600.0));
            double distanceKm = GeolocationRule.calculateDistanceKm(
                    profile.getLastLatitude(), profile.getLastLongitude(),
                    tx.getLatitude(), tx.getLongitude()
            );
            features[5] = distanceKm / hours;
        } else {
            features[5] = 0.0;
        }

        // 6: Velocity Count in 2 Min
        features[6] = profile != null ? profile.getTransactionCountInLastMinutes(tx.getTimestamp(), 2) : 1;

        // 7: Velocity Count in 10 Min
        features[7] = profile != null ? profile.getTransactionCountInLastMinutes(tx.getTimestamp(), 10) : 1;

        // 8: Merchant Category Risk
        String category = tx.getMerchantCategory() != null ? tx.getMerchantCategory().trim().toUpperCase() : "UNKNOWN";
        features[8] = MERCHANT_SCORE_MAP.getOrDefault(category, 0.20);

        return features;
    }

    public static DataPoint toDataPoint(Transaction tx, CardholderProfile profile) {
        double[] f = extractFeatures(tx, profile);
        return new DataPoint(f, tx.isFraudActual() ? 1 : 0);
    }
}
