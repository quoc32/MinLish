const { supabase } = require('../config/supabase');

async function getNotifications(userId) {
  const { data: list, error } = await supabase
    .from('notifications')
    .select('*')
    .eq('user_id', userId)
    .order('created_at', { ascending: false });

  if (error) {
    throw new Error('Failed to fetch notifications.');
  }

  return list;
}

async function readNotification(userId, notifyId) {
  // Update is_read = true where id = notifyId AND user_id = userId
  const { data: updated, error } = await supabase
    .from('notifications')
    .update({ is_read: true })
    .eq('id', notifyId)
    .eq('user_id', userId) // Security check: Ensure it belongs to this user
    .select()
    .maybeSingle();

  if (error) {
    throw new Error('Failed to update notification: ' + error.message);
  }

  if (!updated) {
    throw new Error('Notification not found or access denied.');
  }

  return updated;
}

module.exports = {
  getNotifications,
  readNotification
};
