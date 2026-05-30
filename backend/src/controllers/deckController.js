const deckService = require('../services/deckService');

/**
 * Get all decks with the active user's progress and lock status
 * GET /api/decks
 */
async function getAllDecks(req, res) {
  try {
    const userId = req.user.id;
    const formattedDecks = await deckService.getAllDecks(userId);

    res.status(200).json({
      success: true,
      data: formattedDecks
    });
  } catch (err) {
    console.error('Get all decks API error:', err);
    res.status(err.message.startsWith('Failed') ? 400 : 500).json({
      success: false,
      message: err.message || 'Internal server error fetching decks.'
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

    const { deckName, cards } = await deckService.getDeckCards(deckId, userId);

    res.status(200).json({
      success: true,
      deckName: deckName,
      data: cards
    });
  } catch (err) {
    console.error('Get deck cards API error:', err);
    if (err.message === 'Deck not found.') {
      return res.status(404).json({ success: false, message: err.message });
    }
    if (err.message.includes('Access denied')) {
      return res.status(403).json({ success: false, message: err.message });
    }
    res.status(err.message.startsWith('Failed') ? 400 : 500).json({
      success: false,
      message: err.message || 'Internal server error fetching deck cards.'
    });
  }
}

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

    const newDeck = await deckService.createDeck(userId, { name, icon, tag });

    res.status(201).json({
      success: true,
      message: 'Deck created successfully.',
      data: newDeck
    });
  } catch (err) {
    console.error('Create deck API error:', err);
    res.status(err.message.startsWith('Failed') ? 400 : 500).json({
      success: false,
      message: err.message || 'Internal server error creating deck.'
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

    const updatedDeck = await deckService.updateDeck(deckId, { name, icon, tag });

    res.status(200).json({
      success: true,
      message: 'Deck updated successfully.',
      data: updatedDeck
    });
  } catch (err) {
    console.error('Update deck API error:', err);
    res.status(err.message.includes('required') || err.message.startsWith('Failed') ? 400 : 500).json({
      success: false,
      message: err.message || 'Internal server error updating deck.'
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

    await deckService.deleteDeck(deckId);

    res.status(200).json({
      success: true,
      message: 'Deck and all related data deleted successfully.'
    });
  } catch (err) {
    console.error('Delete deck API error:', err);
    res.status(err.message.startsWith('Failed') ? 400 : 500).json({
      success: false,
      message: err.message || 'Internal server error deleting deck.'
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
