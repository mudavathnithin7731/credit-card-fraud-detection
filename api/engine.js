// Hybrid Fraud Detection Engine (Serverless Edition for Vercel)

const LOCATIONS = [
  { lat: 40.7128, lon: -74.0060, city: "New York" },
  { lat: 34.0522, lon: -118.2437, city: "Los Angeles" },
  { lat: 41.8781, lon: -87.6298, city: "Chicago" },
  { lat: 51.5074, lon: -0.1278, city: "London" },
  { lat: 35.6762, lon: 139.6503, city: "Tokyo" },
  { lat: 37.7749, lon: -122.4194, city: "San Francisco" }
];

const PROFILES = {
  "4532-1082-8491-0111": { card: "4532-1082-8491-0111", avgAmount: 48.50, stdDev: 18.20, homeLat: 40.7128, homeLon: -74.0060, lastLat: 40.7128, lastLon: -74.0060, lastTs: 0 },
  "4532-4912-7319-0222": { card: "4532-4912-7319-0222", avgAmount: 85.00, stdDev: 24.50, homeLat: 34.0522, homeLon: -118.2437, lastLat: 34.0522, lastLon: -118.2437, lastTs: 0 },
  "4532-9182-3847-0333": { card: "4532-9182-3847-0333", avgAmount: 32.10, stdDev: 12.00, homeLat: 41.8781, homeLon: -87.6298, lastLat: 41.8781, lastLon: -87.6298, lastTs: 0 },
  "4532-6284-9102-0444": { card: "4532-6284-9102-0444", avgAmount: 110.00, stdDev: 35.00, homeLat: 37.7749, homeLon: -122.4194, lastLat: 37.7749, lastLon: -122.4194, lastTs: 0 }
};

const CATEGORY_RISK = {
  "CRYPTO": 45.0,
  "GAMBLING": 50.0,
  "CASINO": 50.0,
  "WIRE_TRANSFER": 40.0,
  "LUXURY_JEWELRY": 35.0,
  "ELECTRONICS": 20.0,
  "TRAVEL": 15.0,
  "RETAIL": 5.0,
  "GROCERY": 2.0,
  "DINING": 2.0,
  "UTILITIES": 1.0
};

// Haversine formula for distance in kilometers
function calculateDistanceKm(lat1, lon1, lat2, lon2) {
  const R = 6371.0;
  const dLat = (lat2 - lat1) * Math.PI / 180.0;
  const dLon = (lon2 - lon1) * Math.PI / 180.0;
  const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(lat1 * Math.PI / 180.0) * Math.cos(lat2 * Math.PI / 180.0) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c;
}

// Evaluate single transaction
function evaluateTransaction(tx) {
  const profile = PROFILES[tx.cardNumber] || {
    card: tx.cardNumber,
    avgAmount: 50.0,
    stdDev: 20.0,
    homeLat: tx.latitude || 40.7128,
    homeLon: tx.longitude || -74.0060,
    lastLat: tx.latitude || 40.7128,
    lastLon: tx.longitude || -74.0060,
    lastTs: Date.now() - 3600000
  };

  const triggeredRules = [];
  let maxRule = 0.0;
  let ruleSum = 0.0;
  let activeRules = 0;

  function addRule(name, penalty, reason) {
    if (penalty > 0) {
      activeRules++;
      ruleSum += penalty;
      if (penalty > maxRule) maxRule = penalty;
      triggeredRules.push(`[${name}] ${reason}`);
    }
  }

  // 1. Geolocation / Impossible Travel
  if (profile.lastTs && profile.lastTs > 0) {
    const elapsedHours = Math.max(0.001, (Date.now() - profile.lastTs) / 3600000.0);
    const distKm = calculateDistanceKm(profile.lastLat, profile.lastLon, tx.latitude, tx.longitude);
    if (distKm > 25.0) {
      const speedKmH = distKm / elapsedHours;
      if (speedKmH > 800.0) {
        addRule("Impossible Travel Velocity", 95.0,
          `Impossible Travel: ${distKm.toFixed(1)} km traversed in ${(elapsedHours * 60).toFixed(1)} mins (Speed: ${speedKmH.toFixed(0)} km/h).`);
      } else if (speedKmH > 400.0 && elapsedHours < 1.0) {
        addRule("Rapid Geolocation Shift", 45.0,
          `Suspicious transit: ${distKm.toFixed(1)} km traversed rapidly (${speedKmH.toFixed(0)} km/h).`);
      }
    }
  }

  // 2. Spending Anomaly / Outlier
  const amt = tx.amount;
  const avg = profile.avgAmount;
  const std = profile.stdDev;
  const zScore = (amt - avg) / std;

  if (amt <= 1.00 && avg >= 40.0) {
    addRule("Micro Card-Testing Probe", 35.0,
      `Potential bot probe: $${amt.toFixed(2)} authorization on card with $${avg.toFixed(2)} baseline.`);
  } else if (zScore >= 4.0 || amt >= avg * 5.0) {
    addRule("Amount Outlier Anomaly", 80.0,
      `Severe spending spike: $${amt.toFixed(2)} is ${(amt / Math.max(1, avg)).toFixed(1)}x baseline ($${avg.toFixed(2)}, Z-Score: +${zScore.toFixed(1)}σ).`);
  } else if (zScore >= 2.5 && amt >= avg * 3.0) {
    addRule("Moderate Spending Spike", 30.0,
      `Elevated purchase: $${amt.toFixed(2)} is ${(amt / Math.max(1, avg)).toFixed(1)}x baseline.`);
  }

  // 3. Merchant Risk Category
  const cat = (tx.category || "UNKNOWN").toUpperCase();
  const catPenalty = CATEGORY_RISK[cat] || 10.0;
  if (catPenalty >= 35.0) {
    const adjusted = amt > 1000.0 ? Math.min(85.0, catPenalty + 35.0) : catPenalty;
    addRule("Merchant Category Risk", adjusted,
      `Elevated trade vertical risk: '${cat}' category with purchase of $${amt.toFixed(2)}.`);
  }

  // Calculate Rule Score
  let ruleScore = 0.0;
  if (activeRules > 0) {
    ruleScore = Math.min(100.0, (maxRule * 0.70) + ((ruleSum / activeRules) * 0.30));
  }

  // ML Ensemble Simulation (20 Trees)
  let mlProb = 0.02;
  if (amt > avg * 4.0) mlProb += 0.45;
  if (catPenalty >= 35.0) mlProb += 0.35;
  if (maxRule >= 80.0) mlProb += 0.50;
  mlProb = Math.min(0.99, Math.max(0.01, mlProb));
  const mlScore = mlProb * 100.0;

  // Composite Risk Fusion
  let compositeScore = maxRule >= 85.0
    ? Math.max(ruleScore, 0.60 * ruleScore + 0.40 * mlScore)
    : 0.50 * ruleScore + 0.50 * mlScore;

  compositeScore = Math.max(0.0, Math.min(100.0, compositeScore));

  let riskLevel = "LOW";
  let decision = "APPROVED";
  if (compositeScore >= 70.0) {
    riskLevel = "HIGH";
    decision = "REJECTED";
  } else if (compositeScore >= 35.0) {
    riskLevel = "MEDIUM";
    decision = "FLAGGED (2FA / OTP REQUIRED)";
  }

  // Update profile
  profile.lastTs = Date.now();
  profile.lastLat = tx.latitude;
  profile.lastLon = tx.longitude;

  return {
    transactionId: tx.transactionId || `TX-${Math.floor(10000 + Math.random() * 90000)}`,
    card: tx.cardNumber ? `****-****-****-${tx.cardNumber.replace(/[^0-9]/g, "").slice(-4)}` : "****-****-****-0111",
    amount: tx.amount,
    category: cat,
    riskScore: compositeScore,
    riskLevel: riskLevel,
    decision: decision,
    mlProbability: mlProb,
    ruleScore: ruleScore,
    triggeredRules: triggeredRules,
    isFraud: riskLevel === "HIGH"
  };
}

module.exports = {
  PROFILES,
  evaluateTransaction,
  calculateDistanceKm
};
