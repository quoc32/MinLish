const { createClient } = require('@supabase/supabase-js');
require('dotenv').config();

const supabase = createClient(process.env.SUPABASE_URL, process.env.SUPABASE_ANON_KEY);

async function run() {
  try {
    console.log('Fetching a record from word_progress...');
    const { data: wpData, error: wpErr } = await supabase
      .from('word_progress')
      .select('*')
      .limit(1);
    if (wpErr) {
      console.error('word_progress error:', wpErr);
    } else {
      console.log('word_progress record:', wpData);
    }

    console.log('Fetching a record from study_activity...');
    const { data: saData, error: saErr } = await supabase
      .from('study_activity')
      .select('*')
      .limit(1);
    if (saErr) {
      console.error('study_activity error:', saErr);
    } else {
      console.log('study_activity record:', saData);
    }
  } catch (err) {
    console.error('Error:', err);
  }
}

run();
