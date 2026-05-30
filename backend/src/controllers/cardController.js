const cardService = require('../services/cardService');

/**
 * Create a new card in a deck
 * POST /api/cards
 */
async function createCard(req, res) {
  try {
    const userId = req.user.id;
    const { deckId, word, pronunciation, meaning, descriptionEn, example, wordType, collocation, relatedWords, note } = req.body;

    if (!deckId || !word || !meaning) {
      return res.status(400).json({
        success: false,
        message: 'deckId, word, and meaning are required.'
      });
    }

    const newCard = await cardService.createCard(userId, { deckId, word, pronunciation, meaning, descriptionEn, example, wordType, collocation, relatedWords, note });

    res.status(201).json({
      success: true,
      message: 'Card created successfully.',
      data: newCard
    });
  } catch (err) {
    console.error('Create card API error:', err);
    if (err.message === 'Deck not found.') {
      return res.status(404).json({ success: false, message: err.message });
    }
    res.status(err.message.startsWith('Failed') ? 400 : 500).json({
      success: false,
      message: err.message || 'Internal server error creating card.'
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
    const userId = req.user.id;
    const { word, pronunciation, meaning, descriptionEn, example, wordType, collocation, relatedWords, note } = req.body;

    if (!cardId) {
      return res.status(400).json({
        success: false,
        message: 'Card ID is required.'
      });
    }

    const updatedCard = await cardService.updateCard(cardId, userId, { word, pronunciation, meaning, descriptionEn, example, wordType, collocation, relatedWords, note });

    res.status(200).json({
      success: true,
      message: 'Card updated successfully.',
      data: updatedCard
    });
  } catch (err) {
    console.error('Update card API error:', err);
    res.status(err.message.includes('required') || err.message.startsWith('Failed') ? 400 : 500).json({
      success: false,
      message: err.message || 'Internal server error updating card.'
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
    const userId = req.user.id;

    if (!cardId) {
      return res.status(400).json({
        success: false,
        message: 'Card ID is required.'
      });
    }

    await cardService.deleteCard(cardId, userId);

    res.status(200).json({
      success: true,
      message: 'Card deleted successfully.'
    });
  } catch (err) {
    console.error('Delete card API error:', err);
    if (err.message === 'Card not found.') {
      return res.status(404).json({ success: false, message: err.message });
    }
    res.status(err.message.startsWith('Failed') ? 400 : 500).json({
      success: false,
      message: err.message || 'Internal server error deleting card.'
    });
  }
}

/**
 * Create multiple cards in a deck
 * POST /api/cards/bulk
 */
async function createBulkCards(req, res) {
  try {
    const userId = req.user.id;
    const { deckId, cards } = req.body;

    if (!deckId || !cards || !Array.isArray(cards)) {
      return res.status(400).json({
        success: false,
        message: 'deckId and an array of cards are required.'
      });
    }

    const insertedCount = await cardService.createBulkCards(deckId, userId, cards);

    res.status(201).json({
      success: true,
      message: `${insertedCount} cards created successfully.`
    });
  } catch (err) {
    console.error('Create bulk cards API error:', err);
    if (err.message === 'Deck not found.') {
      return res.status(404).json({ success: false, message: err.message });
    }
    res.status(err.message.startsWith('Failed') || err.message.includes('valid cards') ? 400 : 500).json({
      success: false,
      message: err.message || 'Internal server error creating bulk cards.'
    });
  }
}

module.exports = {
  createCard,
  updateCard,
  deleteCard,
  createBulkCards
};
