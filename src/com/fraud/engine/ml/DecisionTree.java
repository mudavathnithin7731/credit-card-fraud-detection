package com.fraud.engine.ml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Supervised Decision Tree binary classifier for fraud detection.
 * Utilizes Gini Impurity reduction for optimal split selection.
 */
public class DecisionTree {

    public static class Node {
        public boolean isLeaf;
        public int predictedClass;
        public double fraudProbability;
        public int splitFeatureIndex;
        public double splitThreshold;
        public Node left;
        public Node right;

        public Node(boolean isLeaf, int classOrFeature, double probOrThreshold) {
            this.isLeaf = isLeaf;
            if (isLeaf) {
                this.predictedClass = classOrFeature;
                this.fraudProbability = probOrThreshold;
            } else {
                this.splitFeatureIndex = classOrFeature;
                this.splitThreshold = probOrThreshold;
            }
        }

        public static Node leaf(int predictedClass, double fraudProbability) {
            return new Node(true, predictedClass, fraudProbability);
        }

        public static Node split(int splitFeatureIndex, double splitThreshold) {
            return new Node(false, splitFeatureIndex, splitThreshold);
        }
    }

    private final int maxDepth;
    private final int minSamplesSplit;
    private final int numFeaturesPerSplit; // <= 0 means all features
    private Node root;
    private final Random random;

    public DecisionTree(int maxDepth, int minSamplesSplit, int numFeaturesPerSplit, long seed) {
        this.maxDepth = maxDepth;
        this.minSamplesSplit = minSamplesSplit;
        this.numFeaturesPerSplit = numFeaturesPerSplit;
        this.random = new Random(seed);
    }

    public DecisionTree(int maxDepth, int minSamplesSplit) {
        this(maxDepth, minSamplesSplit, -1, 42L);
    }

    public DecisionTree() {
        this(6, 10, -1, 42L);
    }

    public void train(List<DataPoint> data) {
        if (data == null || data.isEmpty()) {
            root = Node.leaf(0, 0.0);
            return;
        }
        root = buildTree(data, 0);
    }

    private Node buildTree(List<DataPoint> data, int currentDepth) {
        int total = data.size();
        int fraudCount = 0;
        for (DataPoint p : data) {
            if (p.getLabel() == 1) fraudCount++;
        }
        double fraudProb = (double) fraudCount / total;
        int majorityClass = fraudProb >= 0.5 ? 1 : 0;

        // Base cases: max depth, pure node, or below min samples
        if (currentDepth >= maxDepth || total < minSamplesSplit || fraudCount == 0 || fraudCount == total) {
            return Node.leaf(majorityClass, fraudProb);
        }

        int numTotalFeatures = data.get(0).numFeatures();
        List<Integer> candidateFeatures = new ArrayList<>();
        for (int i = 0; i < numTotalFeatures; i++) candidateFeatures.add(i);

        if (numFeaturesPerSplit > 0 && numFeaturesPerSplit < numTotalFeatures) {
            Collections.shuffle(candidateFeatures, random);
            candidateFeatures = candidateFeatures.subList(0, numFeaturesPerSplit);
        }

        double bestGini = calculateGini(data);
        int bestFeature = -1;
        double bestThreshold = 0.0;
        List<DataPoint> bestLeft = null;
        List<DataPoint> bestRight = null;

        for (int featIndex : candidateFeatures) {
            // Sample quantile thresholds to speed up training while preserving quality
            List<Double> values = new ArrayList<>();
            for (DataPoint p : data) {
                values.add(p.getFeature(featIndex));
            }
            Collections.sort(values);

            int step = Math.max(1, values.size() / 15);
            for (int i = step; i < values.size(); i += step) {
                double threshold = values.get(i);
                List<DataPoint> left = new ArrayList<>();
                List<DataPoint> right = new ArrayList<>();

                for (DataPoint p : data) {
                    if (p.getFeature(featIndex) <= threshold) {
                        left.add(p);
                    } else {
                        right.add(p);
                    }
                }

                if (left.isEmpty() || right.isEmpty()) continue;

                double currentGini = (double) left.size() / total * calculateGini(left)
                        + (double) right.size() / total * calculateGini(right);

                if (currentGini < bestGini) {
                    bestGini = currentGini;
                    bestFeature = featIndex;
                    bestThreshold = threshold;
                    bestLeft = left;
                    bestRight = right;
                }
            }
        }

        if (bestFeature == -1 || bestLeft == null || bestRight == null) {
            return Node.leaf(majorityClass, fraudProb);
        }

        Node node = Node.split(bestFeature, bestThreshold);
        node.left = buildTree(bestLeft, currentDepth + 1);
        node.right = buildTree(bestRight, currentDepth + 1);
        return node;
    }

    private double calculateGini(List<DataPoint> data) {
        if (data.isEmpty()) return 0.0;
        int count1 = 0;
        for (DataPoint p : data) {
            if (p.getLabel() == 1) count1++;
        }
        double p1 = (double) count1 / data.size();
        double p0 = 1.0 - p1;
        return 1.0 - (p0 * p0 + p1 * p1);
    }

    public double predictProbability(DataPoint point) {
        if (root == null) return 0.0;
        Node curr = root;
        while (!curr.isLeaf) {
            if (point.getFeature(curr.splitFeatureIndex) <= curr.splitThreshold) {
                curr = curr.left;
            } else {
                curr = curr.right;
            }
            if (curr == null) return 0.0;
        }
        return curr.fraudProbability;
    }

    public int predict(DataPoint point) {
        return predictProbability(point) >= 0.5 ? 1 : 0;
    }

    public Node getRoot() {
        return root;
    }
}
