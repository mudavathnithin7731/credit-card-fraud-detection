const { evaluateTransaction, PROFILES } = require('./engine');

module.exports = (req, res) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Content-Type', 'application/json');

  const cards = Object.keys(PROFILES);
  const categories = ["GROCERY", "DINING", "RETAIL", "ELECTRONICS", "UTILITIES"];
  const list = [];

  for (let i = 0; i < 20; i++) {
    const card = cards[i % cards.length];
    const prof = PROFILES[card];
    const isAttacker = i === 7 || i === 15;

    let tx;
    if (!isAttacker) {
      tx = {
        transactionId: `TX-${10000 + i}`,
        cardNumber: card,
        amount: +(prof.avgAmount + (Math.random() * 20 - 10)).toFixed(2),
        category: categories[Math.floor(Math.random() * categories.length)],
        latitude: +(prof.homeLat + (Math.random() * 0.05 - 0.025)).toFixed(4),
        longitude: +(prof.homeLon + (Math.random() * 0.05 - 0.025)).toFixed(4)
      };
    } else {
      tx = {
        transactionId: `TX-FRD-${10000 + i}`,
        cardNumber: card,
        amount: i === 7 ? 1450.00 : 4200.00,
        category: i === 7 ? "ELECTRONICS" : "CRYPTO",
        latitude: i === 7 ? 35.6762 : prof.homeLat,
        longitude: i === 7 ? 139.6503 : prof.homeLon
      };
    }

    const evalResult = evaluateTransaction(tx);
    const reason = evalResult.triggeredRules.length > 0 ? evalResult.triggeredRules[0] : "Normal Profile";
    list.push({
      id: tx.transactionId,
      card: evalResult.card,
      amount: tx.amount,
      category: tx.category,
      score: evalResult.riskScore,
      decision: evalResult.decision,
      level: evalResult.riskLevel,
      reason: reason,
      isFraud: evalResult.isFraud
    });
  }

  return res.status(200).json(list);
};
