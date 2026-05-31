const authService = require('../services/authService');

/**
 * Register a new user
 * POST /api/auth/register
 */
async function register(req, res) {
  try {
    const { email, password, displayName, targetGoal, wordsPerDay } = req.body;

    if (!email || !password || !displayName) {
      return res.status(400).json({
        success: false,
        message: 'Missing required fields: email, password, displayName'
      });
    }

    const data = await authService.register({ email, password, displayName, targetGoal, wordsPerDay });

    res.status(201).json({
      success: true,
      message: 'User registered successfully.',
      data
    });
  } catch (err) {
    console.error('Registration API error:', err);
    res.status(err.message.includes('failed') ? 400 : 500).json({
      success: false,
      message: err.message || 'Internal server error during registration.'
    });
  }
}

/**
 * Login user
 * POST /api/auth/login
 */
async function login(req, res) {
  try {
    const { email, password } = req.body;

    if (!email || !password) {
      return res.status(400).json({
        success: false,
        message: 'Email and password are required.'
      });
    }

    const data = await authService.login({ email, password });

    res.status(200).json({
      success: true,
      message: 'Login successful.',
      data
    });
  } catch (err) {
    console.error('Login API error:', err);
    res.status(401).json({
      success: false,
      message: err.message || 'Invalid credentials or internal server error.'
    });
  }
}

/**
 * Google Login
 * POST /api/auth/google
 */
async function loginWithGoogle(req, res) {
  try {
    const { idToken } = req.body;

    if (!idToken) {
      return res.status(400).json({
        success: false,
        message: 'Google ID Token is required.'
      });
    }

    const data = await authService.loginWithGoogle({ idToken });

    res.status(200).json({
      success: true,
      message: 'Google login successful.',
      data
    });
  } catch (err) {
    console.error('Google Login API error:', err);
    res.status(401).json({
      success: false,
      message: err.message || 'Invalid Google token or server error.'
    });
  }
}

/**
 * Get user profile
 * GET /api/auth/profile
 */
async function getProfile(req, res) {
  try {
    const userId = req.user.id;
    const profile = await authService.getProfile(userId);

    res.status(200).json({
      success: true,
      data: profile
    });
  } catch (err) {
    console.error('Get profile error:', err);
    res.status(err.message === 'Profile not found.' ? 404 : 500).json({
      success: false,
      message: err.message || 'Internal server error fetching profile.'
    });
  }
}

/**
 * Update user profile
 * PUT /api/auth/profile
 */
async function updateProfile(req, res) {
  try {
    const userId = req.user.id;
    const profile = await authService.updateProfile(userId, req.body);

    res.status(200).json({
      success: true,
      message: 'Profile updated successfully.',
      data: profile
    });
  } catch (err) {
    console.error('Update profile API error:', err);
    res.status(400).json({
      success: false,
      message: err.message || 'Internal server error updating profile.'
    });
  }
}

async function forgotPassword(req, res) {
  try {
    const { email } = req.body;
    if (!email) {
      return res.status(400).json({
        success: false,
        message: 'Email is required.'
      });
    }

    await authService.forgotPassword(email);

    res.status(200).json({
      success: true,
      message: 'Password reset link sent to your email.'
    });
  } catch (err) {
    console.error('Forgot password API error:', err);
    res.status(400).json({
      success: false,
      message: err.message || 'Failed to request password reset.'
    });
  }
}

async function resetPassword(req, res) {
  try {
    const { password } = req.body;
    
    const authHeader = req.headers.authorization;
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return res.status(401).json({
        success: false,
        message: 'No authorization token provided.'
      });
    }
    const token = authHeader.split(' ')[1];

    if (!password) {
      return res.status(400).json({
        success: false,
        message: 'New password is required.'
      });
    }

    await authService.resetPassword(token, password);

    res.status(200).json({
      success: true,
      message: 'Password has been reset successfully.'
    });
  } catch (err) {
    console.error('Reset password API error:', err);
    res.status(400).json({
      success: false,
      message: err.message || 'Failed to reset password.'
    });
  }
}

module.exports = {
  register,
  login,
  loginWithGoogle,
  getProfile,
  updateProfile,
  forgotPassword,
  resetPassword
};
