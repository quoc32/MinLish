const express = require('express');
const router = express.Router();
const deckController = require('../controllers/deckController');
const { authMiddleware } = require('../middleware/authMiddleware');

// Protected routes for decks and cards
router.get('/', authMiddleware, deckController.getAllDecks);
router.get('/:id/cards', authMiddleware, deckController.getDeckCards);
router.post('/', authMiddleware, deckController.createDeck);
router.put('/:id', authMiddleware, deckController.updateDeck);
router.delete('/:id', authMiddleware, deckController.deleteDeck);

module.exports = router;
