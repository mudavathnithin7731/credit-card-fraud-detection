package com.fraud.ui;

import com.fraud.data.SyntheticDataGenerator;
import com.fraud.engine.FraudDetectionEngine;
import com.fraud.engine.ml.Dataset;
import com.fraud.engine.ml.ModelEvaluator;
import com.fraud.engine.ml.RandomForest;
import com.fraud.engine.rules.FraudRule;
import com.fraud.model.CardholderProfile;
import com.fraud.model.DetectionResult;
import com.fraud.model.RiskLevel;
import com.fraud.model.Transaction;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Map;

/**
 * Modern Java Swing Desktop GUI for Credit Card Fraud Detection.
 * Provides interactive real-time assessment, live stream simulation,
 * confusion matrix visualization, and profile inspection.
 */
public class FraudDetectionGUI extends JFrame {
    private final FraudDetectionEngine engine;
    private final RandomForest model;
    private SyntheticDataGenerator.GeneratedData benchmarkData;
    private ModelEvaluator.Metrics latestMetrics;

    // GUI Components - Tab 1
    private JComboBox<String> cardSelector;
    private JTextField txtAmount;
    private JTextField txtMerchant;
    private JComboBox<String> cmbCategory;
    private JTextField txtLatitude;
    private JTextField txtLongitude;
    private JLabel lblDecision;
    private JProgressBar progressRisk;
    private JLabel lblRiskDetails;
    private JTextArea txtReasonCodes;

    // GUI Components - Tab 2
    private DefaultTableModel streamTableModel;
    private JLabel lblStatTotal;
    private JLabel lblStatApproved;
    private JLabel lblStatFlagged;
    private JLabel lblStatBlocked;

    // GUI Components - Tab 3
    private JLabel lblAcc, lblPrec, lblRec, lblF1;
    private JLabel lblTP, lblFP, lblTN, lblFN;

    public FraudDetectionGUI() {
        super("Credit Card Fraud Detection System - Hybrid ML & Heuristic Platform");

        // Initialize core engines
        this.model = new RandomForest(20, 7, 8, 42L);
        this.engine = new FraudDetectionEngine(model);

        initBackend();
        initComponents();
    }

    private void initBackend() {
        benchmarkData = SyntheticDataGenerator.generateDataset(3000, 0.08, 42L);
        for (CardholderProfile p : benchmarkData.profiles.values()) {
            engine.registerProfile(p);
        }
        Dataset[] split = benchmarkData.dataset.trainTestSplit(0.80, 42L);
        model.train(split[0]);
        latestMetrics = ModelEvaluator.evaluate(model, split[1]);
    }

    private void initComponents() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 750);
        setLocationRelativeTo(null);

        // Main Layout
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(createHeaderPanel(), BorderLayout.NORTH);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabbedPane.addTab("  Real-Time Fraud Assessment  ", createAssessmentTab());
        tabbedPane.addTab("  Transaction Stream Simulator  ", createSimulatorTab());
        tabbedPane.addTab("  Model Benchmark & Confusion Matrix  ", createMetricsTab());
        tabbedPane.addTab("  Cardholder Profiles & Heuristics  ", createProfilesTab());

        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        add(mainPanel);
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new java.awt.Color(24, 43, 73));
        header.setBorder(new EmptyBorder(14, 20, 14, 20));

        JPanel titleBox = new JPanel(new GridLayout(2, 1));
        titleBox.setOpaque(false);

        JLabel title = new JLabel("CREDIT CARD FRAUD DETECTION SYSTEM");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("Hybrid Risk Architecture: Random Forest Ensemble (20 Trees) + Multi-Factor Behavioral Rules");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(new java.awt.Color(200, 215, 235));

        titleBox.add(title);
        titleBox.add(subtitle);

        header.add(titleBox, BorderLayout.WEST);
        return header;
    }

    private JPanel createAssessmentTab() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Left Form Panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new java.awt.Color(180, 195, 210)),
                "Transaction Inputs & Presets", TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 14)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Card selection
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Target Card:"), gbc);
        gbc.gridx = 1;
        cardSelector = new JComboBox<>();
        for (String card : benchmarkData.profiles.keySet()) {
            cardSelector.addItem(card);
        }
        formPanel.add(cardSelector, gbc);

        // Amount
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Amount ($):"), gbc);
        gbc.gridx = 1;
        txtAmount = new JTextField("45.00", 12);
        formPanel.add(txtAmount, gbc);

        // Merchant
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Merchant:"), gbc);
        gbc.gridx = 1;
        txtMerchant = new JTextField("Target Superstore", 12);
        formPanel.add(txtMerchant, gbc);

        // Category
        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Category:"), gbc);
        gbc.gridx = 1;
        cmbCategory = new JComboBox<>(new String[]{
                "GROCERY", "DINING", "RETAIL", "ELECTRONICS", "CRYPTO", "GAMBLING", "LUXURY_JEWELRY", "WIRE_TRANSFER"
        });
        formPanel.add(cmbCategory, gbc);

        // Latitude
        gbc.gridx = 0; gbc.gridy = 4;
        formPanel.add(new JLabel("Merchant Latitude:"), gbc);
        gbc.gridx = 1;
        txtLatitude = new JTextField("40.7128", 12);
        formPanel.add(txtLatitude, gbc);

        // Longitude
        gbc.gridx = 0; gbc.gridy = 5;
        formPanel.add(new JLabel("Merchant Longitude:"), gbc);
        gbc.gridx = 1;
        txtLongitude = new JTextField("-74.0060", 12);
        formPanel.add(txtLongitude, gbc);

        // Preset Test Buttons
        JPanel presetPanel = new JPanel(new GridLayout(2, 2, 6, 6));
        presetPanel.setBorder(new EmptyBorder(8, 0, 8, 0));

        JButton btnNormal = new JButton("Scenario 1: Normal Grocery ($35)");
        btnNormal.addActionListener(e -> applyPreset("Normal", 35.0, "Walmart-Grocery", "GROCERY", 40.7128, -74.0060));

        JButton btnCloning = new JButton("Scenario 2: Impossible Travel (Tokyo)");
        btnCloning.addActionListener(e -> applyPreset("Cloning", 750.0, "Tokyo-DutyFree", "ELECTRONICS", 35.6762, 139.6503));

        JButton btnSpike = new JButton("Scenario 3: Crypto Outlier ($4,500)");
        btnSpike.addActionListener(e -> applyPreset("Spike", 4500.0, "CoinVault-ATM", "CRYPTO", 40.7128, -74.0060));

        JButton btnBot = new JButton("Scenario 4: Bot Probe ($0.50)");
        btnBot.addActionListener(e -> applyPreset("Bot", 0.50, "Online-Probe-Inc", "ELECTRONICS", 40.7128, -74.0060));

        presetPanel.add(btnNormal);
        presetPanel.add(btnCloning);
        presetPanel.add(btnSpike);
        presetPanel.add(btnBot);

        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2;
        formPanel.add(presetPanel, gbc);

        // Evaluate Button
        JButton btnEvaluate = new JButton("  EVALUATE TRANSACTION RISK  ");
        btnEvaluate.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnEvaluate.setBackground(new java.awt.Color(30, 110, 200));
        btnEvaluate.setForeground(Color.WHITE);
        btnEvaluate.setFocusPainted(false);
        btnEvaluate.addActionListener(e -> runAssessment());

        gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 2;
        formPanel.add(btnEvaluate, gbc);

        // Right Result Panel
        JPanel resultPanel = new JPanel(new BorderLayout(10, 10));
        resultPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new java.awt.Color(180, 195, 210)),
                "Evaluation Outcome & Risk Breakdown", TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 14)));

        JPanel decisionBox = new JPanel(new BorderLayout(8, 8));
        decisionBox.setBorder(new EmptyBorder(10, 15, 10, 15));
        decisionBox.setBackground(new java.awt.Color(245, 248, 252));

        lblDecision = new JLabel("STATUS: READY", SwingConstants.CENTER);
        lblDecision.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblDecision.setForeground(new java.awt.Color(80, 90, 100));
        lblDecision.setBorder(new EmptyBorder(10, 0, 10, 0));

        progressRisk = new JProgressBar(0, 100);
        progressRisk.setValue(0);
        progressRisk.setStringPainted(true);
        progressRisk.setFont(new Font("Segoe UI", Font.BOLD, 13));
        progressRisk.setPreferredSize(new Dimension(300, 26));

        lblRiskDetails = new JLabel("Score: --/100 | ML Probability: -- | Rule Score: --", SwingConstants.CENTER);
        lblRiskDetails.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        decisionBox.add(lblDecision, BorderLayout.NORTH);
        decisionBox.add(progressRisk, BorderLayout.CENTER);
        decisionBox.add(lblRiskDetails, BorderLayout.SOUTH);

        txtReasonCodes = new JTextArea();
        txtReasonCodes.setEditable(false);
        txtReasonCodes.setFont(new Font("Consolas", Font.PLAIN, 13));
        txtReasonCodes.setBackground(new java.awt.Color(252, 253, 255));
        txtReasonCodes.setText("Ready for evaluation.\nSelect a scenario or enter transaction details and click Evaluate.");
        JScrollPane scrollReasons = new JScrollPane(txtReasonCodes);
        scrollReasons.setBorder(BorderFactory.createTitledBorder("Triggered Rule Signals & Reason Codes"));

        resultPanel.add(decisionBox, BorderLayout.NORTH);
        resultPanel.add(scrollReasons, BorderLayout.CENTER);

        panel.add(formPanel, BorderLayout.WEST);
        panel.add(resultPanel, BorderLayout.CENTER);

        return panel;
    }

    private void applyPreset(String name, double amt, String merchant, String cat, double lat, double lon) {
        txtAmount.setText(String.format("%.2f", amt));
        txtMerchant.setText(merchant);
        cmbCategory.setSelectedItem(cat);
        txtLatitude.setText(String.format("%.4f", lat));
        txtLongitude.setText(String.format("%.4f", lon));
        runAssessment();
    }

    private void runAssessment() {
        try {
            String card = (String) cardSelector.getSelectedItem();
            double amount = Double.parseDouble(txtAmount.getText().trim());
            String merchant = txtMerchant.getText().trim();
            String category = (String) cmbCategory.getSelectedItem();
            double lat = Double.parseDouble(txtLatitude.getText().trim());
            double lon = Double.parseDouble(txtLongitude.getText().trim());

            Transaction tx = new Transaction("TX-GUI-" + (System.currentTimeMillis() % 100000),
                    card, System.currentTimeMillis(), amount, merchant, category, lat, lon);

            DetectionResult result = engine.evaluate(tx);

            progressRisk.setValue((int) Math.round(result.getRiskScore()));
            lblRiskDetails.setText(String.format("Risk Score: %.1f / 100 | ML Probability: %.1f%% | Rule Penalty: %.1f",
                    result.getRiskScore(), result.getMlProbability() * 100, result.getRuleScore()));

            if (result.getRiskLevel() == RiskLevel.LOW) {
                lblDecision.setText("APPROVED (LOW RISK)");
                lblDecision.setForeground(new java.awt.Color(34, 139, 34));
                progressRisk.setForeground(new java.awt.Color(40, 167, 69));
            } else if (result.getRiskLevel() == RiskLevel.MEDIUM) {
                lblDecision.setText("FLAGGED (OTP / 2FA CHALLENGE)");
                lblDecision.setForeground(new java.awt.Color(200, 130, 0));
                progressRisk.setForeground(new java.awt.Color(255, 193, 7));
            } else {
                lblDecision.setText("REJECTED / SUSPECTED FRAUD");
                lblDecision.setForeground(new java.awt.Color(190, 30, 30));
                progressRisk.setForeground(new java.awt.Color(220, 53, 69));
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Transaction ID : ").append(result.getTransactionId()).append("\n");
            sb.append("Card Evaluated : ").append(tx.getMaskedCardNumber()).append("\n");
            sb.append("Decision Tier  : ").append(result.getDecision()).append("\n\n");
            sb.append("Triggered Rules & Analysis:\n");
            if (result.getTriggeredRules().isEmpty()) {
                sb.append("  [CLEAN] No heuristic anomalies detected.\n");
                sb.append("  [ML] Transaction vector conforms to legitimate spending distribution.\n");
            } else {
                for (String r : result.getTriggeredRules()) {
                    sb.append("  * ").append(r).append("\n");
                }
            }
            txtReasonCodes.setText(sb.toString());

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error in inputs: " + ex.getMessage(), "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel createSimulatorTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Stats Bar
        JPanel statsPanel = new JPanel(new GridLayout(1, 4, 15, 0));
        statsPanel.setPreferredSize(new Dimension(800, 70));

        lblStatTotal = createStatCard("Total Transactions", "0", new java.awt.Color(50, 70, 95));
        lblStatApproved = createStatCard("Approved", "0", new java.awt.Color(34, 139, 34));
        lblStatFlagged = createStatCard("Flagged (2FA)", "0", new java.awt.Color(210, 140, 10));
        lblStatBlocked = createStatCard("Blocked (Fraud)", "0", new java.awt.Color(200, 40, 40));

        statsPanel.add(lblStatTotal.getParent());
        statsPanel.add(lblStatApproved.getParent());
        statsPanel.add(lblStatFlagged.getParent());
        statsPanel.add(lblStatBlocked.getParent());

        // Table
        String[] cols = {"Tx ID", "Card", "Amount ($)", "Category", "Risk Score", "Decision", "Flags / Reason"};
        streamTableModel = new DefaultTableModel(cols, 0);
        JTable table = new JTable(streamTableModel);
        table.setRowHeight(24);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        // Custom Cell Renderer for Decisions
        table.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, isSelected, hasFocus, row, col);
                String str = val != null ? val.toString() : "";
                if (str.contains("APPROVED")) {
                    l.setForeground(new java.awt.Color(34, 139, 34));
                    l.setFont(l.getFont().deriveFont(Font.BOLD));
                } else if (str.contains("FLAGGED")) {
                    l.setForeground(new java.awt.Color(200, 130, 0));
                    l.setFont(l.getFont().deriveFont(Font.BOLD));
                } else if (str.contains("REJECTED")) {
                    l.setForeground(new java.awt.Color(200, 40, 40));
                    l.setFont(l.getFont().deriveFont(Font.BOLD));
                }
                return l;
            }
        });

        JScrollPane scrollTable = new JScrollPane(table);

        // Control buttons
        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        JButton btnRunBatch = new JButton("Run Batch Stream (25 Tx)");
        btnRunBatch.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnRunBatch.addActionListener(e -> runBatchSimulation(25));

        JButton btnInjectFraud = new JButton("Inject Live Fraud Burst (5 Attacks)");
        btnInjectFraud.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnInjectFraud.setForeground(new java.awt.Color(190, 20, 20));
        btnInjectFraud.addActionListener(e -> injectFraudBurst());

        JButton btnClear = new JButton("Clear Table");
        btnClear.addActionListener(e -> {
            streamTableModel.setRowCount(0);
            updateStats(0, 0, 0, 0);
        });

        btnBar.add(btnRunBatch);
        btnBar.add(btnInjectFraud);
        btnBar.add(btnClear);

        panel.add(statsPanel, BorderLayout.NORTH);
        panel.add(scrollTable, BorderLayout.CENTER);
        panel.add(btnBar, BorderLayout.SOUTH);

        return panel;
    }

    private JLabel createStatCard(String title, String val, java.awt.Color col) {
        JPanel card = new JPanel(new GridLayout(2, 1));
        card.setBorder(BorderFactory.createLineBorder(new java.awt.Color(210, 220, 230)));
        card.setBackground(Color.WHITE);

        JLabel tLbl = new JLabel(title, SwingConstants.CENTER);
        tLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tLbl.setForeground(new java.awt.Color(100, 110, 120));

        JLabel vLbl = new JLabel(val, SwingConstants.CENTER);
        vLbl.setFont(new Font("Segoe UI", Font.BOLD, 22));
        vLbl.setForeground(col);

        card.add(tLbl);
        card.add(vLbl);
        return vLbl;
    }

    private void runBatchSimulation(int count) {
        List<Transaction> list = benchmarkData.transactions.subList(0, Math.min(count, benchmarkData.transactions.size()));
        int app = 0, flg = 0, blk = 0;

        for (Transaction tx : list) {
            DetectionResult res = engine.evaluate(tx);
            String reason = res.getTriggeredRules().isEmpty() ? "Normal Profile" : res.getTriggeredRules().get(0);
            streamTableModel.addRow(new Object[]{
                    tx.getTransactionId(),
                    tx.getMaskedCardNumber(),
                    String.format("%.2f", tx.getAmount()),
                    tx.getMerchantCategory(),
                    String.format("%.1f", res.getRiskScore()),
                    res.getDecision(),
                    reason
            });

            if (res.getRiskLevel() == RiskLevel.LOW) app++;
            else if (res.getRiskLevel() == RiskLevel.MEDIUM) flg++;
            else blk++;
        }
        updateStats(streamTableModel.getRowCount(), app, flg, blk);
    }

    private void injectFraudBurst() {
        String card = benchmarkData.profiles.keySet().iterator().next();
        CardholderProfile prof = engine.getProfile(card);

        // 5 rapid suspicious transactions
        Transaction[] attacks = new Transaction[]{
                new Transaction("ATK-101", card, System.currentTimeMillis(), 0.50, "Micro-Auth-Bot", "ELECTRONICS", prof.getHomeLatitude(), prof.getHomeLongitude(), true),
                new Transaction("ATK-102", card, System.currentTimeMillis() + 15000, 0.75, "Micro-Auth-Bot", "ELECTRONICS", prof.getHomeLatitude(), prof.getHomeLongitude(), true),
                new Transaction("ATK-103", card, System.currentTimeMillis() + 30000, 1500.00, "DarkMarket-Ex", "CRYPTO", prof.getHomeLatitude(), prof.getHomeLongitude(), true),
                new Transaction("ATK-104", card, System.currentTimeMillis() + 60000, 2400.00, "Tokyo-Casino", "GAMBLING", 35.6762, 139.6503, true),
                new Transaction("ATK-105", card, System.currentTimeMillis() + 90000, 6000.00, "Overseas-Wire", "WIRE_TRANSFER", 51.5074, -0.1278, true)
        };

        for (Transaction tx : attacks) {
            DetectionResult res = engine.evaluate(tx);
            String reason = res.getTriggeredRules().isEmpty() ? "ML Outlier" : res.getTriggeredRules().get(0);
            streamTableModel.insertRow(0, new Object[]{
                    tx.getTransactionId(),
                    tx.getMaskedCardNumber(),
                    String.format("%.2f", tx.getAmount()),
                    tx.getMerchantCategory(),
                    String.format("%.1f", res.getRiskScore()),
                    res.getDecision(),
                    reason
            });
        }
    }

    private void updateStats(int total, int app, int flg, int blk) {
        lblStatTotal.setText(String.valueOf(total));
        lblStatApproved.setText(String.valueOf(app));
        lblStatFlagged.setText(String.valueOf(flg));
        lblStatBlocked.setText(String.valueOf(blk));
    }

    private JPanel createMetricsTab() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Top KPI Cards
        JPanel kpiPanel = new JPanel(new GridLayout(1, 4, 15, 0));
        kpiPanel.setPreferredSize(new Dimension(800, 80));

        lblAcc = createStatCard("Accuracy", String.format("%.2f%%", latestMetrics.accuracy * 100), new java.awt.Color(30, 110, 200));
        lblPrec = createStatCard("Precision (PPV)", String.format("%.2f%%", latestMetrics.precision * 100), new java.awt.Color(34, 139, 34));
        lblRec = createStatCard("Recall (Sensitivity)", String.format("%.2f%%", latestMetrics.recall * 100), new java.awt.Color(210, 100, 10));
        lblF1 = createStatCard("F1-Score", String.format("%.2f%%", latestMetrics.f1Score * 100), new java.awt.Color(120, 50, 160));

        kpiPanel.add(lblAcc.getParent());
        kpiPanel.add(lblPrec.getParent());
        kpiPanel.add(lblRec.getParent());
        kpiPanel.add(lblF1.getParent());

        // Center Confusion Matrix Panel
        JPanel cmPanel = new JPanel(new GridBagLayout());
        cmPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new java.awt.Color(180, 195, 210)),
                "CONFUSION MATRIX (Binary Classification Benchmark)", TitledBorder.CENTER, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 15)));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 12, 8, 12);
        c.fill = GridBagConstraints.BOTH;

        // Labels
        c.gridx = 1; c.gridy = 0;
        JLabel h1 = new JLabel("Actual Fraud (Positive)", SwingConstants.CENTER);
        h1.setFont(new Font("Segoe UI", Font.BOLD, 13));
        cmPanel.add(h1, c);

        c.gridx = 2;
        JLabel h2 = new JLabel("Actual Legit (Negative)", SwingConstants.CENTER);
        h2.setFont(new Font("Segoe UI", Font.BOLD, 13));
        cmPanel.add(h2, c);

        c.gridx = 0; c.gridy = 1;
        JLabel v1 = new JLabel("Predicted Fraud", SwingConstants.RIGHT);
        v1.setFont(new Font("Segoe UI", Font.BOLD, 13));
        cmPanel.add(v1, c);

        c.gridy = 2;
        JLabel v2 = new JLabel("Predicted Legit", SwingConstants.RIGHT);
        v2.setFont(new Font("Segoe UI", Font.BOLD, 13));
        cmPanel.add(v2, c);

        // Matrix Boxes
        c.gridx = 1; c.gridy = 1;
        lblTP = createMatrixCell("True Positive (TP)", latestMetrics.truePositives, new java.awt.Color(220, 245, 220));
        cmPanel.add(lblTP, c);

        c.gridx = 2;
        lblFP = createMatrixCell("False Positive (FP)", latestMetrics.falsePositives, new java.awt.Color(255, 230, 230));
        cmPanel.add(lblFP, c);

        c.gridx = 1; c.gridy = 2;
        lblFN = createMatrixCell("False Negative (FN)", latestMetrics.falseNegatives, new java.awt.Color(255, 230, 230));
        cmPanel.add(lblFN, c);

        c.gridx = 2;
        lblTN = createMatrixCell("True Negative (TN)", latestMetrics.trueNegatives, new java.awt.Color(220, 245, 220));
        cmPanel.add(lblTN, c);

        panel.add(kpiPanel, BorderLayout.NORTH);
        panel.add(cmPanel, BorderLayout.CENTER);

        return panel;
    }

    private JLabel createMatrixCell(String name, int count, java.awt.Color bg) {
        JLabel label = new JLabel(String.format("<html><center><b>%s</b><br><font size=5>%d</font></center></html>", name, count), SwingConstants.CENTER);
        label.setOpaque(true);
        label.setBackground(bg);
        label.setBorder(BorderFactory.createLineBorder(new java.awt.Color(160, 180, 200)));
        label.setPreferredSize(new Dimension(180, 90));
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        return label;
    }

    private JPanel createProfilesTab() {
        JPanel panel = new JPanel(new GridLayout(2, 1, 10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Profiles Table
        String[] profCols = {"Card Number", "Baseline Avg ($)", "Std Dev (σ)", "Home Coordinates", "Last Location"};
        DefaultTableModel profModel = new DefaultTableModel(profCols, 0);
        for (CardholderProfile p : benchmarkData.profiles.values()) {
            profModel.addRow(new Object[]{
                    p.getCardNumber(),
                    String.format("$%.2f", p.getAverageAmount()),
                    String.format("$%.2f", p.getStandardDeviation()),
                    String.format("%.4f, %.4f", p.getHomeLatitude(), p.getHomeLongitude()),
                    String.format("%.4f, %.4f", p.getLastLatitude(), p.getLastLongitude())
            });
        }
        JTable profTable = new JTable(profModel);
        profTable.setRowHeight(22);
        JScrollPane scrollProf = new JScrollPane(profTable);
        scrollProf.setBorder(BorderFactory.createTitledBorder("Active Cardholder Behavioral Baselines"));

        // Rules Table
        String[] ruleCols = {"Rule Name", "Evaluation Logic", "Penalty Range"};
        DefaultTableModel ruleModel = new DefaultTableModel(ruleCols, 0);
        ruleModel.addRow(new Object[]{"Velocity Rule", "Detects rapid bursts (>3 in 2m, >6 in 10m)", "0 - 85 pts"});
        ruleModel.addRow(new Object[]{"Impossible Geolocation Rule", "Haversine distance & travel speed > 800 km/h", "0 - 95 pts"});
        ruleModel.addRow(new Object[]{"Amount Outlier Rule", "Z-Score > 3.5σ or 5x baseline average spend", "0 - 80 pts"});
        ruleModel.addRow(new Object[]{"Merchant Risk Rule", "High-risk verticals (Crypto, Gambling, Wire)", "0 - 85 pts"});
        ruleModel.addRow(new Object[]{"Unusual Time Rule", "Late-night dormant hours (01:00 - 05:00 AM)", "0 - 40 pts"});

        JTable ruleTable = new JTable(ruleModel);
        ruleTable.setRowHeight(22);
        JScrollPane scrollRules = new JScrollPane(ruleTable);
        scrollRules.setBorder(BorderFactory.createTitledBorder("Active Heuristic Rule Specifications"));

        panel.add(scrollProf);
        panel.add(scrollRules);

        return panel;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new FraudDetectionGUI().setVisible(true);
        });
    }
}
