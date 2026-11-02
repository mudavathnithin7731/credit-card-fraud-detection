package com.fraud.ui;

import com.fraud.data.SyntheticDataGenerator;
import com.fraud.engine.FraudDetectionEngine;
import com.fraud.engine.ml.Dataset;
import com.fraud.engine.ml.ModelEvaluator;
import com.fraud.engine.ml.RandomForest;
import com.fraud.model.CardholderProfile;
import com.fraud.model.DetectionResult;
import com.fraud.model.Transaction;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Interactive Command-Line Interface providing full system control, dataset benchmarking,
 * model training, interactive transaction inspection, and batch stream simulation.
 */
public class ConsoleApp {
    private final FraudDetectionEngine engine;
    private final RandomForest model;
    private SyntheticDataGenerator.GeneratedData benchmarkData;
    private ModelEvaluator.Metrics latestMetrics;
    private final Scanner scanner;

    public ConsoleApp() {
        this.model = new RandomForest(20, 7, 8, 42L);
        this.engine = new FraudDetectionEngine(model);
        this.scanner = new Scanner(System.in);
    }

    public void start() {
        printBanner();
        // Initialize default baseline dataset
        System.out.println("Initializing baseline dataset (3,000 transactions)...");
        benchmarkData = SyntheticDataGenerator.generateDataset(3000, 0.08, 42L);
        for (CardholderProfile p : benchmarkData.profiles.values()) {
            engine.registerProfile(p);
        }
        System.out.println("Training Random Forest ensemble (20 bagging decision trees)...");
        Dataset[] split = benchmarkData.dataset.trainTestSplit(0.80, 42L);
        model.train(split[0]);
        latestMetrics = ModelEvaluator.evaluate(model, split[1]);
        System.out.println("System initialized successfully! Model F1-Score: "
                + String.format("%.2f%%", latestMetrics.f1Score * 100));

        boolean running = true;
        while (running) {
            printMenu();
            System.out.print("Enter choice (1-7): ");
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1":
                    handleRegenerateDataset();
                    break;
                case "2":
                    handleModelEvaluation();
                    break;
                case "3":
                    handleManualTransaction();
                    break;
                case "4":
                    handleStreamingSimulation();
                    break;
                case "5":
                    handleViewProfiles();
                    break;
                case "6":
                    handleExportCSV();
                    break;
                case "7":
                    System.out.println("\nExiting Credit Card Fraud Detection System. Goodbye!");
                    running = false;
                    break;
                default:
                    System.out.println("Invalid choice. Please enter a number between 1 and 7.");
            }
        }
    }

    private void printBanner() {
        System.out.println("===============================================================================");
        System.out.println("       CREDIT CARD FRAUD DETECTION SYSTEM (HYBRID ML + HEURISTIC ENGINE)       ");
        System.out.println("===============================================================================");
    }

    private void printMenu() {
        System.out.println("\n--------------------------------- MAIN MENU ----------------------------------");
        System.out.println(" 1. Ingest / Re-generate Synthetic Benchmark Dataset");
        System.out.println(" 2. View Machine Learning Model Performance Metrics (Confusion Matrix)");
        System.out.println(" 3. Evaluate Single Transaction (Real-Time Risk Scoring)");
        System.out.println(" 4. Run Live Transaction Stream Simulator (20 Transactions)");
        System.out.println(" 5. View Registered Cardholder Profiles & Geolocation Baselines");
        System.out.println(" 6. Export Benchmark Transactions to CSV");
        System.out.println(" 7. Exit System");
        System.out.println("------------------------------------------------------------------------------");
    }

    private void handleRegenerateDataset() {
        System.out.print("\nEnter number of transactions to generate (e.g., 5000): ");
        int count = 5000;
        try {
            count = Integer.parseInt(scanner.nextLine().trim());
        } catch (Exception ignored) {}

        System.out.print("Enter target fraud ratio (e.g., 0.08 for 8%): ");
        double ratio = 0.08;
        try {
            ratio = Double.parseDouble(scanner.nextLine().trim());
        } catch (Exception ignored) {}

        System.out.println("\nGenerating " + count + " transactions with " + (ratio * 100) + "% fraud ratio...");
        benchmarkData = SyntheticDataGenerator.generateDataset(count, ratio, System.currentTimeMillis());
        for (CardholderProfile p : benchmarkData.profiles.values()) {
            engine.registerProfile(p);
        }

        System.out.println("Re-training Random Forest model...");
        Dataset[] split = benchmarkData.dataset.trainTestSplit(0.80, 42L);
        model.train(split[0]);
        latestMetrics = ModelEvaluator.evaluate(model, split[1]);
        System.out.println("Training complete!");
        System.out.println(latestMetrics.getFormattedSummary());
    }

    private void handleModelEvaluation() {
        if (latestMetrics != null) {
            System.out.println(latestMetrics.getFormattedSummary());
        } else {
            System.out.println("No evaluation metrics available yet. Please train the model first.");
        }
    }

    private void handleManualTransaction() {
        System.out.println("\n--- Interactive Transaction Risk Assessment ---");
        System.out.println("Known Cards: ");
        int idx = 1;
        for (String card : benchmarkData.profiles.keySet()) {
            System.out.printf("  [%d] %s (Avg Spend: $%.2f)\n", idx++, card, benchmarkData.profiles.get(card).getAverageAmount());
            if (idx > 5) break;
        }

        System.out.print("\nEnter Card Number (or press Enter for default): ");
        String card = scanner.nextLine().trim();
        if (card.isEmpty()) {
            card = benchmarkData.profiles.keySet().iterator().next();
        }

        CardholderProfile profile = engine.getProfile(card);
        if (profile == null) {
            profile = engine.getOrCreateProfile(card, 50.0, 40.7128, -74.0060);
        }

        System.out.printf("Selected Card: %s (Baseline Avg: $%.2f, Home Lat/Lon: %.4f, %.4f)\n",
                card, profile.getAverageAmount(), profile.getHomeLatitude(), profile.getHomeLongitude());

        System.out.print("Enter Transaction Amount ($): ");
        double amount = 45.0;
        try {
            amount = Double.parseDouble(scanner.nextLine().trim());
        } catch (Exception ignored) {}

        System.out.print("Enter Merchant Name: ");
        String merchant = scanner.nextLine().trim();
        if (merchant.isEmpty()) merchant = "BestBuy-Electronics";

        System.out.print("Enter Category [GROCERY, DINING, RETAIL, ELECTRONICS, CRYPTO, GAMBLING, LUXURY_JEWELRY]: ");
        String category = scanner.nextLine().trim().toUpperCase();
        if (category.isEmpty()) category = "ELECTRONICS";

        System.out.printf("Enter Latitude (Press Enter for Home: %.4f, or Tokyo: 35.6762): ", profile.getHomeLatitude());
        String latStr = scanner.nextLine().trim();
        double lat = latStr.isEmpty() ? profile.getHomeLatitude() : Double.parseDouble(latStr);

        System.out.printf("Enter Longitude (Press Enter for Home: %.4f, or Tokyo: 139.6503): ", profile.getHomeLongitude());
        String lonStr = scanner.nextLine().trim();
        double lon = lonStr.isEmpty() ? profile.getHomeLongitude() : Double.parseDouble(lonStr);

        Transaction tx = new Transaction("TX-MANUAL-" + System.currentTimeMillis() % 10000,
                card, System.currentTimeMillis(), amount, merchant, category, lat, lon);

        DetectionResult result = engine.evaluate(tx);

        System.out.println("\n=======================================================");
        System.out.println("                 RISK EVALUATION RESULT                ");
        System.out.println("=======================================================");
        System.out.printf(" Transaction ID   : %s\n", result.getTransactionId());
        System.out.printf(" Final Risk Score : %5.1f / 100.0\n", result.getRiskScore());
        System.out.printf(" Risk Level       : %s\n", result.getRiskLevel().getDisplayLabel());
        System.out.printf(" System Decision  : %s\n", result.getDecision());
        System.out.printf(" ML Probability   : %5.1f%%\n", result.getMlProbability() * 100);
        System.out.printf(" Rule Penalty     : %5.1f / 100.0\n", result.getRuleScore());
        System.out.println(" Triggered Reason Codes:");
        if (result.getTriggeredRules().isEmpty()) {
            System.out.println("   - None. Transaction adheres to normal cardholder profile.");
        } else {
            for (String r : result.getTriggeredRules()) {
                System.out.println("   * " + r);
            }
        }
        System.out.println("=======================================================");
    }

    private void handleStreamingSimulation() {
        System.out.println("\n--- Streaming Simulation: Evaluating 20 Sample Transactions ---");
        System.out.printf("%-14s | %-16s | %-9s | %-14s | %-10s | %-12s | %s\n",
                "Transaction ID", "Card", "Amount", "Category", "Risk Score", "Decision", "Flags");
        System.out.println("------------------------------------------------------------------------------------------------------");

        List<Transaction> samples = benchmarkData.transactions.subList(0, Math.min(20, benchmarkData.transactions.size()));
        for (Transaction tx : samples) {
            DetectionResult res = engine.evaluate(tx);
            String flags = res.getTriggeredRules().isEmpty() ? "Normal" : res.getTriggeredRules().get(0);
            if (flags.length() > 32) flags = flags.substring(0, 29) + "...";

            System.out.printf("%-14s | %-16s | $%-8.2f | %-14s | %5.1f/100  | %-12s | %s\n",
                    tx.getTransactionId(), tx.getMaskedCardNumber(), tx.getAmount(),
                    tx.getMerchantCategory(), res.getRiskScore(), res.getDecision(), flags);
        }
        System.out.println("------------------------------------------------------------------------------------------------------");
    }

    private void handleViewProfiles() {
        System.out.println("\n--- Registered Cardholder Profiles ---");
        for (Map.Entry<String, CardholderProfile> entry : benchmarkData.profiles.entrySet()) {
            CardholderProfile p = entry.getValue();
            System.out.printf("Card: %s | Baseline Spend: $%.2f (±$%.2f) | Home: (%.4f, %.4f) | Last Tx: (%.4f, %.4f)\n",
                    entry.getKey(), p.getAverageAmount(), p.getStandardDeviation(),
                    p.getHomeLatitude(), p.getHomeLongitude(), p.getLastLatitude(), p.getLastLongitude());
        }
    }

    private void handleExportCSV() {
        File file = new File("transactions_export.csv");
        try {
            SyntheticDataGenerator.saveToCSV(benchmarkData.transactions, file);
            System.out.println("Successfully exported " + benchmarkData.transactions.size()
                    + " transactions to: " + file.getAbsolutePath());
        } catch (Exception e) {
            System.out.println("Export failed: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        new ConsoleApp().start();
    }
}
