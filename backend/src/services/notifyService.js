const { supabase } = require('../config/supabase');
const { sendEmail } = require('../utils/mailer');
const { getAllUserEmailsLocal } = require('../utils/userEmailCache');

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
  const { data: updated, error } = await supabase
    .from('notifications')
    .update({ is_read: true })
    .eq('id', notifyId)
    .eq('user_id', userId)
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

async function sendDailyStudyEmailReminders() {
  // 1. Lấy tất cả thông tin hồ sơ người dùng
  const { data: profiles, error } = await supabase
    .from('profiles')
    .select('id, display_name, target_goal, level, xp, streak');

  if (error) {
    throw new Error('Failed to retrieve profiles for email reminders: ' + error.message);
  }

  // 2. Lấy ánh xạ email đã cache
  const emailsMap = getAllUserEmailsLocal();
  const todayStr = new Date().toISOString().split('T')[0];
  const results = [];

  // 3. Quét kiểm tra hoạt động học tập
  for (const profile of profiles) {
    const userId = profile.id;
    const email = emailsMap[userId];

    if (!email) {
      console.log(`Bỏ qua email nhắc học cho ${profile.display_name} (ID: ${userId}) - chưa lưu email local.`);
      continue;
    }

    // Kiểm tra hoạt động học hôm nay
    const { data: activity, error: activityError } = await supabase
      .from('study_activity')
      .select('*')
      .eq('user_id', userId)
      .eq('date', todayStr)
      .maybeSingle();

    if (activityError) {
      console.error(`Lỗi kiểm tra hoạt động của email ${email}:`, activityError.message);
      continue;
    }

    // Nếu chưa học hoặc học 0 từ -> tiến hành gửi email nhắc nhở
    if (!activity || activity.words_count === 0) {
      const subject = `🔥 Giữ vững chuỗi học tập MinLish của bạn!`;
      const html = `
        <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #e2e8f0; border-radius: 16px;">
          <h2 style="color: #4f46e5; text-align: center;">Đã đến giờ học tiếng Anh rồi, ${profile.display_name}! 🚀</h2>
          <p>Chào bạn,</p>
          <p>Hôm nay bạn chưa hoàn thành chỉ tiêu học từ vựng hàng ngày của mình trên <b>MinLish</b>.</p>
          <div style="background-color: #f8fafc; border-radius: 12px; padding: 16px; margin: 20px 0;">
            <p style="margin: 4px 0;"><b>Chuỗi Streak hiện tại:</b> ${profile.streak} ngày 🔥</p>
            <p style="margin: 4px 0;"><b>Cấp độ hiện tại:</b> Cấp ${profile.level} (XP: ${profile.xp}) ⭐</p>
            <p style="margin: 4px 0;"><b>Mục tiêu học:</b> ${profile.target_goal || 'IELTS'}</p>
          </div>
          <p>Dành ra chỉ 5 phút hôm nay để giữ vững chuỗi Streak của bạn và củng cố kiến thức nhé. Đừng để ngắt quãng tiến độ!</p>
          <div style="text-align: center; margin-top: 30px;">
            <a href="minlish://dashboard" style="background-color: #4f46e5; color: white; padding: 12px 24px; text-decoration: none; border-radius: 8px; font-weight: bold; display: inline-block;">VÀO HỌC NGAY</a>
          </div>
          <hr style="margin-top: 40px; border: 0.5px solid #f1f5f9;" />
          <p style="font-size: 11px; color: #64748b; text-align: center;">MinLish App • Ứng dụng học từ vựng hiệu quả bằng lặp lại ngắt quãng (Spaced Repetition)</p>
        </div>
      `;

      console.log(`Gửi email nhắc nhở học cho ${email}...`);
      const sendResult = await sendEmail({
        to: email,
        subject,
        html,
        text: `Chào ${profile.display_name}, hôm nay bạn chưa học từ vựng trên MinLish. Hãy vào học để duy trì chuỗi Streak ${profile.streak} ngày của mình nhé!`
      });

      results.push({
        userId,
        email,
        displayName: profile.display_name,
        sent: sendResult.success,
        loggedLocal: sendResult.loggedLocal || false
      });
    }
  }

  return results;
}

module.exports = {
  getNotifications,
  readNotification,
  sendDailyStudyEmailReminders
};
