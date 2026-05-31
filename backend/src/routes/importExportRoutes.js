const express = require('express');
const router = express.Router();
const importExportController = require('../controllers/importExportController');
const { authMiddleware } = require('../middleware/authMiddleware');

router.get('/export/:deckId', authMiddleware, importExportController.exportDeck);
router.post('/import', authMiddleware, importExportController.importDeck);

// CSV endpoints
router.get('/export/:deckId/csv', authMiddleware, importExportController.exportDeckCsv);
router.post('/import/csv', authMiddleware, importExportController.importDeckCsv);

module.exports = router;
