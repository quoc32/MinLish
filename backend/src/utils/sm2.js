/**
 * SuperMemo-2 Spaced Repetition Engine
 */

const QUALITY_MAP = {
  again: 0,
  hard: 3,
  good: 4,
  easy: 5
};

/**
 * Calculates new SM-2 parameters for a vocabulary card
 * @param {string} quality - 'again', 'hard', 'good', 'easy'
 * @param {number} prevEF - Previous ease factor (default: 2.50)
 * @param {number} prevInterval - Previous review interval in days (default: 0)
 * @param {number} prevRepetitions - Previous consecutive correct repetitions (default: 0)
 * @returns {object} { ease_factor, interval, repetitions, status, next_review }
 */
function calculateSM2(quality, prevEF = 2.50, prevInterval = 0, prevRepetitions = 0) {
  // Parse inputs to float/int
  let EF = parseFloat(prevEF) || 2.50;
  let interval = parseInt(prevInterval, 10) || 0;
  let repetitions = parseInt(prevRepetitions, 10) || 0;

  // Map quality rating to SM-2 score (0 to 5)
  const q = QUALITY_MAP[quality] !== undefined ? QUALITY_MAP[quality] : 3;

  // SM-2 logic
  if (q < 3) {
    // Incorrect answer / black out
    repetitions = 0;
    interval = 1;
  } else {
    // Correct answer
    if (repetitions === 0) {
      interval = 1;
    } else if (repetitions === 1) {
      interval = 6;
    } else {
      interval = Math.round(interval * EF);
    }
    repetitions += 1;
  }

  // Calculate new Ease Factor (EF)
  EF = EF + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02));
  
  // Bound Ease Factor at a minimum of 1.3
  EF = Math.max(1.3, EF);
  
  // Round EF to 2 decimal places
  EF = parseFloat(EF.toFixed(2));

  // Determine Word Progress Status
  let status = 'learning';
  if (quality === 'again') {
    status = 'needs_review';
  } else if (quality === 'easy' || (repetitions >= 3 && EF >= 2.5)) {
    status = 'proficient';
  } else {
    status = 'learning';
  }

  // Calculate Next Review Date
  const nextReview = new Date();
  nextReview.setDate(nextReview.getDate() + interval);

  return {
    ease_factor: EF,
    interval,
    repetitions,
    status,
    next_review: nextReview.toISOString()
  };
}

module.exports = { calculateSM2 };
