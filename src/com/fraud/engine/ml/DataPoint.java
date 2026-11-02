package com.fraud.engine.ml;

import java.util.Arrays;

/**
 * Represents a single labeled or unlabeled feature vector for the ML model.
 */
public class DataPoint {
    private final double[] features;
    private final int label; // 0 = Legitimate, 1 = Fraud

    public DataPoint(double[] features, int label) {
        this.features = Arrays.copyOf(features, features.length);
        this.label = label;
    }

    public DataPoint(double[] features) {
        this(features, 0);
    }

    public double[] getFeatures() {
        return features;
    }

    public double getFeature(int index) {
        return features[index];
    }

    public int getLabel() {
        return label;
    }

    public int numFeatures() {
        return features.length;
    }
}
