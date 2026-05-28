const express = require('express');
const router = express.Router();
const notifyController = require('../controllers/notifyController');
const { authMiddleware } = require('../middleware/authMiddleware');

// Protected notifications routes
router.get('/', authMiddleware, notifyController.getNotifications);
router.patch('/:id/read', authMiddleware, notifyController.readNotification);

module.exports = router;
