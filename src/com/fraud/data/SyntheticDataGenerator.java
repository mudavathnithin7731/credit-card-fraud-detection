package com.fraud.data;

import com.fraud.engine.ml.DataPoint;
import com.fraud.engine.ml.Dataset;
import com.fraud.engine.ml.FeatureExtractor;
import com.fraud.model.CardholderProfile;
import com.fraud.model.Transaction;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

/**
 * Generates realistic benchmark credit card transactions with both legitimate
 * behavioral baselines and authentic fraud attack vectors (card cloning, bot velocity,
 * account takeover, and spending anomalies).
 */
public class SyntheticDataGenerator {

    public static class GeneratedData {
        public final List<Transaction> transactions;
        public final Map<String, CardholderProfile> profiles;
        public final Dataset dataset;

        public GeneratedData(List<Transaction> transactions, Map<String, CardholderProfile> profiles, Dataset dataset) {
            this.transactions = transactions;
            this.profiles = profiles;
            this.dataset = dataset;
        }
    }

    // Common urban centers: [Name, Lat, Lon]
    private static final double[][] LOCATIONS = {
            {40.7128, -74.0060}, // New York
            {34.0522, -118.2437}, // Los Angeles
            {41.8781, -87.6298}, // Chicago
            {51.5074, -0.1278}, // London
            {35.6762, 139.6503}, // Tokyo
            {37.7749, -122.4194}, // San Francisco
            {28.6139, 77.2090}, // New Delhi
            {1.3521, 103.8198} // Singapore
    };

    private static final String[] LEGIT_CATEGORIES = {
            "GROCERY", "DINING", "RETAIL", "UTILITIES", "ELECTRONICS", "ENTERTAINMENT"
    };

    private static final String[] FRAUD_CATEGORIES = {
            "CRYPTO", "GAMBLING", "LUXURY_JEWELRY", "WIRE_TRANSFER", "ELECTRONICS"
    };

    public static GeneratedData generateDataset(int totalCount, double fraudRatio, long seed) {
        Random rand = new Random(seed);
        List<Transaction> txList = new ArrayList<>(totalCount);
        Map<String, CardholderProfile> profileMap = new HashMap<>();

        // Generate 25 distinct cardholder profiles
        for (int i = 1; i <= 25; i++) {
            String cardNum = String.format("4532-%04d-%04d-%04d",
                    rand.nextInt(9000) + 1000, rand.nextInt(9000) + 1000, i * 111);
            double[] loc = LOCATIONS[rand.nextInt(LOCATIONS.length)];
            double avgAmt = 30.0 + rand.nextDouble() * 70.0; // $30 to $100 avg
            double stdDev = 10.0 + rand.nextDouble() * 20.0;
            CardholderProfile prof = new CardholderProfile(cardNum, avgAmt, stdDev, loc[0], loc[1]);
            profileMap.put(cardNum, prof);
        }

        List<String> cardKeys = new ArrayList<>(profileMap.keySet());
        long baseTime = LocalDateTime.of(2026, 1, 15, 8, 0).toInstant(ZoneOffset.UTC).toEpochMilli();

        int targetFraudCount = (int) (totalCount * fraudRatio);
        Set<Integer> fraudIndices = new HashSet<>();
        while (fraudIndices.size() < targetFraudCount) {
            fraudIndices.add(rand.nextInt(totalCount));
        }

        for (int i = 0; i < totalCount; i++) {
            String cardNum = cardKeys.get(rand.nextInt(cardKeys.size()));
            CardholderProfile prof = profileMap.get(cardNum);
            boolean isFraud = fraudIndices.contains(i);

            // Increment time by 1 to 15 minutes between card events
            baseTime += (long) ((1 + rand.nextInt(15)) * 60 * 1000L);

            Transaction tx;
            if (!isFraud) {
                // Legitimate pattern
                double amount = Math.max(5.0, prof.getAverageAmount() + rand.nextGaussian() * prof.getStandardDeviation());
                String cat = LEGIT_CATEGORIES[rand.nextInt(LEGIT_CATEGORIES.length)];
                // Small geographic jitter (+- 0.05 deg ~ 5 km)
                double lat = prof.getHomeLatitude() + (rand.nextDouble() - 0.5) * 0.08;
                double lon = prof.getHomeLongitude() + (rand.nextDouble() - 0.5) * 0.08;
                tx = new Transaction("TX-" + (10000 + i), cardNum, baseTime, amount, "Merchant-" + (rand.nextInt(50) + 1), cat, lat, lon, false);
            } else {
                // Fraud attack pattern (one of 4 synthetic scenarios)
                int scenario = rand.nextInt(4);
                if (scenario == 0) {
                    // Impossible Travel / Card Cloning: Far distance, short elapsed time
                    double[] farLoc = LOCATIONS[(rand.nextInt(LOCATIONS.length - 1) + 1) % LOCATIONS.length];
                    double amount = 300.0 + rand.nextDouble() * 1500.0;
                    tx = new Transaction("TX-FRD-" + (10000 + i), cardNum, baseTime, amount, "Overseas-Outlet", "ELECTRONICS", farLoc[0], farLoc[1], true);
                } else if (scenario == 1) {
                    // High Velocity Card Testing
                    double amount = rand.nextBoolean() ? (0.25 + rand.nextDouble() * 0.75) : (1200.0 + rand.nextDouble() * 2000.0);
                    // Rapid time jump (only 10 seconds after previous)
                    baseTime += 10000;
                    tx = new Transaction("TX-FRD-" + (10000 + i), cardNum, baseTime, amount, "Bot-Gateway", "CRYPTO", prof.getHomeLatitude(), prof.getHomeLongitude(), true);
                } else if (scenario == 2) {
                    // Midnight Spending Spike
                    double amount = prof.getAverageAmount() * (6.0 + rand.nextDouble() * 8.0);
                    // Force midnight hour timestamp
                    long midnightTime = baseTime - (baseTime % 86400000L) + (3 * 3600000L + rand.nextInt(3600000));
                    tx = new Transaction("TX-FRD-" + (10000 + i), cardNum, midnightTime, amount, "NightClub-Lounge", "GAMBLING", prof.getHomeLatitude(), prof.getHomeLongitude(), true);
                } else {
                    // Extreme Amount Spike on high-risk vertical
                    double amount = 4000.0 + rand.nextDouble() * 8000.0;
                    String cat = FRAUD_CATEGORIES[rand.nextInt(FRAUD_CATEGORIES.length)];
                    tx = new Transaction("TX-FRD-" + (10000 + i), cardNum, baseTime, amount, "HighRisk-Exchanger", cat, prof.getHomeLatitude(), prof.getHomeLongitude(), true);
                }
            }

            txList.add(tx);
            prof.recordTransaction(tx);
        }

        // Convert to ML dataset
        Dataset dataset = new Dataset(FeatureExtractor.FEATURE_NAMES);
        for (Transaction tx : txList) {
            CardholderProfile prof = profileMap.get(tx.getCardNumber());
            DataPoint dp = FeatureExtractor.toDataPoint(tx, prof);
            dataset.addPoint(dp);
        }

        return new GeneratedData(txList, profileMap, dataset);
    }

    /**
     * Saves generated transactions to a standard CSV file for persistence and academic review.
     */
    public static void saveToCSV(List<Transaction> transactions, File file) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("TransactionID,CardNumber,Timestamp,DateTime,Amount,Merchant,Category,Latitude,Longitude,IsFraud\n");
            for (Transaction tx : transactions) {
                writer.write(String.format("%s,%s,%d,%s,%.2f,%s,%s,%.6f,%.6f,%d\n",
                        tx.getTransactionId(),
                        tx.getCardNumber(),
                        tx.getTimestamp(),
                        tx.getFormattedDateTime(),
                        tx.getAmount(),
                        tx.getMerchant(),
                        tx.getMerchantCategory(),
                        tx.getLatitude(),
                        tx.getLongitude(),
                        tx.isFraudActual() ? 1 : 0
                ));
            }
        }
    }

    /**
     * Loads transactions from a CSV file.
     */
    public static List<Transaction> loadFromCSV(File file) throws IOException {
        List<Transaction> list = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine(); // header
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(",");
                if (parts.length >= 10) {
                    String id = parts[0];
                    String card = parts[1];
                    long ts = Long.parseLong(parts[2]);
                    double amt = Double.parseDouble(parts[4]);
                    String merchant = parts[5];
                    String cat = parts[6];
                    double lat = Double.parseDouble(parts[7]);
                    double lon = Double.parseDouble(parts[8]);
                    boolean isFraud = "1".equals(parts[9].trim()) || "true".equalsIgnoreCase(parts[9].trim());
                    list.add(new Transaction(id, card, ts, amt, merchant, cat, lat, lon, isFraud));
                }
            }
        }
        return list;
    }
}
