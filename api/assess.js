const { evaluateTransaction } = require('./engine');

module.exports = (req, res) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

  if (req.method === 'OPTIONS') {
    return res.status(200).end();
  }

  if (req.method !== 'POST') {
    return res.status(405).json({ error: 'Method Not Allowed' });
  }

  try {
    const body = typeof req.body === 'string' ? JSON.parse(req.body) : (req.body || {});
    const tx = {
      transactionId: body.transactionId || `TX-${Math.floor(10000 + Math.random() * 90000)}`,
      cardNumber: body.cardNumber || "4532-1082-8491-0111",
      amount: parseFloat(body.amount) || 45.0,
      merchant: body.merchant || "Retail Store",
      category: body.category || "ELECTRONICS",
      latitude: parseFloat(body.latitude) || 40.7128,
      longitude: parseFloat(body.longitude) || -74.0060
    };

    const result = evaluateTransaction(tx);
    return res.status(200).json(result);
  } catch (err) {
    return res.status(400).json({ error: err.message });
  }
};
