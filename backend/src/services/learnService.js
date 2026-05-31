const { supabase } = require('../config/supabase');
const { calculateSM2 } = require('../utils/sm2');

async function getDailyPlan(userId) {
  // 1. Fetch user profile to get words_per_day and target_goal setting
  const { data: profile, error: profileError } = await supabase
    .from('profiles')
    .select('words_per_day, target_goal')
    .eq('id', userId)
    .single();

  const wordsPerDay = (profile && profile.words_per_day) ? profile.words_per_day : 20;
  const targetGoal = (profile && profile.target_goal) ? profile.target_goal : 'IELTS';

  // 2. Fetch decks belonging to the user's target_goal
  const { data: decks, error: decksError } = await supabase
    .from('decks')
    .select(`
      id,
      order_index,
      target_goal,
      deck_progress (
        is_unlocked,
        user_id
      )
    `)
    .eq('target_goal', targetGoal)
    .or(`user_id.is.null,user_id.eq.${userId}`)
    .order('order_index', { ascending: true });

  if (decksError) {
    throw new Error('Failed to retrieve learning plan: ' + decksError.message);
  }

  // Determine unlocked deck IDs
  const unlockedDeckIds = decks
    .filter(deck => {
      const progress = deck.deck_progress
        ? deck.deck_progress.find(p => p.user_id === userId)
        : null;
      return progress ? progress.is_unlocked : (deck.order_index === 1);
    })
    .map(deck => deck.id);

  if (unlockedDeckIds.length === 0) {
    return {
      wordsPerDay,
      newCardsCount: 0,
      reviewCardsCount: 0,
      inSessionReviewCount: 0,
      newCards: [],
      reviewCards: [],
      inSessionReviewCards: []
    };
  }

  // 3. Fetch all cards in the unlocked decks
  const { data: cards, error: cardsError } = await supabase
    .from('cards')
    .select('*')
    .in('deck_id', unlockedDeckIds);

  if (cardsError) {
    throw new Error('Failed to retrieve unlocked deck cards: ' + cardsError.message);
  }

  // 4. Fetch all word progress records for the user
  const { data: progressRecords, error: progressError } = await supabase
    .from('word_progress')
    .select('*')
    .eq('user_id', userId);

  if (progressError) {
    throw new Error('Failed to retrieve word progress: ' + progressError.message);
  }

  // Create a progress lookup map
  const progressMap = new Map();
  progressRecords.forEach(rec => {
    progressMap.set(rec.card_id, rec);
  });

  const allNewCards = [];
  const reviewCards = [];
  const inSessionReviewCards = [];
  const now = new Date();

  // 5. Split cards into New, Due Review, and In-Session Review
  cards.forEach(card => {
    const progress = progressMap.get(card.id);

    if (!progress) {
      allNewCards.push({
        ...card,
        progress: null
      });
    } else {
      const nextReviewDate = new Date(progress.next_review);
      const isDue = nextReviewDate <= now;
      const needsReview = progress.status === 'needs_review';

      if (needsReview && progress.interval === 0) {
        inSessionReviewCards.push({
          ...card,
          progress: {
            status: progress.status,
            ease_factor: parseFloat(progress.ease_factor),
            interval: progress.interval,
            repetitions: progress.repetitions,
            next_review: progress.next_review
          }
        });
      } else if (isDue || needsReview) {
        reviewCards.push({
          ...card,
          progress: {
            status: progress.status,
            ease_factor: parseFloat(progress.ease_factor),
            interval: progress.interval,
            repetitions: progress.repetitions,
            next_review: progress.next_review
          }
        });
      }
    }
  });

  // 6. Limit new cards to user's wordsPerDay setting
  const newCards = allNewCards.slice(0, wordsPerDay);

  return {
    wordsPerDay,
    newCardsCount: newCards.length,
    reviewCardsCount: reviewCards.length,
    inSessionReviewCount: inSessionReviewCards.length,
    totalNewCardsAvailable: allNewCards.length,
    newCards,
    reviewCards,
    inSessionReviewCards
  };
}

async function submitReview(userId, cardId, quality) {
  // 1. Fetch card details to get its deck ID
  const { data: card, error: cardError } = await supabase
    .from('cards')
    .select('id, deck_id, word')
    .eq('id', cardId)
    .single();

  if (cardError || !card) {
    throw new Error('Flashcard not found.');
  }

  const deckId = card.deck_id;

  // 2. Fetch existing word progress
  const { data: prevProgress, error: prevError } = await supabase
    .from('word_progress')
    .select('*')
    .eq('user_id', userId)
    .eq('card_id', cardId)
    .maybeSingle();

  const isFirstTime = !prevProgress;
  const prevEF = prevProgress ? parseFloat(prevProgress.ease_factor) : 2.50;
  const prevInterval = prevProgress ? prevProgress.interval : 0;
  const prevRepetitions = prevProgress ? prevProgress.repetitions : 0;

  // 3. Compute SM-2 update parameters
  const sm2Result = calculateSM2(quality, prevEF, prevInterval, prevRepetitions);

  // 4. Save word progress
  const { data: wordProgress, error: wpSaveError } = await supabase
    .from('word_progress')
    .upsert({
      user_id: userId,
      card_id: cardId,
      status: sm2Result.status,
      ease_factor: sm2Result.ease_factor,
      interval: sm2Result.interval,
      repetitions: sm2Result.repetitions,
      next_review: sm2Result.next_review
    }, { onConflict: 'user_id, card_id' })
    .select()
    .single();

  if (wpSaveError) {
    throw new Error('Failed to update spaced repetition parameters: ' + wpSaveError.message);
  }

  // 5. Update daily activity calendar (study_activity table)
  const todayStr = new Date().toISOString().split('T')[0];
  const { data: activityRecord } = await supabase
    .from('study_activity')
    .select('*')
    .eq('user_id', userId)
    .eq('date', todayStr)
    .maybeSingle();

  if (activityRecord) {
    await supabase
      .from('study_activity')
      .update({ words_count: activityRecord.words_count + 1 })
      .eq('id', activityRecord.id);
  } else {
    await supabase
      .from('study_activity')
      .insert({
        user_id: userId,
        date: todayStr,
        words_count: 1
      });
  }

  // 6. Recalculate deck progress ratio
  const { count: totalCards } = await supabase
    .from('cards')
    .select('*', { count: 'exact', head: true })
    .eq('deck_id', deckId);

  const { data: deckCards } = await supabase
    .from('cards')
    .select('id')
    .eq('deck_id', deckId);

  const deckCardIds = deckCards.map(c => c.id);

  const { count: learnedCards } = await supabase
    .from('word_progress')
    .select('*', { count: 'exact', head: true })
    .eq('user_id', userId)
    .in('card_id', deckCardIds);

  const progressValue = totalCards > 0 ? parseFloat((learnedCards / totalCards).toFixed(2)) : 0.00;

  // Upsert deck progress record
  const { data: dpRecord } = await supabase
    .from('deck_progress')
    .upsert({
      user_id: userId,
      deck_id: deckId,
      progress: progressValue,
      is_unlocked: true
    }, { onConflict: 'user_id, deck_id' })
    .select()
    .single();

  // 7. Map Progression: Auto unlock next deck if this deck is finished (progress = 1.00)
  let nextDeckUnlocked = false;
  let nextDeckName = '';

  if (progressValue >= 1.00) {
    const { data: currentDeck } = await supabase
      .from('decks')
      .select('order_index, target_goal, user_id')
      .eq('id', deckId)
      .single();

    if (currentDeck) {
      const { data: nextDeck } = await supabase
        .from('decks')
        .select('id, name')
        .eq('order_index', currentDeck.order_index + 1)
        .eq('target_goal', currentDeck.target_goal)
        .or(`user_id.is.null,user_id.eq.${userId}`)
        .maybeSingle();

      if (nextDeck) {
        await supabase
          .from('deck_progress')
          .upsert({
            user_id: userId,
            deck_id: nextDeck.id,
            is_unlocked: true
          }, { onConflict: 'user_id, deck_id' });

        nextDeckUnlocked = true;
        nextDeckName = nextDeck.name;
      }
    }
  }

  // 8. Distribute RPG-Gamification rewards: Award XP & update Streak
  let xpGain = 10;
  if (quality === 'again') xpGain = 5;
  else if (isFirstTime) xpGain = 15;

  const { data: profile } = await supabase
    .from('profiles')
    .select('*')
    .eq('id', userId)
    .single();

  let newXp = profile.xp + xpGain;
  let newLevel = Math.floor(newXp / 100) + 1;
  let currentStreak = profile.streak;
  let maxStreak = profile.max_streak;

  const yesterday = new Date();
  yesterday.setDate(yesterday.getDate() - 1);
  const yesterdayStr = yesterday.toISOString().split('T')[0];

  const { data: yesterdayActivity } = await supabase
    .from('study_activity')
    .select('*')
    .eq('user_id', userId)
    .eq('date', yesterdayStr)
    .maybeSingle();

  const { data: todayActivity } = await supabase
    .from('study_activity')
    .select('*')
    .eq('user_id', userId)
    .eq('date', todayStr)
    .maybeSingle();

  const wordsStudiedToday = todayActivity ? todayActivity.words_count : 1;

  if (wordsStudiedToday === 1) {
    if (yesterdayActivity && yesterdayActivity.words_count > 0) {
      currentStreak += 1;
    } else {
      currentStreak = 1;
    }
    maxStreak = Math.max(maxStreak, currentStreak);
  }

  const { count: proficientCount } = await supabase
    .from('word_progress')
    .select('*', { count: 'exact', head: true })
    .eq('user_id', userId)
    .eq('status', 'proficient');

  const { count: reviewedCount } = await supabase
    .from('word_progress')
    .select('*', { count: 'exact', head: true })
    .eq('user_id', userId);

  const retentionRate = reviewedCount > 0 ? Math.round((proficientCount / reviewedCount) * 100) : 0;

  const { data: updatedProfile } = await supabase
    .from('profiles')
    .update({
      xp: newXp,
      level: newLevel,
      streak: currentStreak,
      max_streak: maxStreak,
      retention_rate: retentionRate,
      updated_at: new Date().toISOString()
    })
    .eq('id', userId)
    .select()
    .single();

  // 9. Generate Streak Milestones notifications if streak hit certain counts
  if (wordsStudiedToday === 1 && (currentStreak % 5 === 0)) {
    await supabase
      .from('notifications')
      .insert({
        user_id: userId,
        title: 'Streak Milestone! 🔥',
        content: `Incredible work! You have maintained a ${currentStreak} days learning streak. Keep it going!`,
        type: 'streak_reminder'
      });
  }

  return {
    cardId,
    word: card.word,
    quality,
    sm2: {
      ease_factor: sm2Result.ease_factor,
      interval: sm2Result.interval,
      interval_minutes: sm2Result.interval_minutes,
      repetitions: sm2Result.repetitions,
      status: sm2Result.status,
      next_review: sm2Result.next_review
    },
    rewards: {
      xpGained: xpGain,
      xpTotal: newXp,
      levelUp: newLevel > profile.level,
      level: newLevel,
      streak: currentStreak,
      retentionRate
    },
    deckProgress: {
      deckId,
      progress: progressValue,
      completed: progressValue >= 1.00,
      nextDeckUnlocked,
      nextDeckName
    }
  };
}

async function resetProgress(userId) {
  // 1. Xóa toàn bộ tiến độ từ vựng của người dùng
  const { error: wpError } = await supabase
    .from('word_progress')
    .delete()
    .eq('user_id', userId);

  if (wpError) {
    throw new Error('Failed to reset word progress: ' + wpError.message);
  }

  // 2. Xóa toàn bộ tiến trình của các bộ từ
  const { error: dpError } = await supabase
    .from('deck_progress')
    .delete()
    .eq('user_id', userId);

  if (dpError) {
    throw new Error('Failed to reset deck progress: ' + dpError.message);
  }

  // 3. Xóa lịch sử hoạt động học
  const { error: saError } = await supabase
    .from('study_activity')
    .delete()
    .eq('user_id', userId);

  if (saError) {
    throw new Error('Failed to reset study activity: ' + saError.message);
  }

  // 4. Đặt lại các thông số cấp độ, XP, Streak về giá trị ban đầu trong profile
  const { data: profile, error: profileError } = await supabase
    .from('profiles')
    .update({
      xp: 0,
      level: 1,
      streak: 0,
      max_streak: 0,
      retention_rate: 0,
      updated_at: new Date().toISOString()
    })
    .eq('id', userId)
    .select()
    .single();

  if (profileError) {
    throw new Error('Failed to reset profile stats: ' + profileError.message);
  }

  return profile;
}

module.exports = {
  getDailyPlan,
  submitReview,
  resetProgress
};
