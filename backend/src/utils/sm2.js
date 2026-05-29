/**
 * SuperMemo-2 Spaced Repetition Engine (Enhanced)
 * 
 * Cải thiện từ SM-2 gốc với hỗ trợ short intervals cho phiên học hiện tại:
 * - again: ôn lại sau 5 phút (intra-session)
 * - hard:  ôn lại sau 10 phút (lần đầu), sau đó interval × 1.2
 * - good:  SM-2 chuẩn (1 ngày → 3 ngày → interval × EF)
 * - easy:  nhảy nhanh (4 ngày → interval × EF, EF tăng nhanh)
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
 * @returns {object} { ease_factor, interval, interval_minutes, repetitions, status, next_review }
 *   - interval: khoảng cách tính bằng ngày (0 nếu ôn lại trong phiên)
 *   - interval_minutes: khoảng cách tính bằng phút (chỉ dùng khi interval = 0)
 */
function calculateSM2(quality, prevEF = 2.50, prevInterval = 0, prevRepetitions = 0) {
  // Parse inputs to float/int
  let EF = parseFloat(prevEF) || 2.50;
  let interval = parseInt(prevInterval, 10) || 0;
  let repetitions = parseInt(prevRepetitions, 10) || 0;

  // Map quality rating to SM-2 score (0 to 5)
  const q = QUALITY_MAP[quality] !== undefined ? QUALITY_MAP[quality] : 3;

  // Khoảng cách tính bằng phút (dùng cho intra-session reviews)
  let intervalMinutes = 0;

  // ============================================================
  // SM-2 ENHANCED LOGIC
  // ============================================================
  if (q < 3) {
    // "Again" — ôn lại ngay trong phiên học hiện tại
    repetitions = 0;
    interval = 0;           // 0 ngày = intra-session
    intervalMinutes = 5;    // Ôn lại sau 5 phút

  } else if (q === 3) {
    // "Hard" — khoảng cách ngắn hơn bình thường
    if (repetitions === 0) {
      // Lần đầu gặp + chọn hard: ôn lại sau 10 phút (trong phiên)
      interval = 0;
      intervalMinutes = 10;
    } else if (repetitions === 1) {
      // Lần 2: ôn lại vào ngày mai
      interval = 1;
    } else {
      // Lần 3+: tăng chậm hơn SM-2 chuẩn (×1.2 thay vì ×EF)
      interval = Math.max(1, Math.round(interval * 1.2));
    }
    repetitions += 1;

  } else if (q === 4) {
    // "Good" — SM-2 chuẩn
    if (repetitions === 0) {
      interval = 1;         // 1 ngày
    } else if (repetitions === 1) {
      interval = 3;         // 3 ngày
    } else {
      interval = Math.round(interval * EF);
    }
    repetitions += 1;

  } else {
    // "Easy" (q=5) — nhảy nhanh, bỏ qua giai đoạn learning
    if (repetitions === 0) {
      interval = 4;         // 4 ngày (skip learning phase)
    } else if (repetitions === 1) {
      interval = 7;         // ~1 tuần
    } else {
      interval = Math.round(interval * EF);
    }
    repetitions += 1;
  }

  // ============================================================
  // EASE FACTOR CALCULATION (Giữ nguyên công thức SM-2 gốc)
  // ============================================================
  // EF' = EF + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))
  EF = EF + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02));
  
  // Bound Ease Factor at a minimum of 1.3
  EF = Math.max(1.3, EF);
  
  // Round EF to 2 decimal places
  EF = parseFloat(EF.toFixed(2));

  // ============================================================
  // WORD STATUS DETERMINATION
  // ============================================================
  let status = 'learning';
  if (quality === 'again') {
    status = 'needs_review';
  } else if (quality === 'easy' || (repetitions >= 3 && EF >= 2.5)) {
    status = 'proficient';
  } else {
    status = 'learning';
  }

  // ============================================================
  // NEXT REVIEW DATE CALCULATION
  // ============================================================
  const nextReview = new Date();
  if (interval === 0 && intervalMinutes > 0) {
    // Intra-session review: tính bằng phút
    nextReview.setMinutes(nextReview.getMinutes() + intervalMinutes);
  } else {
    // Spaced review: tính bằng ngày
    nextReview.setDate(nextReview.getDate() + interval);
  }

  return {
    ease_factor: EF,
    interval,
    interval_minutes: intervalMinutes,
    repetitions,
    status,
    next_review: nextReview.toISOString()
  };
}

module.exports = { calculateSM2 };
