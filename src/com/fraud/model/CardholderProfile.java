package com.fraud.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tracks historical spending habits and physical movement of a cardholder.
 */
public class CardholderProfile {
    private final String cardNumber;
    private double averageAmount;
    private double standardDeviation;
    private double homeLatitude;
    private double homeLongitude;
    
    private long lastTransactionTimestamp;
    private double lastLatitude;
    private double lastLongitude;
    
    // Recent transaction timestamps for velocity checking
    private final List<Long> recentTimestamps = new ArrayList<>();

    public CardholderProfile(String cardNumber, double averageAmount, double standardDeviation,
                             double homeLatitude, double homeLongitude) {
        this.cardNumber = cardNumber;
        this.averageAmount = averageAmount;
        this.standardDeviation = Math.max(standardDeviation, 15.0); // minimum baseline deviation
        this.homeLatitude = homeLatitude;
        this.homeLongitude = homeLongitude;
        this.lastTransactionTimestamp = 0;
        this.lastLatitude = homeLatitude;
        this.lastLongitude = homeLongitude;
    }

    public synchronized void recordTransaction(Transaction tx) {
        this.lastTransactionTimestamp = tx.getTimestamp();
        this.lastLatitude = tx.getLatitude();
        this.lastLongitude = tx.getLongitude();
        
        recentTimestamps.add(tx.getTimestamp());
        // Clean timestamps older than 1 hour (3600000 ms)
        long oneHourAgo = tx.getTimestamp() - 3600000;
        recentTimestamps.removeIf(t -> t < oneHourAgo);
    }

    public synchronized int getTransactionCountInLastMinutes(long currentTimestamp, int minutes) {
        long windowStart = currentTimestamp - (minutes * 60L * 1000L);
        int count = 0;
        for (Long t : recentTimestamps) {
            if (t >= windowStart && t <= currentTimestamp) {
                count++;
            }
        }
        return count;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public double getAverageAmount() {
        return averageAmount;
    }

    public void setAverageAmount(double averageAmount) {
        this.averageAmount = averageAmount;
    }

    public double getStandardDeviation() {
        return standardDeviation;
    }

    public void setStandardDeviation(double standardDeviation) {
        this.standardDeviation = Math.max(standardDeviation, 15.0);
    }

    public double getHomeLatitude() {
        return homeLatitude;
    }

    public double getHomeLongitude() {
        return homeLongitude;
    }

    public long getLastTransactionTimestamp() {
        return lastTransactionTimestamp;
    }

    public double getLastLatitude() {
        return lastLatitude;
    }

    public double getLastLongitude() {
        return lastLongitude;
    }

    public List<Long> getRecentTimestamps() {
        return Collections.unmodifiableList(recentTimestamps);
    }
}
