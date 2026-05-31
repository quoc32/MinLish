const { supabase } = require('../config/supabase');

async function getStatsDashboard(userId) {
  // 1. Fetch profile metrics
  const { data: profile, error: profileError } = await supabase
    .from('profiles')
    .select('xp, level, streak, max_streak, target_goal, retention_rate')
    .eq('id', userId)
    .single();

  if (profileError || !profile) {
    throw new Error('Profile metrics not found.');
  }

  // 2. Fetch vocabulary status breakdown (Donut Chart)
  const { data: wordProgressList, error: progressError } = await supabase
    .from('word_progress')
    .select('status')
    .eq('user_id', userId);

  if (progressError) {
    throw new Error('Failed to fetch card statistics.');
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
    throw new Error('Failed to fetch activity records.');
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

  return {
    profile,
    donutChart,
    barChart
  };
}

module.exports = {
  getStatsDashboard
};
