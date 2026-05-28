const { supabase } = require('../config/supabase');

/**
 * Get all decks with the active user's progress and lock status
 * GET /api/decks
 */
async function getAllDecks(req, res) {
  try {
    const userId = req.user.id;

    // Fetch decks and their progress rows for this user
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
        deck_progress (
          progress,
          is_unlocked,
          user_id
        )
      `)
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
      .select('id, name')
      .eq('id', deckId)
      .single();

    if (deckError || !deck) {
      return res.status(404).json({
        success: false,
        message: 'Deck not found.'
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
  getDeckCards
};
