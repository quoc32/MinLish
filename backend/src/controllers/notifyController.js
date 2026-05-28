const { supabase } = require('../config/supabase');

/**
 * Fetch all notifications for the active user
 * GET /api/notifications
 */
async function getNotifications(req, res) {
  try {
    const userId = req.user.id;

    const { data: list, error } = await supabase
      .from('notifications')
      .select('*')
      .eq('user_id', userId)
      .order('created_at', { ascending: false });

    if (error) {
      return res.status(400).json({
        success: false,
        message: 'Failed to fetch notifications.',
        error: error.message
      });
    }

    res.status(200).json({
      success: true,
      data: list
    });
  } catch (err) {
    console.error('Get notifications API error:', err);
    res.status(500).json({
      success: false,
      message: 'Internal server error fetching notifications.'
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

    // Update is_read = true where id = notifyId AND user_id = userId
    const { data: updated, error } = await supabase
      .from('notifications')
      .update({ is_read: true })
      .eq('id', notifyId)
      .eq('user_id', userId) // Security check: Ensure it belongs to this user
      .select()
      .maybeSingle();

    if (error) {
      return res.status(400).json({
        success: false,
        message: 'Failed to update notification.',
        error: error.message
      });
    }

    if (!updated) {
      return res.status(404).json({
        success: false,
        message: 'Notification not found or access denied.'
      });
    }

    res.status(200).json({
      success: true,
      message: 'Notification marked as read.',
      data: updated
    });
  } catch (err) {
    console.error('Mark notification read API error:', err);
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
