const express = require('express');
const router = express.Router();
const statController = require('../controllers/statController');
const { authMiddleware } = require('../middleware/authMiddleware');

// Protected statistics routes
router.get('/dashboard', authMiddleware, statController.getStatsDashboard);

module.exports = router;
