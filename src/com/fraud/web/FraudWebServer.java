package com.fraud.web;

import com.fraud.data.SyntheticDataGenerator;
import com.fraud.engine.FraudDetectionEngine;
import com.fraud.engine.ml.Dataset;
import com.fraud.engine.ml.ModelEvaluator;
import com.fraud.engine.ml.RandomForest;
import com.fraud.model.CardholderProfile;
import com.fraud.model.DetectionResult;
import com.fraud.model.RiskLevel;
import com.fraud.model.Transaction;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.awt.Desktop;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Built-in Lightweight HTTP Web Server and REST API.
 * Uses Java standard library (com.sun.net.httpserver) with ZERO external dependencies.
 * Serves an interactive modern web dashboard on http://localhost:8080.
 */
public class FraudWebServer {
    private final int port;
    private final FraudDetectionEngine engine;
    private final RandomForest model;
    private SyntheticDataGenerator.GeneratedData benchmarkData;
    private ModelEvaluator.Metrics latestMetrics;

    public FraudWebServer(int port) {
        this.port = port;
        this.model = new RandomForest(20, 7, 8, 42L);
        this.engine = new FraudDetectionEngine(model);
        initBackend();
    }

    public FraudWebServer() {
        this(8080);
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

    public void start() throws IOException {
        HttpServer server;
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
        } catch (IOException e) {
            // Port might be in use, fall back to 8085
            server = HttpServer.create(new InetSocketAddress(8085), 0);
        }

        int activePort = server.getAddress().getPort();
        String url = "http://localhost:" + activePort;

        server.createContext("/", new DashboardHandler());
        server.createContext("/api/assess", new AssessHandler());
        server.createContext("/api/stream", new StreamHandler());
        server.createContext("/api/metrics", new MetricsHandler());
        server.createContext("/api/profiles", new ProfilesHandler());
        server.createContext("/api/inject-burst", new InjectBurstHandler());

        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.start();

        System.out.println("==================================================================");
        System.out.println("   CREDIT CARD FRAUD DETECTION WEB SERVER RUNNING SUCCESSFULLY!   ");
        System.out.println("==================================================================");
        System.out.println(" Web Dashboard URL : " + url);
        System.out.println(" Opening dashboard in your default web browser...");
        System.out.println(" Press Ctrl+C in this terminal to stop the server.");
        System.out.println("==================================================================");

        openBrowser(url);
    }

    private void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                Runtime.getRuntime().exec("cmd /c start " + url);
            }
        } catch (Exception e) {
            System.out.println("Notice: Could not automatically open browser. Please navigate to: " + url);
        }
    }

    private static void sendJsonResponse(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private class DashboardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            if (!"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            String html = getDashboardHtml();
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            if ("GET".equalsIgnoreCase(method)) {
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } else {
                exchange.getResponseBody().close();
            }
        }
    }

    private class MetricsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String json = String.format(Locale.US,
                    "{\"accuracy\":%.4f,\"precision\":%.4f,\"recall\":%.4f,\"f1Score\":%.4f,\"tp\":%d,\"fp\":%d,\"tn\":%d,\"fn\":%d,\"total\":%d}",
                    latestMetrics.accuracy, latestMetrics.precision, latestMetrics.recall, latestMetrics.f1Score,
                    latestMetrics.truePositives, latestMetrics.falsePositives, latestMetrics.trueNegatives, latestMetrics.falseNegatives,
                    latestMetrics.totalSamples);
            sendJsonResponse(exchange, 200, json);
        }
    }

    private class ProfilesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (CardholderProfile p : benchmarkData.profiles.values()) {
                if (!first) sb.append(",");
                first = false;
                sb.append(String.format(Locale.US,
                        "{\"card\":\"%s\",\"avgAmount\":%.2f,\"stdDev\":%.2f,\"homeLat\":%.4f,\"homeLon\":%.4f}",
                        p.getCardNumber(), p.getAverageAmount(), p.getStandardDeviation(), p.getHomeLatitude(), p.getHomeLongitude()));
            }
            sb.append("]");
            sendJsonResponse(exchange, 200, sb.toString());
        }
    }

    private class StreamHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            List<Transaction> samples = benchmarkData.transactions.subList(0, Math.min(25, benchmarkData.transactions.size()));
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Transaction tx : samples) {
                if (!first) sb.append(",");
                first = false;
                DetectionResult res = engine.evaluate(tx);
                String reason = res.getTriggeredRules().isEmpty() ? "Normal Profile" : res.getTriggeredRules().get(0);
                reason = reason.replace("\"", "\\\"");

                sb.append(String.format(Locale.US,
                        "{\"id\":\"%s\",\"card\":\"%s\",\"amount\":%.2f,\"category\":\"%s\",\"score\":%.1f,\"decision\":\"%s\",\"level\":\"%s\",\"reason\":\"%s\",\"isFraud\":%b}",
                        tx.getTransactionId(), tx.getMaskedCardNumber(), tx.getAmount(), tx.getMerchantCategory(),
                        res.getRiskScore(), res.getDecision(), res.getRiskLevel().name(), reason, res.isFraud()));
            }
            sb.append("]");
            sendJsonResponse(exchange, 200, sb.toString());
        }
    }

    private class InjectBurstHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String card = benchmarkData.profiles.keySet().iterator().next();
            CardholderProfile prof = engine.getProfile(card);
            long now = System.currentTimeMillis();

            Transaction[] attacks = new Transaction[]{
                    new Transaction("ATK-101", card, now, 0.50, "Micro-Auth-Bot", "ELECTRONICS", prof.getHomeLatitude(), prof.getHomeLongitude(), true),
                    new Transaction("ATK-102", card, now + 12000, 0.75, "Micro-Auth-Bot", "ELECTRONICS", prof.getHomeLatitude(), prof.getHomeLongitude(), true),
                    new Transaction("ATK-103", card, now + 25000, 1850.00, "DarkMarket-Ex", "CRYPTO", prof.getHomeLatitude(), prof.getHomeLongitude(), true),
                    new Transaction("ATK-104", card, now + 45000, 3200.00, "Tokyo-Casino", "GAMBLING", 35.6762, 139.6503, true),
                    new Transaction("ATK-105", card, now + 70000, 7500.00, "Overseas-Wire", "WIRE_TRANSFER", 51.5074, -0.1278, true)
            };

            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < attacks.length; i++) {
                if (i > 0) sb.append(",");
                Transaction tx = attacks[i];
                DetectionResult res = engine.evaluate(tx);
                String reason = res.getTriggeredRules().isEmpty() ? "ML Spike" : res.getTriggeredRules().get(0);
                reason = reason.replace("\"", "\\\"");

                sb.append(String.format(Locale.US,
                        "{\"id\":\"%s\",\"card\":\"%s\",\"amount\":%.2f,\"category\":\"%s\",\"score\":%.1f,\"decision\":\"%s\",\"level\":\"%s\",\"reason\":\"%s\",\"isFraud\":true}",
                        tx.getTransactionId(), tx.getMaskedCardNumber(), tx.getAmount(), tx.getMerchantCategory(),
                        res.getRiskScore(), res.getDecision(), res.getRiskLevel().name(), reason));
            }
            sb.append("]");
            sendJsonResponse(exchange, 200, sb.toString());
        }
    }

    private class AssessHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 200, "{}");
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }

            String body;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                body = sb.toString();
            }

            // Simple parser for incoming JSON fields
            String card = extractJsonString(body, "cardNumber");
            if (card == null || card.isEmpty()) card = benchmarkData.profiles.keySet().iterator().next();

            double amount = extractJsonDouble(body, "amount", 45.0);
            String merchant = extractJsonString(body, "merchant");
            if (merchant == null || merchant.isEmpty()) merchant = "Retail-Store";
            String category = extractJsonString(body, "category");
            if (category == null || category.isEmpty()) category = "ELECTRONICS";

            CardholderProfile prof = engine.getOrCreateProfile(card, 50.0, 40.7128, -74.0060);
            double lat = extractJsonDouble(body, "latitude", prof.getHomeLatitude());
            double lon = extractJsonDouble(body, "longitude", prof.getHomeLongitude());

            Transaction tx = new Transaction("TX-WEB-" + (System.currentTimeMillis() % 100000),
                    card, System.currentTimeMillis(), amount, merchant, category, lat, lon);

            DetectionResult result = engine.evaluate(tx);

            StringBuilder reasonsJson = new StringBuilder("[");
            for (int i = 0; i < result.getTriggeredRules().size(); i++) {
                if (i > 0) reasonsJson.append(",");
                reasonsJson.append("\"").append(result.getTriggeredRules().get(i).replace("\"", "\\\"")).append("\"");
            }
            reasonsJson.append("]");

            String respJson = String.format(Locale.US,
                    "{\"transactionId\":\"%s\",\"card\":\"%s\",\"riskScore\":%.1f,\"riskLevel\":\"%s\",\"decision\":\"%s\",\"mlProbability\":%.4f,\"ruleScore\":%.1f,\"triggeredRules\":%s}",
                    result.getTransactionId(), tx.getMaskedCardNumber(), result.getRiskScore(),
                    result.getRiskLevel().name(), result.getDecision(), result.getMlProbability(),
                    result.getRuleScore(), reasonsJson.toString());

            sendJsonResponse(exchange, 200, respJson);
        }
    }

    private static String extractJsonString(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx == -1) return null;
        int colon = json.indexOf(':', idx + search.length());
        if (colon == -1) return null;
        int firstQuote = json.indexOf('"', colon);
        if (firstQuote == -1) return null;
        int secondQuote = json.indexOf('"', firstQuote + 1);
        if (secondQuote == -1) return null;
        return json.substring(firstQuote + 1, secondQuote);
    }

    private static double extractJsonDouble(String json, String key, double defaultVal) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx == -1) return defaultVal;
        int colon = json.indexOf(':', idx + search.length());
        if (colon == -1) return defaultVal;
        int start = colon + 1;
        while (start < json.length() && (Character.isWhitespace(json.charAt(start)) || json.charAt(start) == '"')) {
            start++;
        }
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.' || json.charAt(end) == '-')) {
            end++;
        }
        try {
            return Double.parseDouble(json.substring(start, end));
        } catch (Exception e) {
            return defaultVal;
        }
    }

    private String getDashboardHtml() {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "  <meta charset=\"UTF-8\">\n" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "  <title>Credit Card Fraud Detection System - Web Portal</title>\n" +
                "  <style>\n" +
                "    :root {\n" +
                "      --primary: #2563eb;\n" +
                "      --primary-dark: #1d4ed8;\n" +
                "      --bg: #0f172a;\n" +
                "      --card-bg: #1e293b;\n" +
                "      --card-border: #334155;\n" +
                "      --text: #f8fafc;\n" +
                "      --text-muted: #94a3b8;\n" +
                "      --green: #10b981;\n" +
                "      --yellow: #f59e0b;\n" +
                "      --red: #ef4444;\n" +
                "    }\n" +
                "    * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; }\n" +
                "    body { background-color: var(--bg); color: var(--text); padding: 20px; min-height: 100vh; }\n" +
                "    .container { max-width: 1200px; margin: 0 auto; }\n" +
                "    header { background: linear-gradient(135deg, #1e3a8a 0%, #1e293b 100%); border: 1px solid var(--card-border); border-radius: 12px; padding: 24px; margin-bottom: 24px; display: flex; justify-content: space-between; align-items: center; box-shadow: 0 10px 25px -5px rgba(0,0,0,0.3); }\n" +
                "    h1 { font-size: 24px; font-weight: 800; letter-spacing: -0.5px; }\n" +
                "    .badge-tag { background: rgba(37,99,235,0.25); border: 1px solid #3b82f6; color: #93c5fd; padding: 4px 10px; border-radius: 20px; font-size: 12px; font-weight: 600; }\n" +
                "    .tabs { display: flex; gap: 8px; margin-bottom: 20px; border-bottom: 1px solid var(--card-border); padding-bottom: 10px; }\n" +
                "    .tab-btn { background: transparent; border: none; color: var(--text-muted); font-size: 15px; font-weight: 600; padding: 8px 16px; border-radius: 8px; cursor: pointer; transition: all 0.2s; }\n" +
                "    .tab-btn.active { background: var(--primary); color: white; }\n" +
                "    .tab-content { display: none; }\n" +
                "    .tab-content.active { display: block; }\n" +
                "    .grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 24px; }\n" +
                "    .card { background: var(--card-bg); border: 1px solid var(--card-border); border-radius: 12px; padding: 20px; box-shadow: 0 4px 12px rgba(0,0,0,0.2); }\n" +
                "    .card h2 { font-size: 18px; margin-bottom: 16px; border-bottom: 1px solid var(--card-border); padding-bottom: 8px; color: #93c5fd; }\n" +
                "    .form-group { margin-bottom: 14px; }\n" +
                "    label { display: block; font-size: 13px; font-weight: 600; color: var(--text-muted); margin-bottom: 6px; }\n" +
                "    input, select { width: 100%; background: #0f172a; border: 1px solid var(--card-border); border-radius: 6px; padding: 10px 12px; color: var(--text); font-size: 14px; outline: none; }\n" +
                "    input:focus, select:focus { border-color: var(--primary); }\n" +
                "    .btn-group { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin: 14px 0; }\n" +
                "    .btn-preset { background: #334155; border: 1px solid #475569; color: var(--text); padding: 8px 10px; border-radius: 6px; font-size: 12px; cursor: pointer; text-align: left; transition: all 0.2s; }\n" +
                "    .btn-preset:hover { background: #475569; }\n" +
                "    .btn-submit { width: 100%; background: var(--primary); border: none; color: white; padding: 12px; border-radius: 8px; font-size: 15px; font-weight: 700; cursor: pointer; transition: background 0.2s; }\n" +
                "    .btn-submit:hover { background: var(--primary-dark); }\n" +
                "    .result-box { text-align: center; padding: 20px; border-radius: 10px; background: #0f172a; border: 1px solid var(--card-border); margin-bottom: 16px; }\n" +
                "    .decision-badge { display: inline-block; padding: 8px 18px; border-radius: 30px; font-size: 18px; font-weight: 800; text-transform: uppercase; margin-bottom: 12px; }\n" +
                "    .bg-approved { background: rgba(16,185,129,0.2); border: 1px solid var(--green); color: var(--green); }\n" +
                "    .bg-flagged { background: rgba(245,158,11,0.2); border: 1px solid var(--yellow); color: var(--yellow); }\n" +
                "    .bg-rejected { background: rgba(239,68,68,0.2); border: 1px solid var(--red); color: var(--red); }\n" +
                "    .gauge-container { width: 100%; background: #334155; height: 16px; border-radius: 8px; overflow: hidden; margin: 12px 0; }\n" +
                "    .gauge-fill { height: 100%; width: 0%; transition: width 0.5s ease-in-out; }\n" +
                "    .kpi-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 24px; }\n" +
                "    .kpi-box { background: var(--card-bg); border: 1px solid var(--card-border); border-radius: 10px; padding: 16px; text-align: center; }\n" +
                "    .kpi-val { font-size: 24px; font-weight: 800; margin-top: 6px; }\n" +
                "    table { width: 100%; border-collapse: collapse; font-size: 13px; }\n" +
                "    th { background: #0f172a; padding: 10px 12px; text-align: left; color: var(--text-muted); border-bottom: 1px solid var(--card-border); }\n" +
                "    td { padding: 10px 12px; border-bottom: 1px solid var(--card-border); }\n" +
                "    .tag { padding: 3px 8px; border-radius: 4px; font-size: 11px; font-weight: 700; }\n" +
                "    .cm-grid { display: grid; grid-template-columns: 140px 1fr 1fr; gap: 10px; max-width: 600px; margin: 20px auto; text-align: center; }\n" +
                "    .cm-cell { background: #0f172a; border: 1px solid var(--card-border); border-radius: 8px; padding: 20px; font-size: 13px; }\n" +
                "    .cm-num { font-size: 26px; font-weight: 800; margin-top: 8px; }\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "<div class=\"container\">\n" +
                "  <header>\n" +
                "    <div>\n" +
                "      <h1>CREDIT CARD FRAUD DETECTION SYSTEM</h1>\n" +
                "      <p style=\"color: var(--text-muted); font-size: 13px; margin-top: 4px;\">Hybrid Intelligence: Random Forest ML (20 Bagging Trees) + Multi-Factor Behavioral Engine</p>\n" +
                "    </div>\n" +
                "    <div>\n" +
                "      <span class=\"badge-tag\">LIVE PORT 8080</span>\n" +
                "      <span class=\"badge-tag\" style=\"margin-left: 6px;\">JDK 21</span>\n" +
                "    </div>\n" +
                "  </header>\n" +
                "\n" +
                "  <div class=\"tabs\">\n" +
                "    <button class=\"tab-btn active\" onclick=\"switchTab('assessment')\">Real-Time Assessment</button>\n" +
                "    <button class=\"tab-btn\" onclick=\"switchTab('stream')\">Live Stream Simulator</button>\n" +
                "    <button class=\"tab-btn\" onclick=\"switchTab('metrics')\">ML Confusion Matrix</button>\n" +
                "    <button class=\"tab-btn\" onclick=\"switchTab('profiles')\">Cardholder Profiles</button>\n" +
                "  </div>\n" +
                "\n" +
                "  <!-- TAB 1: REAL-TIME ASSESSMENT -->\n" +
                "  <div id=\"tab-assessment\" class=\"tab-content active\">\n" +
                "    <div class=\"grid-2\">\n" +
                "      <div class=\"card\">\n" +
                "        <h2>Transaction Input & Scenario Presets</h2>\n" +
                "        <div class=\"form-group\">\n" +
                "          <label>Target Cardholder</label>\n" +
                "          <select id=\"selCard\"></select>\n" +
                "        </div>\n" +
                "        <div class=\"form-group\">\n" +
                "          <label>Transaction Amount ($)</label>\n" +
                "          <input type=\"number\" id=\"inpAmount\" value=\"45.00\" step=\"0.01\">\n" +
                "        </div>\n" +
                "        <div class=\"form-group\">\n" +
                "          <label>Merchant Name</label>\n" +
                "          <input type=\"text\" id=\"inpMerchant\" value=\"Costco-Wholesale\">\n" +
                "        </div>\n" +
                "        <div class=\"form-group\">\n" +
                "          <label>Merchant Category</label>\n" +
                "          <select id=\"selCategory\">\n" +
                "            <option value=\"GROCERY\">GROCERY</option>\n" +
                "            <option value=\"DINING\">DINING</option>\n" +
                "            <option value=\"RETAIL\">RETAIL</option>\n" +
                "            <option value=\"ELECTRONICS\">ELECTRONICS</option>\n" +
                "            <option value=\"CRYPTO\">CRYPTO</option>\n" +
                "            <option value=\"GAMBLING\">GAMBLING</option>\n" +
                "            <option value=\"LUXURY_JEWELRY\">LUXURY_JEWELRY</option>\n" +
                "            <option value=\"WIRE_TRANSFER\">WIRE_TRANSFER</option>\n" +
                "          </select>\n" +
                "        </div>\n" +
                "        <div class=\"form-group\" style=\"display:grid; grid-template-columns:1fr 1fr; gap:10px;\">\n" +
                "          <div>\n" +
                "            <label>Latitude</label>\n" +
                "            <input type=\"text\" id=\"inpLat\" value=\"40.7128\">\n" +
                "          </div>\n" +
                "          <div>\n" +
                "            <label>Longitude</label>\n" +
                "            <input type=\"text\" id=\"inpLon\" value=\"-74.0060\">\n" +
                "          </div>\n" +
                "        </div>\n" +
                "        <label>Instant Attack Scenario Presets:</label>\n" +
                "        <div class=\"btn-group\">\n" +
                "          <button class=\"btn-preset\" onclick=\"applyPreset(35.0, 'Walmart', 'GROCERY', 40.7128, -74.0060)\">🟢 1. Normal Grocery ($35)</button>\n" +
                "          <button class=\"btn-preset\" onclick=\"applyPreset(750.0, 'Tokyo-DutyFree', 'ELECTRONICS', 35.6762, 139.6503)\">🔴 2. Impossible Travel (Tokyo)</button>\n" +
                "          <button class=\"btn-preset\" onclick=\"applyPreset(4800.0, 'CoinVault-ATM', 'CRYPTO', 40.7128, -74.0060)\">🔴 3. Crypto Spike ($4,800)</button>\n" +
                "          <button class=\"btn-preset\" onclick=\"applyPreset(0.50, 'Probe-Bot-Inc', 'ELECTRONICS', 40.7128, -74.0060)\">🟡 4. Micro Card-Probe ($0.50)</button>\n" +
                "        </div>\n" +
                "        <button class=\"btn-submit\" onclick=\"assessTransaction()\">EVALUATE TRANSACTION RISK</button>\n" +
                "      </div>\n" +
                "\n" +
                "      <div class=\"card\">\n" +
                "        <h2>Risk Assessment Outcome</h2>\n" +
                "        <div class=\"result-box\">\n" +
                "          <div id=\"resBadge\" class=\"decision-badge bg-approved\">READY FOR INPUT</div>\n" +
                "          <div style=\"font-size: 14px; color: var(--text-muted);\" id=\"resSub\">Select a scenario or click Evaluate</div>\n" +
                "          <div class=\"gauge-container\">\n" +
                "            <div id=\"gaugeFill\" class=\"gauge-fill\" style=\"background: var(--green);\"></div>\n" +
                "          </div>\n" +
                "          <div style=\"display:flex; justify-content:space-between; font-size:12px; color:var(--text-muted);\">\n" +
                "            <span>Low Risk (0)</span>\n" +
                "            <span id=\"lblScore\">Score: --/100</span>\n" +
                "            <span>Critical Risk (100)</span>\n" +
                "          </div>\n" +
                "        </div>\n" +
                "        <h3 style=\"font-size:14px; margin-bottom:10px; color:#93c5fd;\">Triggered Rule Signals & Diagnostics</h3>\n" +
                "        <ul id=\"resRules\" style=\"list-style:none; font-size:13px; line-height:1.7; color:#cbd5e1;\">\n" +
                "          <li>No evaluation executed yet.</li>\n" +
                "        </ul>\n" +
                "      </div>\n" +
                "    </div>\n" +
                "  </div>\n" +
                "\n" +
                "  <!-- TAB 2: LIVE STREAM SIMULATOR -->\n" +
                "  <div id=\"tab-stream\" class=\"tab-content\">\n" +
                "    <div class=\"card\">\n" +
                "      <div style=\"display:flex; justify-content:space-between; align-items:center; margin-bottom:16px;\">\n" +
                "        <h2>Live Transaction Stream Evaluation</h2>\n" +
                "        <div>\n" +
                "          <button class=\"btn-preset\" style=\"background:var(--primary); color:white;\" onclick=\"loadStream()\">Refresh Batch (25 Tx)</button>\n" +
                "          <button class=\"btn-preset\" style=\"background:var(--red); color:white;\" onclick=\"injectBurst()\">Inject Attack Burst (5 Tx)</button>\n" +
                "        </div>\n" +
                "      </div>\n" +
                "      <div style=\"overflow-x:auto;\">\n" +
                "        <table>\n" +
                "          <thead>\n" +
                "            <tr>\n" +
                "              <th>Transaction ID</th>\n" +
                "              <th>Card Number</th>\n" +
                "              <th>Amount</th>\n" +
                "              <th>Category</th>\n" +
                "              <th>Risk Score</th>\n" +
                "              <th>System Decision</th>\n" +
                "              <th>Triggered Heuristic Reason</th>\n" +
                "            </tr>\n" +
                "          </thead>\n" +
                "          <tbody id=\"streamBody\"></tbody>\n" +
                "        </table>\n" +
                "      </div>\n" +
                "    </div>\n" +
                "  </div>\n" +
                "\n" +
                "  <!-- TAB 3: METRICS -->\n" +
                "  <div id=\"tab-metrics\" class=\"tab-content\">\n" +
                "    <div class=\"kpi-row\">\n" +
                "      <div class=\"kpi-box\"><span style=\"color:var(--text-muted); font-size:13px;\">Accuracy</span><div id=\"kpiAcc\" class=\"kpi-val\" style=\"color:#38bdf8;\">--</div></div>\n" +
                "      <div class=\"kpi-box\"><span style=\"color:var(--text-muted); font-size:13px;\">Precision (PPV)</span><div id=\"kpiPrec\" class=\"kpi-val\" style=\"color:var(--green);\">--</div></div>\n" +
                "      <div class=\"kpi-box\"><span style=\"color:var(--text-muted); font-size:13px;\">Recall (Sensitivity)</span><div id=\"kpiRec\" class=\"kpi-val\" style=\"color:var(--yellow);\">--</div></div>\n" +
                "      <div class=\"kpi-box\"><span style=\"color:var(--text-muted); font-size:13px;\">F1-Score (Balanced)</span><div id=\"kpiF1\" class=\"kpi-val\" style=\"color:#c084fc;\">--</div></div>\n" +
                "    </div>\n" +
                "    <div class=\"card\">\n" +
                "      <h2>Academic Confusion Matrix (Test Split: 600 Records)</h2>\n" +
                "      <div class=\"cm-grid\">\n" +
                "        <div></div>\n" +
                "        <div style=\"font-weight:700; color:#93c5fd;\">Actual Fraud</div>\n" +
                "        <div style=\"font-weight:700; color:#93c5fd;\">Actual Legitimate</div>\n" +
                "\n" +
                "        <div style=\"font-weight:700; text-align:right; padding-top:25px;\">Predicted Fraud</div>\n" +
                "        <div class=\"cm-cell\" style=\"background:rgba(16,185,129,0.15); border-color:var(--green);\">\n" +
                "          <div>True Positive (TP)</div>\n" +
                "          <div id=\"cmTP\" class=\"cm-num\" style=\"color:var(--green);\">--</div>\n" +
                "        </div>\n" +
                "        <div class=\"cm-cell\" style=\"background:rgba(239,68,68,0.15); border-color:var(--red);\">\n" +
                "          <div>False Positive (FP)</div>\n" +
                "          <div id=\"cmFP\" class=\"cm-num\" style=\"color:var(--red);\">--</div>\n" +
                "        </div>\n" +
                "\n" +
                "        <div style=\"font-weight:700; text-align:right; padding-top:25px;\">Predicted Legit</div>\n" +
                "        <div class=\"cm-cell\" style=\"background:rgba(239,68,68,0.15); border-color:var(--red);\">\n" +
                "          <div>False Negative (FN)</div>\n" +
                "          <div id=\"cmFN\" class=\"cm-num\" style=\"color:var(--red);\">--</div>\n" +
                "        </div>\n" +
                "        <div class=\"cm-cell\" style=\"background:rgba(16,185,129,0.15); border-color:var(--green);\">\n" +
                "          <div>True Negative (TN)</div>\n" +
                "          <div id=\"cmTN\" class=\"cm-num\" style=\"color:var(--green);\">--</div>\n" +
                "        </div>\n" +
                "      </div>\n" +
                "    </div>\n" +
                "  </div>\n" +
                "\n" +
                "  <!-- TAB 4: PROFILES -->\n" +
                "  <div id=\"tab-profiles\" class=\"tab-content\">\n" +
                "    <div class=\"card\">\n" +
                "      <h2>Registered Cardholder Behavioral Baselines</h2>\n" +
                "      <table>\n" +
                "        <thead>\n" +
                "          <tr>\n" +
                "            <th>Card Number</th>\n" +
                "            <th>Baseline Average Spend ($)</th>\n" +
                "            <th>Spending Std Dev (&sigma;)</th>\n" +
                "            <th>Home Coordinates (Lat, Lon)</th>\n" +
                "          </tr>\n" +
                "        </thead>\n" +
                "        <tbody id=\"profBody\"></tbody>\n" +
                "      </table>\n" +
                "    </div>\n" +
                "  </div>\n" +
                "</div>\n" +
                "\n" +
                "<script>\n" +
                "  function switchTab(name) {\n" +
                "    document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));\n" +
                "    document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));\n" +
                "    event.target.classList.add('active');\n" +
                "    document.getElementById('tab-' + name).classList.add('active');\n" +
                "    if (name === 'stream') loadStream();\n" +
                "    if (name === 'metrics') loadMetrics();\n" +
                "    if (name === 'profiles') loadProfiles();\n" +
                "  }\n" +
                "\n" +
                "  function applyPreset(amt, merchant, cat, lat, lon) {\n" +
                "    document.getElementById('inpAmount').value = amt.toFixed(2);\n" +
                "    document.getElementById('inpMerchant').value = merchant;\n" +
                "    document.getElementById('selCategory').value = cat;\n" +
                "    document.getElementById('inpLat').value = lat.toFixed(4);\n" +
                "    document.getElementById('inpLon').value = lon.toFixed(4);\n" +
                "    assessTransaction();\n" +
                "  }\n" +
                "\n" +
                "  async function assessTransaction() {\n" +
                "    const payload = {\n" +
                "      cardNumber: document.getElementById('selCard').value,\n" +
                "      amount: parseFloat(document.getElementById('inpAmount').value),\n" +
                "      merchant: document.getElementById('inpMerchant').value,\n" +
                "      category: document.getElementById('selCategory').value,\n" +
                "      latitude: parseFloat(document.getElementById('inpLat').value),\n" +
                "      longitude: parseFloat(document.getElementById('inpLon').value)\n" +
                "    };\n" +
                "    const res = await fetch('/api/assess', {\n" +
                "      method: 'POST',\n" +
                "      headers: {'Content-Type': 'application/json'},\n" +
                "      body: JSON.stringify(payload)\n" +
                "    });\n" +
                "    const d = await res.json();\n" +
                "    const badge = document.getElementById('resBadge');\n" +
                "    const fill = document.getElementById('gaugeFill');\n" +
                "    fill.style.width = d.riskScore + '%';\n" +
                "    document.getElementById('lblScore').innerText = 'Score: ' + d.riskScore.toFixed(1) + '/100';\n" +
                "    badge.className = 'decision-badge';\n" +
                "    if (d.riskLevel === 'LOW') {\n" +
                "      badge.innerText = 'APPROVED (LOW RISK)';\n" +
                "      badge.classList.add('bg-approved');\n" +
                "      fill.style.background = 'var(--green)';\n" +
                "    } else if (d.riskLevel === 'MEDIUM') {\n" +
                "      badge.innerText = 'FLAGGED (2FA / OTP REQUIRED)';\n" +
                "      badge.classList.add('bg-flagged');\n" +
                "      fill.style.background = 'var(--yellow)';\n" +
                "    } else {\n" +
                "      badge.innerText = 'REJECTED / SUSPECTED FRAUD';\n" +
                "      badge.classList.add('bg-rejected');\n" +
                "      fill.style.background = 'var(--red)';\n" +
                "    }\n" +
                "    document.getElementById('resSub').innerText = 'ML Probability: ' + (d.mlProbability*100).toFixed(1) + '% | Rule Penalty: ' + d.ruleScore.toFixed(1);\n" +
                "    const ul = document.getElementById('resRules');\n" +
                "    ul.innerHTML = '';\n" +
                "    if (!d.triggeredRules || d.triggeredRules.length === 0) {\n" +
                "      ul.innerHTML = '<li>🟢 Clean Profile: Transaction conforms to established spending bounds.</li>';\n" +
                "    } else {\n" +
                "      d.triggeredRules.forEach(r => {\n" +
                "        ul.innerHTML += '<li style=\"color:#fca5a5;\">⚠️ ' + r + '</li>';\n" +
                "      });\n" +
                "    }\n" +
                "  }\n" +
                "\n" +
                "  async function loadProfiles() {\n" +
                "    const res = await fetch('/api/profiles');\n" +
                "    const list = await res.json();\n" +
                "    const sel = document.getElementById('selCard');\n" +
                "    sel.innerHTML = '';\n" +
                "    const tbody = document.getElementById('profBody');\n" +
                "    tbody.innerHTML = '';\n" +
                "    list.forEach(p => {\n" +
                "      sel.innerHTML += `<option value=\"${p.card}\">${p.card} (Avg: $${p.avgAmount})</option>`;\n" +
                "      tbody.innerHTML += `<tr><td><b>${p.card}</b></td><td>$${p.avgAmount.toFixed(2)}</td><td>&plusmn;$${p.stdDev.toFixed(2)}</td><td>${p.homeLat.toFixed(4)}, ${p.homeLon.toFixed(4)}</td></tr>`;\n" +
                "    });\n" +
                "  }\n" +
                "\n" +
                "  async function loadStream() {\n" +
                "    const res = await fetch('/api/stream');\n" +
                "    const list = await res.json();\n" +
                "    renderStreamTable(list);\n" +
                "  }\n" +
                "\n" +
                "  async function injectBurst() {\n" +
                "    const res = await fetch('/api/inject-burst');\n" +
                "    const list = await res.json();\n" +
                "    renderStreamTable(list, true);\n" +
                "  }\n" +
                "\n" +
                "  function renderStreamTable(list, prepend = false) {\n" +
                "    const tbody = document.getElementById('streamBody');\n" +
                "    let html = '';\n" +
                "    list.forEach(tx => {\n" +
                "      let badgeClass = tx.level === 'LOW' ? 'bg-approved' : (tx.level === 'MEDIUM' ? 'bg-flagged' : 'bg-rejected');\n" +
                "      html += `<tr>\n" +
                "        <td><code>${tx.id}</code></td>\n" +
                "        <td>${tx.card}</td>\n" +
                "        <td><b>$${tx.amount.toFixed(2)}</b></td>\n" +
                "        <td>${tx.category}</td>\n" +
                "        <td><b>${tx.score.toFixed(1)}/100</b></td>\n" +
                "        <td><span class=\"tag ${badgeClass}\">${tx.decision}</span></td>\n" +
                "        <td style=\"color:${tx.isFraud ? '#fca5a5' : '#94a3b8'};\">${tx.reason}</td>\n" +
                "      </tr>`;\n" +
                "    });\n" +
                "    if (prepend) {\n" +
                "      tbody.innerHTML = html + tbody.innerHTML;\n" +
                "    } else {\n" +
                "      tbody.innerHTML = html;\n" +
                "    }\n" +
                "  }\n" +
                "\n" +
                "  async function loadMetrics() {\n" +
                "    const res = await fetch('/api/metrics');\n" +
                "    const m = await res.json();\n" +
                "    document.getElementById('kpiAcc').innerText = (m.accuracy * 100).toFixed(2) + '%';\n" +
                "    document.getElementById('kpiPrec').innerText = (m.precision * 100).toFixed(2) + '%';\n" +
                "    document.getElementById('kpiRec').innerText = (m.recall * 100).toFixed(2) + '%';\n" +
                "    document.getElementById('kpiF1').innerText = (m.f1Score * 100).toFixed(2) + '%';\n" +
                "    document.getElementById('cmTP').innerText = m.tp;\n" +
                "    document.getElementById('cmFP').innerText = m.fp;\n" +
                "    document.getElementById('cmTN').innerText = m.tn;\n" +
                "    document.getElementById('cmFN').innerText = m.fn;\n" +
                "  }\n" +
                "\n" +
                "  // Initial load\n" +
                "  loadProfiles();\n" +
                "  loadMetrics();\n" +
                "</script>\n" +
                "</body>\n" +
                "</html>";
    }

    public static void main(String[] args) throws IOException {
        new FraudWebServer(8080).start();
    }
}
