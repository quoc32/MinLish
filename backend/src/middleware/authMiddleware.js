const { supabase } = require('../config/supabase');

/**
 * Express Middleware to authenticate and authorize requests
 * using Supabase JWT or developer bypass header
 */
async function authMiddleware(req, res, next) {
  try {
    // 1. Support developer/bypass header for rapid testing
    const testUserId = req.headers['x-user-id'];
    if (testUserId) {
      req.user = { id: testUserId };
      return next();
    }

    // 2. Fetch the authorization header
    const authHeader = req.headers.authorization;
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return res.status(401).json({
        success: false,
        message: 'No authorization token provided. Format: Bearer <token>'
      });
    }

    // Extract the raw token
    const token = authHeader.split(' ')[1];

    // Verify token with Supabase Auth
    const { data: { user }, error } = await supabase.auth.getUser(token);

    if (error || !user) {
      return res.status(401).json({
        success: false,
        message: 'Invalid or expired authorization token.',
        error: error ? error.message : 'User not found'
      });
    }

    // Attach user record to request
    req.user = user;
    next();
  } catch (err) {
    console.error('Auth middleware error:', err);
    res.status(500).json({
      success: false,
      message: 'Internal server error during authentication.'
    });
  }
}

module.exports = { authMiddleware };
