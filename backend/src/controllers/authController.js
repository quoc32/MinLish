const { supabase } = require('../config/supabase');

/**
 * Register a new user
 * POST /api/auth/register
 */
async function register(req, res) {
  try {
    const { email, password, displayName, targetGoal } = req.body;

    if (!email || !password || !displayName) {
      return res.status(400).json({
        success: false,
        message: 'Missing required fields: email, password, displayName'
      });
    }

    // 1. Register user in Supabase Auth
    const { data: authData, error: authError } = await supabase.auth.signUp({
      email,
      password
    });

    if (authError || !authData.user) {
      return res.status(400).json({
        success: false,
        message: authError ? authError.message : 'Registration failed in Supabase Auth'
      });
    }

    const userId = authData.user.id;

    // 2. Insert user profile into the profiles table
    const { data: profileData, error: profileError } = await supabase
      .from('profiles')
      .insert([
        {
          id: userId,
          display_name: displayName,
          target_goal: targetGoal || null,
          level: 1,
          xp: 0,
          streak: 0,
          max_streak: 0,
          retention_rate: 0
        }
      ])
      .select()
      .single();

    if (profileError) {
      console.error('Profile creation error:', profileError);
      return res.status(400).json({
        success: false,
        message: 'Auth user created, but failed to initialize profile row.',
        error: profileError.message,
        userId
      });
    }

    res.status(201).json({
      success: true,
      message: 'User registered successfully.',
      data: {
        user: authData.user,
        profile: profileData,
        session: authData.session
      }
    });
  } catch (err) {
    console.error('Registration API error:', err);
    res.status(500).json({
      success: false,
      message: 'Internal server error during registration.'
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

    const { data, error } = await supabase.auth.signInWithPassword({
      email,
      password
    });

    if (error) {
      return res.status(400).json({
        success: false,
        message: error.message
      });
    }

    // Fetch matching profile
    const { data: profile } = await supabase
      .from('profiles')
      .select('*')
      .eq('id', data.user.id)
      .single();

    res.status(200).json({
      success: true,
      message: 'Logged in successfully.',
      data: {
        user: data.user,
        session: data.session,
        profile: profile || null
      }
    });
  } catch (err) {
    console.error('Login API error:', err);
    res.status(500).json({
      success: false,
      message: 'Internal server error during login.'
    });
  }
}

/**
 * Get profile details
 * GET /api/auth/profile
 */
async function getProfile(req, res) {
  try {
    const userId = req.user.id;

    const { data: profile, error } = await supabase
      .from('profiles')
      .select('*')
      .eq('id', userId)
      .single();

    if (error || !profile) {
      return res.status(404).json({
        success: false,
        message: 'Profile not found.',
        error: error ? error.message : 'Not found'
      });
    }

    res.status(200).json({
      success: true,
      data: profile
    });
  } catch (err) {
    console.error('Get profile API error:', err);
    res.status(500).json({
      success: false,
      message: 'Internal server error fetching profile.'
    });
  }
}

/**
 * Update profile details
 * PUT /api/auth/profile
 */
async function updateProfile(req, res) {
  try {
    const userId = req.user.id;
    const { displayName, targetGoal, level, xp, streak, maxStreak } = req.body;

    // Prepare update payload dynamically
    const updates = {};
    if (displayName !== undefined) updates.display_name = displayName;
    if (targetGoal !== undefined) updates.target_goal = targetGoal;
    if (level !== undefined) updates.level = parseInt(level, 10);
    if (xp !== undefined) updates.xp = parseInt(xp, 10);
    if (streak !== undefined) updates.streak = parseInt(streak, 10);
    if (maxStreak !== undefined) updates.max_streak = parseInt(maxStreak, 10);

    updates.updated_at = new Date().toISOString();

    const { data: profile, error } = await supabase
      .from('profiles')
      .update(updates)
      .eq('id', userId)
      .select()
      .single();

    if (error) {
      return res.status(400).json({
        success: false,
        message: 'Failed to update profile.',
        error: error.message
      });
    }

    res.status(200).json({
      success: true,
      message: 'Profile updated successfully.',
      data: profile
    });
  } catch (err) {
    console.error('Update profile API error:', err);
    res.status(500).json({
      success: false,
      message: 'Internal server error updating profile.'
    });
  }
}

module.exports = {
  register,
  login,
  getProfile,
  updateProfile
};
