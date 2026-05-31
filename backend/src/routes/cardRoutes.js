const express = require('express');
const router = express.Router();
const cardController = require('../controllers/cardController');
const { authMiddleware } = require('../middleware/authMiddleware');

// Protected routes for card CRUD
router.post('/', authMiddleware, cardController.createCard);
router.post('/bulk', authMiddleware, cardController.createBulkCards);
router.put('/:id', authMiddleware, cardController.updateCard);
router.delete('/:id', authMiddleware, cardController.deleteCard);

module.exports = router;
