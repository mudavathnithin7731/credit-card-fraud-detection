package com.fraud.engine.ml;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Random Forest ensemble classifier consisting of multiple bagging Decision Trees.
 * Reduces variance, prevents overfitting, and yields reliable fraud probabilities.
 */
public class RandomForest {
    private final int numTrees;
    private final int maxDepth;
    private final int minSamplesSplit;
    private final List<DecisionTree> trees;
    private final Random random;

    public RandomForest(int numTrees, int maxDepth, int minSamplesSplit, long seed) {
        this.numTrees = numTrees;
        this.maxDepth = maxDepth;
        this.minSamplesSplit = minSamplesSplit;
        this.trees = new ArrayList<>();
        this.random = new Random(seed);
    }

    public RandomForest(int numTrees) {
        this(numTrees, 7, 8, 42L);
    }

    public RandomForest() {
        this(15, 7, 8, 42L);
    }

    public void train(Dataset dataset) {
        List<DataPoint> data = dataset.getPoints();
        if (data == null || data.isEmpty()) return;

        trees.clear();
        int numFeatures = data.get(0).numFeatures();
        // Typically sqrt(features) for classification
        int numFeaturesPerSplit = Math.max(2, (int) Math.round(Math.sqrt(numFeatures)));

        for (int i = 0; i < numTrees; i++) {
            // Bootstrap sample (sampling with replacement)
            List<DataPoint> bootstrap = new ArrayList<>(data.size());
            for (int b = 0; b < data.size(); b++) {
                int randomIndex = random.nextInt(data.size());
                bootstrap.add(data.get(randomIndex));
            }

            DecisionTree tree = new DecisionTree(maxDepth, minSamplesSplit, numFeaturesPerSplit, random.nextLong());
            tree.train(bootstrap);
            trees.add(tree);
        }
    }

    /**
     * Calculates the ensemble probability of fraud [0.0, 1.0].
     */
    public double predictProbability(DataPoint point) {
        if (trees.isEmpty()) return 0.0;

        double sum = 0.0;
        for (DecisionTree tree : trees) {
            sum += tree.predictProbability(point);
        }
        return sum / trees.size();
    }

    public int predict(DataPoint point) {
        return predictProbability(point) >= 0.5 ? 1 : 0;
    }

    public int getNumTrees() {
        return trees.size();
    }
}
