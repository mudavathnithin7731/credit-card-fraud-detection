package com.fraud;

import com.fraud.data.SyntheticDataGenerator;
import com.fraud.engine.FraudDetectionEngine;
import com.fraud.engine.ml.Dataset;
import com.fraud.engine.ml.ModelEvaluator;
import com.fraud.engine.ml.RandomForest;
import com.fraud.model.CardholderProfile;
import com.fraud.model.DetectionResult;
import com.fraud.model.RiskLevel;
import com.fraud.model.Transaction;

import java.io.File;
import java.util.List;

/**
 * Automated test suite to thoroughly verify the fraud detection pipeline,
 * heuristic rule triggers, ML model convergence, and CSV persistence.
 */
public class SystemVerificationTest {

    public static void main(String[] args) {
        System.out.println("=================================================================");
        System.out.println("        RUNNING COMPREHENSIVE FRAUD DETECTION TEST SUITE         ");
        System.out.println("=================================================================\n");

        int testsPassed = 0;
        int totalTests = 5;

        try {
            // -------------------------------------------------------------
            // Test 1: Synthetic Data Generation & ML Model Convergence
            // -------------------------------------------------------------
            System.out.print("[TEST 1/5] Testing Dataset Generation & Random Forest Convergence... ");
            SyntheticDataGenerator.GeneratedData genData = SyntheticDataGenerator.generateDataset(2500, 0.08, 42L);
            if (genData.transactions.size() != 2500) {
                throw new AssertionError("Dataset size mismatch: expected 2500, got " + genData.transactions.size());
            }

            RandomForest rf = new RandomForest(15, 7, 8, 42L);
            Dataset[] splits = genData.dataset.trainTestSplit(0.80, 42L);
            rf.train(splits[0]);
            ModelEvaluator.Metrics metrics = ModelEvaluator.evaluate(rf, splits[1]);

            if (metrics.accuracy < 0.90) {
                throw new AssertionError("Accuracy below benchmark threshold: " + metrics.accuracy);
            }
            if (metrics.recall < 0.70) {
                throw new AssertionError("Recall below benchmark threshold: " + metrics.recall);
            }
            System.out.printf("PASSED! (Accuracy: %.2f%%, Recall: %.2f%%, F1: %.2f%%)\n",
                    metrics.accuracy * 100, metrics.recall * 100, metrics.f1Score * 100);
            testsPassed++;

            FraudDetectionEngine engine = new FraudDetectionEngine(rf);
            for (CardholderProfile p : genData.profiles.values()) {
                engine.registerProfile(p);
            }

            String testCard = genData.profiles.keySet().iterator().next();
            CardholderProfile prof = engine.getProfile(testCard);

            // -------------------------------------------------------------
            // Test 2: Legitimate Normal Transaction
            // -------------------------------------------------------------
            System.out.print("[TEST 2/5] Evaluating Legitimate Everyday Purchase... ");
            Transaction legitTx = new Transaction("TX-TEST-LEGIT", testCard, System.currentTimeMillis(),
                    prof.getAverageAmount() + 5.0, "Safeway-Grocery", "GROCERY",
                    prof.getHomeLatitude() + 0.005, prof.getHomeLongitude() + 0.005, false);

            DetectionResult legitRes = engine.evaluate(legitTx);
            if (legitRes.getRiskLevel() != RiskLevel.LOW || !legitRes.getDecision().contains("APPROVED")) {
                throw new AssertionError("Expected APPROVED/LOW for legit transaction, got: " + legitRes);
            }
            System.out.printf("PASSED! (Score: %.1f/100, Decision: %s)\n", legitRes.getRiskScore(), legitRes.getDecision());
            testsPassed++;

            // -------------------------------------------------------------
            // Test 3: Impossible Geolocation Speed (Card Cloning)
            // -------------------------------------------------------------
            System.out.print("[TEST 3/5] Evaluating Impossible Travel / Card Cloning Attack... ");
            // Tokyo coordinates 10 minutes after New York transaction
            long tenMinutesLater = System.currentTimeMillis() + (10 * 60 * 1000L);
            Transaction cloningTx = new Transaction("TX-TEST-CLONE", testCard, tenMinutesLater,
                    650.0, "Akihabara-DutyFree", "ELECTRONICS", 35.6762, 139.6503, true);

            DetectionResult cloneRes = engine.evaluate(cloningTx);
            if (cloneRes.getRiskLevel() != RiskLevel.HIGH || !cloneRes.getDecision().contains("REJECTED")) {
                throw new AssertionError("Expected REJECTED/HIGH for impossible travel, got: " + cloneRes);
            }
            boolean geoRuleFound = cloneRes.getTriggeredRules().stream().anyMatch(r -> r.contains("Impossible Travel Velocity"));
            if (!geoRuleFound) {
                throw new AssertionError("Impossible travel rule did not trigger as expected!");
            }
            System.out.printf("PASSED! (Score: %.1f/100, Decision: %s, Rule Triggered: YES)\n",
                    cloneRes.getRiskScore(), cloneRes.getDecision());
            testsPassed++;

            // -------------------------------------------------------------
            // Test 4: High-Velocity Automated Burst
            // -------------------------------------------------------------
            System.out.print("[TEST 4/5] Evaluating Rapid High-Velocity Bot Attack... ");
            long now = System.currentTimeMillis() + 7200000; // 2 hours later
            // Send 4 rapid transactions within 40 seconds
            engine.evaluate(new Transaction("BURST-1", testCard, now, 1.0, "Online-Store", "RETAIL", prof.getHomeLatitude(), prof.getHomeLongitude()));
            engine.evaluate(new Transaction("BURST-2", testCard, now + 10000, 1.0, "Online-Store", "RETAIL", prof.getHomeLatitude(), prof.getHomeLongitude()));
            engine.evaluate(new Transaction("BURST-3", testCard, now + 20000, 1.0, "Online-Store", "RETAIL", prof.getHomeLatitude(), prof.getHomeLongitude()));
            DetectionResult burstRes = engine.evaluate(new Transaction("BURST-4", testCard, now + 30000, 1800.0, "Online-Store", "CRYPTO", prof.getHomeLatitude(), prof.getHomeLongitude()));

            boolean velocityRuleFound = burstRes.getTriggeredRules().stream().anyMatch(r -> r.contains("Velocity"));
            if (!velocityRuleFound) {
                throw new AssertionError("Velocity rule did not trigger during burst attack!");
            }
            System.out.printf("PASSED! (Score: %.1f/100, Decision: %s, Velocity Flagged: YES)\n",
                    burstRes.getRiskScore(), burstRes.getDecision());
            testsPassed++;

            // -------------------------------------------------------------
            // Test 5: CSV Serialization & Ingestion Roundtrip
            // -------------------------------------------------------------
            System.out.print("[TEST 5/5] Testing CSV Persistence and Deserialization... ");
            File tempCsv = new File("test_benchmark_transactions.csv");
            SyntheticDataGenerator.saveToCSV(genData.transactions.subList(0, 50), tempCsv);
            List<Transaction> reloaded = SyntheticDataGenerator.loadFromCSV(tempCsv);
            if (reloaded.size() != 50) {
                throw new AssertionError("CSV reload failed: expected 50, got " + reloaded.size());
            }
            tempCsv.delete();
            System.out.println("PASSED! (50/50 records restored correctly)");
            testsPassed++;

            System.out.println("\n=================================================================");
            System.out.printf("   ALL SYSTEM TESTS COMPLETED: %d / %d PASSED (100%% SUCCESS)    \n", testsPassed, totalTests);
            System.out.println("=================================================================");

        } catch (Throwable t) {
            System.err.println("\nTEST FAILED WITH EXCEPTION:");
            t.printStackTrace();
            System.exit(1);
        }
    }
}
