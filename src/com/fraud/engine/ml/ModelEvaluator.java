package com.fraud.engine.ml;

import java.util.List;

/**
 * Computes academic performance evaluation metrics including Confusion Matrix,
 * Precision, Recall, Specificity, and F1-Score.
 */
public class ModelEvaluator {

    public static class Metrics {
        public final int truePositives;
        public final int falsePositives;
        public final int trueNegatives;
        public final int falseNegatives;
        public final int totalSamples;

        public final double accuracy;
        public final double precision;
        public final double recall;
        public final double specificity;
        public final double f1Score;

        public Metrics(int tp, int fp, int tn, int fn) {
            this.truePositives = tp;
            this.falsePositives = fp;
            this.trueNegatives = tn;
            this.falseNegatives = fn;
            this.totalSamples = tp + fp + tn + fn;

            this.accuracy = totalSamples > 0 ? (double) (tp + tn) / totalSamples : 0.0;
            this.precision = (tp + fp) > 0 ? (double) tp / (tp + fp) : 0.0;
            this.recall = (tp + fn) > 0 ? (double) tp / (tp + fn) : 0.0;
            this.specificity = (tn + fp) > 0 ? (double) tn / (tn + fp) : 0.0;

            if (precision + recall > 0) {
                this.f1Score = 2.0 * (precision * recall) / (precision + recall);
            } else {
                this.f1Score = 0.0;
            }
        }

        public String getFormattedSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("\n=======================================================\n");
            sb.append("         MODEL EVALUATION & BENCHMARK METRICS          \n");
            sb.append("=======================================================\n");
            sb.append(String.format(" Total Test Samples Evaluated : %,d\n", totalSamples));
            sb.append("-------------------------------------------------------\n");
            sb.append(" CONFUSION MATRIX:\n");
            sb.append(String.format("                     Actual Fraud   Actual Legitimate\n"));
            sb.append(String.format("  Predicted Fraud    TP: %-10d FP: %-10d\n", truePositives, falsePositives));
            sb.append(String.format("  Predicted Legit    FN: %-10d TN: %-10d\n", falseNegatives, trueNegatives));
            sb.append("-------------------------------------------------------\n");
            sb.append(String.format(" Accuracy            : %6.2f%%\n", accuracy * 100));
            sb.append(String.format(" Precision (PPV)     : %6.2f%%  (Low False Positive Rate)\n", precision * 100));
            sb.append(String.format(" Recall (Sensitivity): %6.2f%%  (High Fraud Catch Rate)\n", recall * 100));
            sb.append(String.format(" Specificity (TNR)   : %6.2f%%\n", specificity * 100));
            sb.append(String.format(" F1-Score (Balanced) : %6.2f%%\n", f1Score * 100));
            sb.append("=======================================================\n");
            return sb.toString();
        }
    }

    public static Metrics evaluate(RandomForest model, Dataset testDataset) {
        List<DataPoint> points = testDataset.getPoints();
        int tp = 0, fp = 0, tn = 0, fn = 0;

        for (DataPoint p : points) {
            int predicted = model.predict(p);
            int actual = p.getLabel();

            if (actual == 1 && predicted == 1) tp++;
            else if (actual == 0 && predicted == 1) fp++;
            else if (actual == 0 && predicted == 0) tn++;
            else if (actual == 1 && predicted == 0) fn++;
        }

        return new Metrics(tp, fp, tn, fn);
    }
}
