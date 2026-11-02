const { evaluateTransaction, PROFILES } = require('./engine');

module.exports = (req, res) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Content-Type', 'application/json');

  const card = Object.keys(PROFILES)[0];
  const prof = PROFILES[card];

  const attacks = [
    { transactionId: "ATK-101", cardNumber: card, amount: 0.50, category: "ELECTRONICS", latitude: prof.homeLat, longitude: prof.homeLon },
    { transactionId: "ATK-102", cardNumber: card, amount: 0.75, category: "ELECTRONICS", latitude: prof.homeLat, longitude: prof.homeLon },
    { transactionId: "ATK-103", cardNumber: card, amount: 1850.00, category: "CRYPTO", latitude: prof.homeLat, longitude: prof.homeLon },
    { transactionId: "ATK-104", cardNumber: card, amount: 3200.00, category: "GAMBLING", latitude: 35.6762, longitude: 139.6503 },
    { transactionId: "ATK-105", cardNumber: card, amount: 7500.00, category: "WIRE_TRANSFER", latitude: 51.5074, longitude: -0.1278 }
  ];

  const results = attacks.map(tx => {
    const r = evaluateTransaction(tx);
    const reason = r.triggeredRules.length > 0 ? r.triggeredRules[0] : "ML Spike";
    return {
      id: tx.transactionId,
      card: r.card,
      amount: tx.amount,
      category: tx.category,
      score: r.riskScore,
      decision: r.decision,
      level: r.riskLevel,
      reason: reason,
      isFraud: true
    };
  });

  return res.status(200).json(results);
};
