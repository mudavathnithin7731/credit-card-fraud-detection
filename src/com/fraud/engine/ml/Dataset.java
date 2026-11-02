package com.fraud.engine.ml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Manages a collection of DataPoint instances for training and evaluating ML models.
 */
public class Dataset {
    private final List<DataPoint> points;
    private final String[] featureNames;

    public Dataset(String[] featureNames) {
        this.featureNames = featureNames != null ? featureNames : new String[0];
        this.points = new ArrayList<>();
    }

    public Dataset(List<DataPoint> points, String[] featureNames) {
        this.points = new ArrayList<>(points);
        this.featureNames = featureNames != null ? featureNames : new String[0];
    }

    public void addPoint(DataPoint point) {
        points.add(point);
    }

    public List<DataPoint> getPoints() {
        return Collections.unmodifiableList(points);
    }

    public int size() {
        return points.size();
    }

    public String[] getFeatureNames() {
        return featureNames;
    }

    public int countFraud() {
        int count = 0;
        for (DataPoint p : points) {
            if (p.getLabel() == 1) count++;
        }
        return count;
    }

    public int countLegitimate() {
        return size() - countFraud();
    }

    /**
     * Splits the dataset into train and test sets randomly given a split ratio (e.g., 0.8).
     */
    public Dataset[] trainTestSplit(double trainRatio, long seed) {
        List<DataPoint> shuffled = new ArrayList<>(points);
        Collections.shuffle(shuffled, new Random(seed));

        int trainSize = (int) (shuffled.size() * trainRatio);
        List<DataPoint> trainPoints = new ArrayList<>(shuffled.subList(0, trainSize));
        List<DataPoint> testPoints = new ArrayList<>(shuffled.subList(trainSize, shuffled.size()));

        return new Dataset[]{
                new Dataset(trainPoints, featureNames),
                new Dataset(testPoints, featureNames)
        };
    }
}
