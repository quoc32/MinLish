const statService = require('../services/statService');

/**
 * Get unified stats dashboard data for the active user
 * GET /api/stats/dashboard
 */
async function getStatsDashboard(req, res) {
  try {
    const userId = req.user.id;
    const data = await statService.getStatsDashboard(userId);

    res.status(200).json({
      success: true,
      data
    });
  } catch (err) {
    console.error('Stats Dashboard API error:', err);
    if (err.message.includes('not found') || err.message.includes('Failed')) {
      return res.status(404).json({
        success: false,
        message: err.message
      });
    }
    res.status(500).json({
      success: false,
      message: 'Internal server error constructing stats dashboard.'
    });
  }
}

module.exports = {
  getStatsDashboard
};
