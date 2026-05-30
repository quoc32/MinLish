const { supabase } = require('../config/supabase');

async function getAllDecks(userId) {
  // Fetch user's target goal
  const { data: profile } = await supabase
    .from('profiles')
    .select('target_goal')
    .eq('id', userId)
    .single();

  const targetGoal = profile ? profile.target_goal : 'IELTS';

  // Fetch decks for this specific goal (both system sample decks and user's custom decks)
  const { data: decks, error } = await supabase
    .from('decks')
    .select(`
      id,
      name,
      icon,
      tag,
      total_words,
      order_index,
      created_at,
      target_goal,
      user_id,
      deck_progress (
        progress,
        is_unlocked,
        user_id
      )
    `)
    .eq('target_goal', targetGoal)
    .or(`user_id.is.null,user_id.eq.${userId}`)
    .order('order_index', { ascending: true });

  if (error) {
    throw new Error('Failed to fetch decks: ' + error.message);
  }

  // Format the response: match the user's specific progress or return default values
  return decks.map(deck => {
    // Find progress entry for the current user (if any)
    const userProgress = deck.deck_progress
      ? deck.deck_progress.find(p => p.user_id === userId)
      : null;

    return {
      id: deck.id,
      name: deck.name,
      icon: deck.icon,
      tag: deck.tag,
      total_words: deck.total_words,
      order_index: deck.order_index,
      created_at: deck.created_at,
      progress: userProgress ? parseFloat(userProgress.progress) : 0.00,
      is_unlocked: userProgress 
        ? userProgress.is_unlocked 
        : (deck.order_index === 1) // First deck node is unlocked by default
    };
  });
}

async function getDeckCards(deckId, userId) {
  // 1. Verify deck exists
  const { data: deck, error: deckError } = await supabase
    .from('decks')
    .select('id, name, user_id')
    .eq('id', deckId)
    .single();

  if (deckError || !deck) {
    throw new Error('Deck not found.');
  }

  // Check ownership of custom deck
  if (deck.user_id && deck.user_id !== userId) {
    throw new Error('Access denied. This deck belongs to another user.');
  }

  // 2. Fetch cards inside this deck, along with word progress for the user
  const { data: cards, error: cardsError } = await supabase
    .from('cards')
    .select(`
      id,
      deck_id,
      word,
      pronunciation,
      meaning,
      description_en,
      example,
      word_type,
      collocation,
      related_words,
      note,
      word_progress (
        status,
        ease_factor,
        interval,
        repetitions,
        next_review,
        user_id
      )
    `)
    .eq('deck_id', deckId);

  if (cardsError) {
    throw new Error('Failed to fetch cards: ' + cardsError.message);
  }

  // Format the cards: attach user's word progress details
  const formattedCards = cards.map(card => {
    const userWordProgress = card.word_progress
      ? card.word_progress.find(wp => wp.user_id === userId)
      : null;

    return {
      id: card.id,
      deck_id: card.deck_id,
      word: card.word,
      pronunciation: card.pronunciation,
      meaning: card.meaning,
      description_en: card.description_en,
      example: card.example,
      word_type: card.word_type,
      collocation: card.collocation,
      related_words: card.related_words,
      note: card.note,
      progress: userWordProgress ? {
        status: userWordProgress.status,
        ease_factor: parseFloat(userWordProgress.ease_factor),
        interval: userWordProgress.interval,
        repetitions: userWordProgress.repetitions,
        next_review: userWordProgress.next_review
      } : null
    };
  });

  return { deckName: deck.name, cards: formattedCards };
}

async function createDeck(userId, { name, icon, tag }) {
  // 1. Fetch user's target goal
  const { data: profile } = await supabase
    .from('profiles')
    .select('target_goal')
    .eq('id', userId)
    .single();

  const targetGoal = profile ? profile.target_goal : 'IELTS';

  // 2. Determine the next order_index within this goal
  const { data: lastDeck } = await supabase
    .from('decks')
    .select('order_index')
    .eq('target_goal', targetGoal)
    .order('order_index', { ascending: false })
    .limit(1)
    .maybeSingle();

  const nextOrderIndex = lastDeck ? lastDeck.order_index + 1 : 1;

  // 3. Insert the new deck with target_goal and owner user_id
  const { data: newDeck, error: insertError } = await supabase
    .from('decks')
    .insert({
      name: name.trim(),
      icon: icon || '📚',
      tag: tag || null,
      total_words: 0,
      order_index: nextOrderIndex,
      target_goal: targetGoal,
      user_id: userId
    })
    .select()
    .single();

  if (insertError) {
    throw new Error('Failed to create deck: ' + insertError.message);
  }

  // 3. Auto-create deck_progress for the creating user (unlocked)
  await supabase
    .from('deck_progress')
    .insert({
      user_id: userId,
      deck_id: newDeck.id,
      progress: 0.00,
      is_unlocked: true
    });

  return {
    ...newDeck,
    progress: 0.00,
    is_unlocked: true
  };
}

async function updateDeck(deckId, userId, { name, icon, tag }) {
  // 1. Verify ownership
  const { data: deck, error: fetchError } = await supabase
    .from('decks')
    .select('user_id')
    .eq('id', deckId)
    .single();

  if (fetchError || !deck) {
    throw new Error('Deck not found.');
  }

  if (deck.user_id !== userId) {
    throw new Error('Access denied. You do not own this deck.');
  }

  // 2. Build update payload dynamically
  const updates = {};
  if (name !== undefined) updates.name = name.trim();
  if (icon !== undefined) updates.icon = icon;
  if (tag !== undefined) updates.tag = tag;

  if (Object.keys(updates).length === 0) {
    throw new Error('At least one field (name, icon, tag) is required to update.');
  }

  const { data: updatedDeck, error } = await supabase
    .from('decks')
    .update(updates)
    .eq('id', deckId)
    .select()
    .single();

  if (error) {
    throw new Error('Failed to update deck: ' + error.message);
  }

  return updatedDeck;
}

async function deleteDeck(deckId, userId) {
  // 1. Verify ownership
  const { data: deck, error: fetchError } = await supabase
    .from('decks')
    .select('user_id')
    .eq('id', deckId)
    .single();

  if (fetchError || !deck) {
    throw new Error('Deck not found.');
  }

  if (deck.user_id !== userId) {
    throw new Error('Access denied. You do not own this deck.');
  }

  // 2. Fetch all card IDs in this deck (needed for cascade cleanup)
  const { data: deckCards } = await supabase
    .from('cards')
    .select('id')
    .eq('deck_id', deckId);

  const cardIds = deckCards ? deckCards.map(c => c.id) : [];

  // 2. Cascade delete: word_progress for these cards
  if (cardIds.length > 0) {
    await supabase
      .from('word_progress')
      .delete()
      .in('card_id', cardIds);
  }

  // 3. Cascade delete: all cards in this deck
  await supabase
    .from('cards')
    .delete()
    .eq('deck_id', deckId);

  // 4. Cascade delete: deck_progress for all users
  await supabase
    .from('deck_progress')
    .delete()
    .eq('deck_id', deckId);

  // 5. Delete the deck itself
  const { error: deleteError } = await supabase
    .from('decks')
    .delete()
    .eq('id', deckId);

  if (deleteError) {
    throw new Error('Failed to delete deck: ' + deleteError.message);
  }
}

module.exports = {
  getAllDecks,
  getDeckCards,
  createDeck,
  updateDeck,
  deleteDeck
};
