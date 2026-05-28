const express = require('express');
const router = express.Router();
const learnController = require('../controllers/learnController');
const { authMiddleware } = require('../middleware/authMiddleware');

// Protected learning routes
router.get('/daily', authMiddleware, learnController.getDailyPlan);
router.post('/review', authMiddleware, learnController.submitReview);

module.exports = router;
