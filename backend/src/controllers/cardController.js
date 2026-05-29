const { supabase } = require('../config/supabase');

/**
 * Create a new card in a deck
 * POST /api/cards
 */
async function createCard(req, res) {
  try {
    const { deckId, word, pronunciation, meaning, descriptionEn, example } = req.body;

    if (!deckId || !word || !meaning) {
      return res.status(400).json({
        success: false,
        message: 'deckId, word, and meaning are required.'
      });
    }

    // 1. Verify deck exists
    const { data: deck, error: deckError } = await supabase
      .from('decks')
      .select('id, total_words')
      .eq('id', deckId)
      .single();

    if (deckError || !deck) {
      return res.status(404).json({
        success: false,
        message: 'Deck not found.'
      });
    }

    // 2. Insert the new card
    const { data: newCard, error: insertError } = await supabase
      .from('cards')
      .insert({
        deck_id: deckId,
        word: word.trim(),
        pronunciation: pronunciation || '',
        meaning: meaning.trim(),
        description_en: descriptionEn || '',
        example: example || ''
      })
      .select()
      .single();

    if (insertError) {
      return res.status(400).json({
        success: false,
        message: 'Failed to create card.',
        error: insertError.message
      });
    }

    // 3. Update total_words count in the deck
    await supabase
      .from('decks')
      .update({ total_words: (deck.total_words || 0) + 1 })
      .eq('id', deckId);

    res.status(201).json({
      success: true,
      message: 'Card created successfully.',
      data: newCard
    });
  } catch (err) {
    console.error('Create card API error:', err);
    res.status(500).json({
      success: false,
      message: 'Internal server error creating card.'
    });
  }
}

/**
 * Update an existing card
 * PUT /api/cards/:id
 */
async function updateCard(req, res) {
  try {
    const cardId = req.params.id;
    const { word, pronunciation, meaning, descriptionEn, example } = req.body;

    if (!cardId) {
      return res.status(400).json({
        success: false,
        message: 'Card ID is required.'
      });
    }

    // Build update payload dynamically
    const updates = {};
    if (word !== undefined) updates.word = word.trim();
    if (pronunciation !== undefined) updates.pronunciation = pronunciation;
    if (meaning !== undefined) updates.meaning = meaning.trim();
    if (descriptionEn !== undefined) updates.description_en = descriptionEn;
    if (example !== undefined) updates.example = example;

    if (Object.keys(updates).length === 0) {
      return res.status(400).json({
        success: false,
        message: 'At least one field is required to update.'
      });
    }

    const { data: updatedCard, error } = await supabase
      .from('cards')
      .update(updates)
      .eq('id', cardId)
      .select()
      .single();

    if (error) {
      return res.status(400).json({
        success: false,
        message: 'Failed to update card.',
        error: error.message
      });
    }

    res.status(200).json({
      success: true,
      message: 'Card updated successfully.',
      data: updatedCard
    });
  } catch (err) {
    console.error('Update card API error:', err);
    res.status(500).json({
      success: false,
      message: 'Internal server error updating card.'
    });
  }
}

/**
 * Delete a card and related data
 * DELETE /api/cards/:id
 */
async function deleteCard(req, res) {
  try {
    const cardId = req.params.id;

    if (!cardId) {
      return res.status(400).json({
        success: false,
        message: 'Card ID is required.'
      });
    }

    // 1. Fetch card to get its deck_id
    const { data: card, error: cardError } = await supabase
      .from('cards')
      .select('id, deck_id')
      .eq('id', cardId)
      .single();

    if (cardError || !card) {
      return res.status(404).json({
        success: false,
        message: 'Card not found.'
      });
    }

    // 2. Cascade delete: word_progress for this card
    await supabase
      .from('word_progress')
      .delete()
      .eq('card_id', cardId);

    // 3. Delete the card
    const { error: deleteError } = await supabase
      .from('cards')
      .delete()
      .eq('id', cardId);

    if (deleteError) {
      return res.status(400).json({
        success: false,
        message: 'Failed to delete card.',
        error: deleteError.message
      });
    }

    // 4. Update total_words count in the deck
    const { count: remainingCards } = await supabase
      .from('cards')
      .select('*', { count: 'exact', head: true })
      .eq('deck_id', card.deck_id);

    await supabase
      .from('decks')
      .update({ total_words: remainingCards || 0 })
      .eq('id', card.deck_id);

    res.status(200).json({
      success: true,
      message: 'Card deleted successfully.'
    });
  } catch (err) {
    console.error('Delete card API error:', err);
    res.status(500).json({
      success: false,
      message: 'Internal server error deleting card.'
    });
  }
}

module.exports = {
  createCard,
  updateCard,
  deleteCard
};
