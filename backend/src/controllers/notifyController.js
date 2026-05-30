const notifyService = require('../services/notifyService');

/**
 * Fetch all notifications for the active user
 * GET /api/notifications
 */
async function getNotifications(req, res) {
  try {
    const userId = req.user.id;
    const data = await notifyService.getNotifications(userId);

    res.status(200).json({
      success: true,
      data
    });
  } catch (err) {
    console.error('Get notifications API error:', err);
    res.status(err.message.includes('Failed') ? 400 : 500).json({
      success: false,
      message: err.message || 'Internal server error fetching notifications.'
    });
  }
}

/**
 * Mark a notification as read
 * PATCH /api/notifications/:id/read
 */
async function readNotification(req, res) {
  try {
    const userId = req.user.id;
    const notifyId = req.params.id;

    if (!notifyId) {
      return res.status(400).json({
        success: false,
        message: 'Notification ID is required.'
      });
    }

    const updated = await notifyService.readNotification(userId, notifyId);

    res.status(200).json({
      success: true,
      message: 'Notification marked as read.',
      data: updated
    });
  } catch (err) {
    console.error('Mark notification read API error:', err);
    if (err.message.includes('not found')) {
      return res.status(404).json({
        success: false,
        message: err.message
      });
    }
    res.status(500).json({
      success: false,
      message: 'Internal server error updating notification.'
    });
  }
}

module.exports = {
  getNotifications,
  readNotification
};
