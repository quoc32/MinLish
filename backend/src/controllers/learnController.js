const learnService = require('../services/learnService');

/**
 * Compile daily learning plan
 * GET /api/learning/daily
 */
async function getDailyPlan(req, res) {
  try {
    const userId = req.user.id;
    const data = await learnService.getDailyPlan(userId);

    res.status(200).json({
      success: true,
      data
    });
  } catch (err) {
    console.error('Get daily learning plan error:', err);
    res.status(err.message.includes('Failed') ? 400 : 500).json({
      success: false,
      message: err.message || 'Internal server error compiling daily learning plan.'
    });
  }
}

/**
 * Handle a flashcard review submittal (applies SM-2 algorithm)
 * POST /api/learning/review
 */
async function submitReview(req, res) {
  try {
    const userId = req.user.id;
    const { cardId, quality } = req.body; // quality: 'again', 'hard', 'good', 'easy'

    if (!cardId || !quality) {
      return res.status(400).json({
        success: false,
        message: 'cardId and quality are required.'
      });
    }

    if (!['again', 'hard', 'good', 'easy'].includes(quality)) {
      return res.status(400).json({
        success: false,
        message: 'Invalid quality. Must be one of: again, hard, good, easy'
      });
    }

    const data = await learnService.submitReview(userId, cardId, quality);

    res.status(200).json({
      success: true,
      message: 'Review recorded successfully.',
      data
    });
  } catch (err) {
    console.error('Submit card review error:', err);
    if (err.message === 'Flashcard not found.') {
      return res.status(444).json({
        success: false,
        message: err.message
      });
    }
    res.status(500).json({
      success: false,
      message: err.message || 'Internal server error recording review.'
    });
  }
}

async function resetProgress(req, res) {
  try {
    const userId = req.user.id;
    const profile = await learnService.resetProgress(userId);

    res.status(200).json({
      success: true,
      message: 'All learning progress has been reset successfully.',
      data: profile
    });
  } catch (err) {
    console.error('Reset learning progress error:', err);
    res.status(500).json({
      success: false,
      message: err.message || 'Internal server error resetting progress.'
    });
  }
}

module.exports = {
  getDailyPlan,
  submitReview,
  resetProgress
};
