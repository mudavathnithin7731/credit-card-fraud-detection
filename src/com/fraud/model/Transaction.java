package com.fraud.model;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Encapsulates the details of a credit card transaction.
 */
public class Transaction {
    private final String transactionId;
    private final String cardNumber;
    private final long timestamp; // Epoch milliseconds
    private final double amount;
    private final String merchant;
    private final String merchantCategory;
    private final double latitude;
    private final double longitude;
    private final boolean isFraudActual; // Ground truth (for training/evaluation)

    public Transaction(String transactionId, String cardNumber, long timestamp, double amount,
                       String merchant, String merchantCategory, double latitude, double longitude,
                       boolean isFraudActual) {
        this.transactionId = transactionId;
        this.cardNumber = cardNumber;
        this.timestamp = timestamp;
        this.amount = amount;
        this.merchant = merchant;
        this.merchantCategory = merchantCategory;
        this.latitude = latitude;
        this.longitude = longitude;
        this.isFraudActual = isFraudActual;
    }

    public Transaction(String transactionId, String cardNumber, long timestamp, double amount,
                       String merchant, String merchantCategory, double latitude, double longitude) {
        this(transactionId, cardNumber, timestamp, amount, merchant, merchantCategory, latitude, longitude, false);
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public String getMaskedCardNumber() {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        String clean = cardNumber.replaceAll("[^0-9]", "");
        if (clean.length() >= 4) {
            return "****-****-****-" + clean.substring(clean.length() - 4);
        }
        return "****-****-****-" + cardNumber;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public LocalDateTime getDateTime() {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
    }

    public String getFormattedDateTime() {
        return getDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public double getAmount() {
        return amount;
    }

    public String getMerchant() {
        return merchant;
    }

    public String getMerchantCategory() {
        return merchantCategory;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public boolean isFraudActual() {
        return isFraudActual;
    }

    @Override
    public String toString() {
        return String.format("Tx[%s | %s | $%.2f | %s (%s) | %s]",
                transactionId, getMaskedCardNumber(), amount, merchant, merchantCategory, getFormattedDateTime());
    }
}
