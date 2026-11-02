const { PROFILES } = require('./engine');

module.exports = (req, res) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Content-Type', 'application/json');

  const list = Object.values(PROFILES).map(p => ({
    card: p.card,
    avgAmount: p.avgAmount,
    stdDev: p.stdDev,
    homeLat: p.homeLat,
    homeLon: p.homeLon
  }));

  return res.status(200).json(list);
};
