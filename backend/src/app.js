const express = require('express');
const cors = require('cors');
const morgan = require('morgan');
require('dotenv').config();

// Route files
const authRoutes = require('./routes/authRoutes');
const deckRoutes = require('./routes/deckRoutes');
const learnRoutes = require('./routes/learnRoutes');
const statRoutes = require('./routes/statRoutes');
const notifyRoutes = require('./routes/notifyRoutes');
const cardRoutes = require('./routes/cardRoutes');
const importExportRoutes = require('./routes/importExportRoutes');

const app = express();
const PORT = process.env.PORT || 3000;

// Standard Middlewares
app.use(cors());
app.use(express.json());
app.use(morgan('dev'));

// Home server confirmation route
app.get('/', (req, res) => {
  res.status(200).json({
    success: true,
    message: 'MinLish Spaced Repetition API Backend is running! 🔥',
    timestamp: new Date().toISOString()
  });
});

// Register Sub-routers
app.use('/api/auth', authRoutes);
app.use('/api/decks', deckRoutes);
app.use('/api/learning', learnRoutes);
app.use('/api/stats', statRoutes);
app.use('/api/notifications', notifyRoutes);
app.use('/api/cards', cardRoutes);
app.use('/api/import-export', importExportRoutes);

// Safe Catch-All 404 Not Found Middleware
app.use((req, res, next) => {
  res.status(404).json({
    success: false,
    message: `Endpoint not found: [${req.method}] ${req.originalUrl}`
  });
});

// Safe Catch-All Global Error Handling Middleware
app.use((err, req, res, next) => {
  console.error('Unhandled Global Exception:', err);
  res.status(500).json({
    success: false,
    message: 'Something went wrong on the server.',
    error: err.message || err
  });
});

// Start Express Server
app.listen(PORT, () => {
  console.log(`===========================================================`);
  console.log(`🚀 MinLish Express Server started on http://localhost:${PORT}`);
  console.log(`🔥 Environment: ${process.env.NODE_ENV || 'development'}`);
  console.log(`===========================================================`);
});

module.exports = app; // For testing
