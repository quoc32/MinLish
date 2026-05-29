const { supabase } = require('../config/supabase');

/**
 * Get all decks with the active user's progress and lock status
 * GET /api/decks
 */
async function getAllDecks(req, res) {
  try {
    const userId = req.user.id;

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
      return res.status(400).json({
        success: false,
        message: 'Failed to fetch decks.',
        error: error.message
      });
    }

    // Format the response: match the user's specific progress or return default values
    const formattedDecks = decks.map(deck => {
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

    res.status(200).json({
      success: true,
      data: formattedDecks
    });
  } catch (err) {
    console.error('Get all decks API error:', err);
    res.status(500).json({
      success: false,
      message: 'Internal server error fetching decks.'
    });
  }
}

/**
 * Get all cards in a deck, along with the user's word progress
 * GET /api/decks/:id/cards
 */
async function getDeckCards(req, res) {
  try {
    const deckId = req.params.id;
    const userId = req.user.id;

    if (!deckId) {
      return res.status(400).json({
        success: false,
        message: 'Deck ID is required.'
      });
    }

    // 1. Verify deck exists
    const { data: deck, error: deckError } = await supabase
      .from('decks')
      .select('id, name, user_id')
      .eq('id', deckId)
      .single();

    if (deckError || !deck) {
      return res.status(404).json({
        success: false,
        message: 'Deck not found.'
      });
    }

    // Check ownership of custom deck
    if (deck.user_id && deck.user_id !== userId) {
      return res.status(403).json({
        success: false,
        message: 'Access denied. This deck belongs to another user.'
      });
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
      return res.status(400).json({
        success: false,
        message: 'Failed to fetch cards.',
        error: cardsError.message
      });
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
        progress: userWordProgress ? {
          status: userWordProgress.status,
          ease_factor: parseFloat(userWordProgress.ease_factor),
          interval: userWordProgress.interval,
          repetitions: userWordProgress.repetitions,
          next_review: userWordProgress.next_review
        } : null
      };
    });

    res.status(200).json({
      success: true,
      deckName: deck.name,
      data: formattedCards
    });
  } catch (err) {
    console.error('Get deck cards API error:', err);
    res.status(500).json({
      success: false,
      message: 'Internal server error fetching deck cards.'
    });
  }
}

module.exports = {
  getAllDecks,
  getDeckCards,
  createDeck,
  updateDeck,
  deleteDeck
};

/**
 * Create a new deck
 * POST /api/decks
 */
async function createDeck(req, res) {
  try {
    const userId = req.user.id;
    const { name, icon, tag } = req.body;

    if (!name || !name.trim()) {
      return res.status(400).json({
        success: false,
        message: 'Deck name is required.'
      });
    }

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
      return res.status(400).json({
        success: false,
        message: 'Failed to create deck.',
        error: insertError.message
      });
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

    res.status(201).json({
      success: true,
      message: 'Deck created successfully.',
      data: {
        ...newDeck,
        progress: 0.00,
        is_unlocked: true
      }
    });
  } catch (err) {
    console.error('Create deck API error:', err);
    res.status(500).json({
      success: false,
      message: 'Internal server error creating deck.'
    });
  }
}

/**
 * Update an existing deck
 * PUT /api/decks/:id
 */
async function updateDeck(req, res) {
  try {
    const deckId = req.params.id;
    const { name, icon, tag } = req.body;

    if (!deckId) {
      return res.status(400).json({
        success: false,
        message: 'Deck ID is required.'
      });
    }

    // Build update payload dynamically
    const updates = {};
    if (name !== undefined) updates.name = name.trim();
    if (icon !== undefined) updates.icon = icon;
    if (tag !== undefined) updates.tag = tag;

    if (Object.keys(updates).length === 0) {
      return res.status(400).json({
        success: false,
        message: 'At least one field (name, icon, tag) is required to update.'
      });
    }

    const { data: updatedDeck, error } = await supabase
      .from('decks')
      .update(updates)
      .eq('id', deckId)
      .select()
      .single();

    if (error) {
      return res.status(400).json({
        success: false,
        message: 'Failed to update deck.',
        error: error.message
      });
    }

    res.status(200).json({
      success: true,
      message: 'Deck updated successfully.',
      data: updatedDeck
    });
  } catch (err) {
    console.error('Update deck API error:', err);
    res.status(500).json({
      success: false,
      message: 'Internal server error updating deck.'
    });
  }
}

/**
 * Delete a deck and all related data (cascade)
 * DELETE /api/decks/:id
 */
async function deleteDeck(req, res) {
  try {
    const deckId = req.params.id;

    if (!deckId) {
      return res.status(400).json({
        success: false,
        message: 'Deck ID is required.'
      });
    }

    // 1. Fetch all card IDs in this deck (needed for cascade cleanup)
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
      return res.status(400).json({
        success: false,
        message: 'Failed to delete deck.',
        error: deleteError.message
      });
    }

    res.status(200).json({
      success: true,
      message: 'Deck and all related data deleted successfully.'
    });
  } catch (err) {
    console.error('Delete deck API error:', err);
    res.status(500).json({
      success: false,
      message: 'Internal server error deleting deck.'
    });
  }
}
