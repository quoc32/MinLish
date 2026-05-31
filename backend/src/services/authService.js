const { supabase } = require('../config/supabase');
const { saveUserEmailLocal } = require('../utils/userEmailCache');

async function register({ email, password, displayName, targetGoal, wordsPerDay }) {
  // 1. Register user in Supabase Auth
  const { data: authData, error: authError } = await supabase.auth.signUp({
    email,
    password
  });

  if (authError || !authData.user) {
    throw new Error(authError ? authError.message : 'Registration failed in Supabase Auth');
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
        words_per_day: wordsPerDay ? parseInt(wordsPerDay, 10) : 20,
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
    throw new Error(`Auth user created, but failed to initialize profile row: ${profileError.message}`);
  }

  saveUserEmailLocal(userId, email);

  return {
    user: authData.user,
    profile: profileData,
    session: authData.session
  };
}

async function login({ email, password }) {
  const { data, error } = await supabase.auth.signInWithPassword({
    email,
    password
  });

  if (error) {
    throw new Error(error.message);
  }

  // Fetch the user's custom profile
  const { data: profile } = await supabase
    .from('profiles')
    .select('*')
    .eq('id', data.user.id)
    .single();

  saveUserEmailLocal(data.user.id, email);

  return {
    user: data.user,
    profile: profile || null,
    session: data.session
  };
}

async function loginWithGoogle({ idToken }) {
  const { data, error } = await supabase.auth.signInWithIdToken({
    provider: 'google',
    token: idToken,
  });

  if (error) {
    throw new Error(error.message);
  }

  const userId = data.user.id;
  const email = data.user.email;
  const name = data.user.user_metadata?.full_name || email.split('@')[0];

  // Upsert profile
  let { data: profile } = await supabase
    .from('profiles')
    .select('*')
    .eq('id', userId)
    .single();

  if (!profile) {
    const { data: newProfile, error: profileError } = await supabase
      .from('profiles')
      .insert([
        {
          id: userId,
          display_name: name,
          level: 1,
          xp: 0,
          streak: 0,
          max_streak: 0,
          retention_rate: 0,
          words_per_day: 20
        }
      ])
      .select()
      .single();

    if (profileError) {
      throw new Error('Google Login: User authenticated but failed to create profile: ' + profileError.message);
    }
    profile = newProfile;
  }

  saveUserEmailLocal(userId, email);

  return {
    user: data.user,
    profile: profile,
    session: data.session
  };
}

async function getProfile(userId) {
  const { data: profile, error } = await supabase
    .from('profiles')
    .select('*')
    .eq('id', userId)
    .single();

  if (error || !profile) {
    throw new Error('Profile not found.');
  }

  return profile;
}

async function updateProfile(userId, updates) {
  const payload = {};
  if (updates.display_name !== undefined) payload.display_name = updates.display_name;
  if (updates.target_goal !== undefined) payload.target_goal = updates.target_goal;
  if (updates.words_per_day !== undefined) payload.words_per_day = parseInt(updates.words_per_day, 10);
  if (updates.reminder_time !== undefined) payload.reminder_time = updates.reminder_time;

  if (Object.keys(payload).length === 0) {
    throw new Error('No valid fields to update.');
  }

  const { data: profile, error } = await supabase
    .from('profiles')
    .update(payload)
    .eq('id', userId)
    .select()
    .single();

  if (error) {
    throw new Error('Failed to update profile: ' + error.message);
  }

  return profile;
}

async function forgotPassword(email) {
  const { error } = await supabase.auth.resetPasswordForEmail(email, {
    redirectTo: 'minlish://reset-password'
  });

  if (error) {
    throw new Error(error.message);
  }
  return true;
}

async function resetPassword(token, newPassword) {
  const { createClient } = require('@supabase/supabase-js');
  const tempClient = createClient(process.env.SUPABASE_URL, process.env.SUPABASE_ANON_KEY, {
    auth: {
      persistSession: false,
      autoRefreshToken: false
    }
  });

  const { error: sessionError } = await tempClient.auth.setSession({
    access_token: token,
    refresh_token: ''
  });

  if (sessionError) {
    throw new Error('Invalid authentication session: ' + sessionError.message);
  }

  const { error: updateError } = await tempClient.auth.updateUser({
    password: newPassword
  });

  if (updateError) {
    throw new Error('Failed to update password: ' + updateError.message);
  }

  return true;
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
