const { supabase } = require('../config/supabase');

/**
 * Get unified stats dashboard data for the active user
 * GET /api/stats/dashboard
 */
async function getStatsDashboard(req, res) {
  try {
    const userId = req.user.id;

    // 1. Fetch profile metrics
    const { data: profile, error: profileError } = await supabase
      .from('profiles')
      .select('xp, level, streak, max_streak, target_goal, retention_rate')
      .eq('id', userId)
      .single();

    if (profileError || !profile) {
      return res.status(404).json({
        success: false,
        message: 'Profile metrics not found.',
        error: profileError ? profileError.message : 'Not found'
      });
    }

    // 2. Fetch vocabulary status breakdown (Donut Chart)
    const { data: wordProgressList, error: progressError } = await supabase
      .from('word_progress')
      .select('status')
      .eq('user_id', userId);

    if (progressError) {
      return res.status(400).json({
        success: false,
        message: 'Failed to fetch card statistics.',
        error: progressError.message
      });
    }

    // Tally statuses
    let learningCount = 0;
    let proficientCount = 0;
    let needsReviewCount = 0;

    wordProgressList.forEach(item => {
      if (item.status === 'learning') learningCount++;
      else if (item.status === 'proficient') proficientCount++;
      else if (item.status === 'needs_review') needsReviewCount++;
    });

    const donutChart = {
      learning: learningCount,
      proficient: proficientCount,
      needs_review: needsReviewCount,
      total_studied: wordProgressList.length
    };

    // 3. Fetch past 7 days study activity (Bar Chart)
    const { data: activityData, error: activityError } = await supabase
      .from('study_activity')
      .select('date, words_count')
      .eq('user_id', userId)
      .order('date', { ascending: false })
      .limit(15); // Fetch extra records to make sure we don't miss past week overlaps

    if (activityError) {
      return res.status(400).json({
        success: false,
        message: 'Failed to fetch activity records.',
        error: activityError.message
      });
    }

    // Map activity data to date strings for quick retrieval
    const activityMap = new Map();
    if (activityData) {
      activityData.forEach(act => {
        activityMap.set(act.date, act.words_count);
      });
    }

    // Generate consecutive last 7 calendar days list
    const barChart = [];
    for (let i = 6; i >= 0; i--) {
      const d = new Date();
      d.setDate(d.getDate() - i);
      const dateStr = d.toISOString().split('T')[0];

      // Format clean day labels for Kotlin frontend, e.g. "28/05"
      const day = String(d.getDate()).padStart(2, '0');
      const month = String(d.getMonth() + 1).padStart(2, '0');
      const label = `${day}/${month}`;

      barChart.push({
        date: dateStr,
        label: label,
        words_count: activityMap.get(dateStr) || 0
      });
    }

    res.status(200).json({
      success: true,
      data: {
        profile,
        donutChart,
        barChart
      }
    });
  } catch (err) {
    console.error('Stats Dashboard API error:', err);
    res.status(500).json({
      success: false,
      message: 'Internal server error constructing stats dashboard.'
    });
  }
}

module.exports = {
  getStatsDashboard
};
