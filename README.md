# Credit Card Fraud Detection System (Java & Cloud Web Portal)

[![Deploy with Vercel](https://vercel.com/button)](https://vercel.com/new/clone?repository-url=https://github.com/mudavathnithin7731/credit-card-fraud-detection)

An academic-grade, end-to-end **Credit Card Fraud Detection System** developed in **Java (OpenJDK 21)** with a live **Cloud Web Portal & REST API deployed on Vercel**. The system implements a **Hybrid Fusion Architecture** combining a **Supervised Machine Learning Random Forest Classifier** with a **Multi-Factor Rule-Based Heuristic Engine**.

---

## 📌 1. Project Overview & Objectives

Financial fraud in electronic payment processing costs the global banking sector tens of billions of dollars annually. Pure machine learning systems often suffer from latency and "black-box" opacity, while traditional rule engines struggle with evolving fraud tactics.

This project delivers a **Hybrid Decision Support System** that addresses both challenges:
1. **Rule-Based Heuristic Engine**: Real-time evaluation of acute anomalies such as impossible geographic velocity, high-frequency transaction bursts, and dormant-hour card probing.
2. **Machine Learning Classifier (Random Forest)**: Evaluates complex non-linear combinations of normalized behavioral features and computes an empirical probability score $P(\text{Fraud})$.
3. **Hybrid Risk Fusion**: Blends rule severity penalties with statistical ML probabilities to generate an actionable Risk Score (0 to 100), automated decision classification (`APPROVED`, `FLAGGED`, `REJECTED`), and human-readable reason codes.

---

## 🏛️ 2. System Architecture

```
                  +-----------------------------------+
                  |   Incoming Credit Card Tx (JSON/CSV) |
                  +-----------------+-----------------+
                                    |
                                    v
                  +-----------------------------------+
                  | Feature Extraction & Normalization |
                  +--------+-----------------+--------+
                           |                 |
            +--------------v---+         +---v---------------+
            | Rule-Based Engine |         | Machine Learning  |
            | (5 Multi-Factors) |         | (Random Forest)   |
            +--------------+---+         +---+---------------+
                           |                 |
                           +--------+--------+
                                    |
                                    v
                     +-----------------------------+
                     | Hybrid Risk Fusion Engine   |
                     | Score = 0.50*Rule + 0.50*ML |
                     +--------------+--------------+
                                    |
            +-----------------------+-----------------------+
            |                       |                       |
            v                       v                       v
    [Score < 35.0]           [35 <= Score < 70]       [Score >= 70.0]
     🟢 APPROVED              🟡 FLAGGED (2FA)         🔴 REJECTED
```

---

## 🔬 3. Core Algorithms & Mathematical Formulations

### A. Geolocation Impossible Velocity (Haversine Formula)
Computes great-circle distance $d$ between successive transactions:
$$a = \sin^2\left(\frac{\Delta\phi}{2}\right) + \cos(\phi_1)\cos(\phi_2)\sin^2\left(\frac{\Delta\lambda}{2}\right)$$
$$d = 2 \cdot R \cdot \text{atan2}\left(\sqrt{a}, \sqrt{1-a}\right)$$
$$\text{Velocity} = \frac{d}{\Delta t} \quad (\text{km/h})$$
*If transit velocity exceeds 800 km/h (cruising speed of commercial aircraft), a high severity penalty (95.0 pts) is triggered for suspected card cloning.*

### B. Statistical Spending Anomaly (Z-Score)
Quantifies deviation from the cardholder's baseline average spend:
$$Z = \frac{x - \mu}{\sigma}$$
*Transactions where $Z \ge 3.5\sigma$ or $x \ge 5\mu$ trigger outlier alerts.*

### C. Random Forest Classifier & Gini Impurity
A collection of $B$ bagged decision trees where each split evaluates the Gini Impurity reduction:
$$I_G(p) = 1 - \sum_{i=1}^{C} p_i^2$$
Ensemble probability:
$$P(\text{Fraud} \mid \mathbf{x}) = \frac{1}{B} \sum_{b=1}^{B} P_b(\text{Fraud} \mid \mathbf{x})$$

---

## 📁 4. Project Directory Structure

```
credit-card-fraud-detection/
├── src/
│   └── com/fraud/
│       ├── Main.java                      # Universal launcher (CLI or GUI)
│       ├── SystemVerificationTest.java    # Automated verification test suite
│       ├── model/
│       │   ├── Transaction.java           # Transaction entity
│       │   ├── CardholderProfile.java     # Historical cardholder behavioral baseline
│       │   ├── DetectionResult.java       # Fraud assessment report & reasons
│       │   └── RiskLevel.java             # LOW, MEDIUM, HIGH enum
│       ├── engine/
│       │   ├── FraudDetectionEngine.java  # Hybrid fusion orchestrator
│       │   ├── rules/
│       │   │   ├── FraudRule.java         # Strategy interface
│       │   │   ├── VelocityRule.java      # Bot & rapid burst detection
│       │   │   ├── GeolocationRule.java   # Impossible travel velocity
│       │   │   ├── AmountAnomalyRule.java # Z-score outlier detection
│       │   │   ├── MerchantRiskRule.java  # High-risk verticals (crypto, gambling)
│       │   │   └── UnusualTimeRule.java   # Dormant night hours (01:00-05:00)
│       │   └── ml/
│       │       ├── DataPoint.java         # ML feature vector
│       │       ├── Dataset.java           # Train/test split & container
│       │       ├── FeatureExtractor.java  # Domain-to-vector transformer
│       │       ├── DecisionTree.java      # Gini-based binary decision tree
│       │       ├── RandomForest.java      # Ensemble bagging classifier
│       │       └── ModelEvaluator.java    # Confusion matrix & benchmark metrics
│       ├── data/
│       │   └── SyntheticDataGenerator.java# Benchmark dataset generator & CSV I/O
│       └── ui/
│           ├── ConsoleApp.java            # Interactive terminal CLI
│           └── FraudDetectionGUI.java     # Desktop Java Swing GUI
├── compile.bat                            # One-click build script
├── run-cli.bat                            # Launch interactive CLI
├── run-gui.bat                            # Launch desktop Swing GUI
├── run-test.bat                           # Execute automated test suite
└── README.md                              # Academic documentation & viva guide
```

---

## 🚀 5. How to Compile & Run

### Prerequisites
- Java JDK 17 or 21 (bundled automatically in the scratch directory or available in system PATH).

### Option A: Run in Web Browser (Interactive Web Dashboard)
Double-click `run-web.bat` or run:
```cmd
run-web.bat
```
*Automatically starts the built-in HTTP server on `http://localhost:8080` and opens your default web browser.*

### Option B: Run Desktop GUI (Swing)
```cmd
run-gui.bat
```
*Opens the desktop interface featuring Real-Time Fraud Assessment, Live Streaming Simulator, and Confusion Matrix visualizer.*

### Option C: Launch Interactive Console Terminal (CLI)
```cmd
run-cli.bat
```
*Provides an ASCII menu for terminal-based evaluation, benchmark generation, and CSV export.*

### Option D: Run Automated Test Suite
```cmd
run-test.bat
```
*Executes all 5 end-to-end integration tests (ML convergence, normal transaction, impossible travel cloning, bot velocity, CSV persistence).*

---

## 🎓 6. Academic Viva & Defense Questions

**Q1: Why use a Hybrid architecture instead of pure Machine Learning?**
> *Pure ML models require historical training data and can fail on zero-day attacks or edge cases (e.g. an account with no previous travel history). A hybrid system ensures that hard physical constraints (like impossible travel speed) and regulatory policies immediately trigger safety actions without waiting for model retraining.*

**Q2: How do you handle class imbalance in credit card fraud detection?**
> *In real-world transaction streams, fraud accounts for < 0.1% to 1% of transactions. In this system, we use stratified train-test splits, feature sub-sampling across bagged decision trees in Random Forest, and report Balanced F1-Score, Recall, and Precision rather than raw accuracy alone.*

**Q3: What happens when an impossible travel anomaly occurs?**
> *The Geolocation Rule calculates the distance between the previous transaction coordinates and the incoming transaction using the Haversine formula. Dividing distance by elapsed time yields velocity. If velocity exceeds 800 km/h, the transaction is immediately rejected with a high severity score (95.0), overriding low ML baseline probabilities.*
