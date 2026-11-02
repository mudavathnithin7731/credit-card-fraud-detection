package com.fraud.engine.rules;

import com.fraud.model.CardholderProfile;
import com.fraud.model.Transaction;

/**
 * Calculates physical distance between successive transactions using the Haversine formula
 * to identify physically impossible transit speeds (evidence of card cloning or account takeover).
 */
public class GeolocationRule implements FraudRule {
    private static final double EARTH_RADIUS_KM = 6371.0;
    private final double maxRealisticSpeedKmH;

    public GeolocationRule(double maxRealisticSpeedKmH) {
        this.maxRealisticSpeedKmH = maxRealisticSpeedKmH;
    }

    public GeolocationRule() {
        this(800.0); // Standard commercial flight top cruise speed ~800-900 km/h
    }

    @Override
    public String getRuleName() {
        return "Impossible Geolocation Velocity Rule";
    }

    @Override
    public double evaluate(Transaction transaction, CardholderProfile profile) {
        if (profile == null || profile.getLastTransactionTimestamp() == 0) {
            return 0.0;
        }

        long timeDiffMillis = transaction.getTimestamp() - profile.getLastTransactionTimestamp();
        if (timeDiffMillis <= 0) {
            // Concurrent or instantaneous transaction from different place
            double dist = calculateDistanceKm(profile.getLastLatitude(), profile.getLastLongitude(),
                    transaction.getLatitude(), transaction.getLongitude());
            if (dist > 15.0) {
                return 95.0; // Impossible: simultaneous transactions at separated locations
            }
            return 0.0;
        }

        double hours = timeDiffMillis / (1000.0 * 3600.0);
        double distanceKm = calculateDistanceKm(profile.getLastLatitude(), profile.getLastLongitude(),
                transaction.getLatitude(), transaction.getLongitude());

        if (distanceKm < 25.0) {
            return 0.0; // Within acceptable local vicinity
        }

        double speedKmH = distanceKm / hours;

        if (speedKmH > 1000.0) {
            return 95.0; // Supersonic / Impossible travel
        } else if (speedKmH > maxRealisticSpeedKmH) {
            return 75.0; // Exceeds commercial flight threshold
        } else if (speedKmH > 400.0 && hours < 1.0) {
            return 45.0; // Rapid transit without sufficient buffer
        }

        return 0.0;
    }

    @Override
    public String getViolationReason(Transaction transaction, CardholderProfile profile) {
        if (profile == null || profile.getLastTransactionTimestamp() == 0) {
            return null;
        }

        long timeDiffMillis = transaction.getTimestamp() - profile.getLastTransactionTimestamp();
        double hours = Math.max(0.0001, timeDiffMillis / (1000.0 * 3600.0));
        double distanceKm = calculateDistanceKm(profile.getLastLatitude(), profile.getLastLongitude(),
                transaction.getLatitude(), transaction.getLongitude());

        if (distanceKm < 25.0) {
            return null;
        }

        double speedKmH = distanceKm / hours;
        if (speedKmH > maxRealisticSpeedKmH) {
            return String.format("Impossible Travel Velocity: %.1f km traversed in %.1f minutes (speed: %.0f km/h).",
                    distanceKm, hours * 60.0, speedKmH);
        }
        return null;
    }

    /**
     * Haversine formula for great-circle distance between two coordinates in kilometers.
     */
    public static double calculateDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}
