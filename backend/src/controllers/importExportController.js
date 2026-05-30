const importExportService = require('../services/importExportService');

/**
 * Export a deck
 * GET /api/import-export/export/:deckId
 */
async function exportDeck(req, res) {
  try {
    const deckId = req.params.deckId;
    const userId = req.user.id;

    if (!deckId) {
      return res.status(400).json({
        success: false,
        message: 'Deck ID is required.'
      });
    }

    const exportedData = await importExportService.exportDeck(deckId, userId);

    res.status(200).json({
      success: true,
      data: exportedData
    });
  } catch (err) {
    console.error('Export deck API error:', err);
    if (err.message === 'Deck not found.') {
      return res.status(404).json({ success: false, message: err.message });
    }
    if (err.message.includes('Access denied')) {
      return res.status(403).json({ success: false, message: err.message });
    }
    res.status(err.message.startsWith('Failed') ? 400 : 500).json({
      success: false,
      message: err.message || 'Internal server error exporting deck.'
    });
  }
}

/**
 * Import a deck
 * POST /api/import-export/import
 */
async function importDeck(req, res) {
  try {
    const userId = req.user.id;
    const { deck, cards } = req.body;

    const newDeck = await importExportService.importDeck(userId, { deck, cards });

    res.status(201).json({
      success: true,
      message: 'Deck imported successfully.',
      data: newDeck
    });
  } catch (err) {
    console.error('Import deck API error:', err);
    res.status(err.message.startsWith('Failed') || err.message.includes('required') ? 400 : 500).json({
      success: false,
      message: err.message || 'Internal server error importing deck.'
    });
  }
}

/**
 * Export a deck as CSV
 * GET /api/import-export/export/:deckId/csv
 */
async function exportDeckCsv(req, res) {
  try {
    const deckId = req.params.deckId;
    const userId = req.user.id;

    if (!deckId) {
      return res.status(400).json({
        success: false,
        message: 'Deck ID is required.'
      });
    }

    const { deckName, csv } = await importExportService.exportDeckCsv(deckId, userId);

    const safeFileName = (deckName || 'deck').replace(/[^a-zA-Z0-9_\-]/g, '_');
    res.setHeader('Content-Type', 'text/csv');
    res.setHeader('Content-Disposition', `attachment; filename="${safeFileName}.csv"`);
    res.status(200).send(csv);
  } catch (err) {
    console.error('Export deck CSV API error:', err);
    if (err.message === 'Deck not found.') {
      return res.status(404).json({ success: false, message: err.message });
    }
    if (err.message.includes('Access denied')) {
      return res.status(403).json({ success: false, message: err.message });
    }
    res.status(err.message.startsWith('Failed') ? 400 : 500).json({
      success: false,
      message: err.message || 'Internal server error exporting deck as CSV.'
    });
  }
}

/**
 * Import a deck from CSV
 * POST /api/import-export/import/csv
 */
async function importDeckCsv(req, res) {
  try {
    const userId = req.user.id;
    const { deckName, csvData } = req.body;

    if (!deckName || !csvData) {
      return res.status(400).json({
        success: false,
        message: 'deckName and csvData are required.'
      });
    }

    const parsedCards = importExportService.parseCsv(csvData);

    const newDeck = await importExportService.importDeck(userId, {
      deck: { name: deckName },
      cards: parsedCards
    });

    res.status(201).json({
      success: true,
      message: 'Deck imported from CSV successfully.',
      data: newDeck
    });
  } catch (err) {
    console.error('Import deck CSV API error:', err);
    res.status(err.message.startsWith('Failed') || err.message.includes('required') ? 400 : 500).json({
      success: false,
      message: err.message || 'Internal server error importing deck from CSV.'
    });
  }
}

module.exports = {
  exportDeck,
  importDeck,
  exportDeckCsv,
  importDeckCsv
};
