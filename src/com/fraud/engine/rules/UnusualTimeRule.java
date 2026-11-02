package com.fraud.engine.rules;

import com.fraud.model.CardholderProfile;
import com.fraud.model.Transaction;

import java.time.LocalDateTime;

/**
 * Flags transactions occurring during dormant night hours (e.g., 01:00 to 05:00)
 * where unauthorized activity is statistically more prevalent.
 */
public class UnusualTimeRule implements FraudRule {
    private final int nightStartHour;
    private final int nightEndHour;

    public UnusualTimeRule(int nightStartHour, int nightEndHour) {
        this.nightStartHour = nightStartHour;
        this.nightEndHour = nightEndHour;
    }

    public UnusualTimeRule() {
        this(1, 5); // 01:00 to 05:00 AM
    }

    @Override
    public String getRuleName() {
        return "Unusual Transaction Time Rule";
    }

    @Override
    public double evaluate(Transaction transaction, CardholderProfile profile) {
        LocalDateTime dt = transaction.getDateTime();
        int hour = dt.getHour();

        if (hour >= nightStartHour && hour <= nightEndHour) {
            // If combined with moderate or large spend
            if (transaction.getAmount() > 500.0) {
                return 40.0;
            } else if (transaction.getAmount() > 150.0) {
                return 25.0;
            } else {
                return 15.0;
            }
        }

        return 0.0;
    }

    @Override
    public String getViolationReason(Transaction transaction, CardholderProfile profile) {
        LocalDateTime dt = transaction.getDateTime();
        int hour = dt.getHour();

        if (hour >= nightStartHour && hour <= nightEndHour && transaction.getAmount() > 150.0) {
            return String.format("Dormant hour activity (%02d:%02d) with elevated spend $%.2f.",
                    hour, dt.getMinute(), transaction.getAmount());
        }
        return null;
    }
}
