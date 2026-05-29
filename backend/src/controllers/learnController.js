const { supabase } = require('../config/supabase');
const { calculateSM2 } = require('../utils/sm2');

/**
 * Compile daily learning plan
 * GET /api/learning/daily
 */
async function getDailyPlan(req, res) {
  try {
    const userId = req.user.id;

    // 1. Fetch user profile to get words_per_day and target_goal setting
    const { data: profile, error: profileError } = await supabase
      .from('profiles')
      .select('words_per_day, target_goal')
      .eq('id', userId)
      .single();

    const wordsPerDay = (profile && profile.words_per_day) ? profile.words_per_day : 20;
    const targetGoal = (profile && profile.target_goal) ? profile.target_goal : 'IELTS';

    // 2. Fetch decks belonging to the user's target_goal (both system sample decks and user's custom decks)
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
      return res.status(400).json({
        success: false,
        message: 'Failed to retrieve learning plan.',
        error: decksError.message
      });
    }

    // Determine unlocked deck IDs (explicitly unlocked OR deck #1)
    const unlockedDeckIds = decks
      .filter(deck => {
        const progress = deck.deck_progress
          ? deck.deck_progress.find(p => p.user_id === userId)
          : null;
        return progress ? progress.is_unlocked : (deck.order_index === 1);
      })
      .map(deck => deck.id);

    if (unlockedDeckIds.length === 0) {
      return res.status(200).json({
        success: true,
        data: {
          wordsPerDay,
          newCardsCount: 0,
          reviewCardsCount: 0,
          inSessionReviewCount: 0,
          newCards: [],
          reviewCards: [],
          inSessionReviewCards: []
        }
      });
    }

    // 3. Fetch all cards in the unlocked decks
    const { data: cards, error: cardsError } = await supabase
      .from('cards')
      .select('*')
      .in('deck_id', unlockedDeckIds);

    if (cardsError) {
      return res.status(400).json({
        success: false,
        message: 'Failed to retrieve unlocked deck cards.',
        error: cardsError.message
      });
    }

    // 4. Fetch all word progress records for the user
    const { data: progressRecords, error: progressError } = await supabase
      .from('word_progress')
      .select('*')
      .eq('user_id', userId);

    if (progressError) {
      return res.status(400).json({
        success: false,
        message: 'Failed to retrieve word progress.',
        error: progressError.message
      });
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
        // User has never reviewed this card before
        allNewCards.push({
          ...card,
          progress: null
        });
      } else {
        const nextReviewDate = new Date(progress.next_review);
        const isDue = nextReviewDate <= now;
        const needsReview = progress.status === 'needs_review';

        if (needsReview && progress.interval === 0) {
          // Intra-session review: cards marked "again" with short interval
          // These should be reviewed within the current study session
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
          // Regular due review: cards whose next_review date has passed
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

    res.status(200).json({
      success: true,
      data: {
        wordsPerDay,
        newCardsCount: newCards.length,
        reviewCardsCount: reviewCards.length,
        inSessionReviewCount: inSessionReviewCards.length,
        totalNewCardsAvailable: allNewCards.length,
        newCards,
        reviewCards,
        inSessionReviewCards
      }
    });
  } catch (err) {
    console.error('Get daily learning plan error:', err);
    res.status(500).json({
      success: false,
      message: 'Internal server error compiling daily learning plan.'
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

    // 1. Fetch card details to get its deck ID
    const { data: card, error: cardError } = await supabase
      .from('cards')
      .select('id, deck_id, word')
      .eq('id', cardId)
      .single();

    if (cardError || !card) {
      return res.status(444).json({
        success: false,
        message: 'Flashcard not found.'
      });
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
      console.error('Word progress save error:', wpSaveError);
      return res.status(400).json({
        success: false,
        message: 'Failed to update spaced repetition parameters.',
        error: wpSaveError.message
      });
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
    // Total count of cards in the deck
    const { count: totalCards } = await supabase
      .from('cards')
      .select('*', { count: 'exact', head: true })
      .eq('deck_id', deckId);

    // Count of cards inside this deck that have a word progress entry for this user
    // (Meaning they have started studying it)
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
      // Find current deck's order_index and target goal info
      const { data: currentDeck } = await supabase
        .from('decks')
        .select('order_index, target_goal, user_id')
        .eq('id', deckId)
        .single();

      if (currentDeck) {
        // Query next deck in sequence, matching target goal and ownership
        const { data: nextDeck } = await supabase
          .from('decks')
          .select('id, name')
          .eq('order_index', currentDeck.order_index + 1)
          .eq('target_goal', currentDeck.target_goal)
          .or(`user_id.is.null,user_id.eq.${userId}`)
          .maybeSingle();

        if (nextDeck) {
          // Upsert next deck to unlocked = true
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
    // Rewards structure:
    // - Incorrect answer: +5 XP
    // - First review (new card): +15 XP
    // - Regular review: +10 XP
    let xpGain = 10;
    if (quality === 'again') {
      xpGain = 5;
    } else if (isFirstTime) {
      xpGain = 15;
    }

    // Fetch user profile
    const { data: profile } = await supabase
      .from('profiles')
      .select('*')
      .eq('id', userId)
      .single();

    let newXp = profile.xp + xpGain;
    let newLevel = Math.floor(newXp / 100) + 1; // RPG progression: 100 XP per level
    let currentStreak = profile.streak;
    let maxStreak = profile.max_streak;

    // Check streak: Did they study yesterday?
    // Get yesterday date string
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

    // Update streak logic
    if (wordsStudiedToday === 1) {
      // First card studied today!
      if (yesterdayActivity && yesterdayActivity.words_count > 0) {
        // Kept the streak alive!
        currentStreak += 1;
      } else {
        // Streak broken or just starting
        currentStreak = 1;
      }
      maxStreak = Math.max(maxStreak, currentStreak);
    }

    // Calculate dynamic retention rate
    // Retention rate = (proficient cards / total cards reviewed) * 100
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

    // Update profile
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

    res.status(200).json({
      success: true,
      message: 'Review recorded successfully.',
      data: {
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
      }
    });
  } catch (err) {
    console.error('Submit card review error:', err);
    res.status(500).json({
      success: false,
      message: 'Internal server error recording review.'
    });
  }
}

module.exports = {
  getDailyPlan,
  submitReview
};
