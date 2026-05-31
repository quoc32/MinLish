const { createClient } = require('@supabase/supabase-js');
require('dotenv').config({ path: '/Users/vovantu/Documents/LAPTRINHDIDONG/MinLish/backend/.env' });

const supabase = createClient(process.env.SUPABASE_URL, process.env.SUPABASE_ANON_KEY);

async function run() {
  try {
    console.log('1. Checking decks count...');
    const { count: deckCount, error: deckErr } = await supabase.from('decks').select('*', { count: 'exact', head: true });
    if (deckErr) console.error('Deck error:', deckErr);
    else console.log('Successfully found decks count:', deckCount);

    console.log('2. Checking cards count...');
    const { count: cardCount, error: cardErr } = await supabase.from('cards').select('*', { count: 'exact', head: true });
    if (cardErr) console.error('Card error:', cardErr);
    else console.log('Successfully found cards count:', cardCount);

    console.log('3. Fetching all target goals in decks table...');
    const { data: decksData } = await supabase.from('decks').select('name, target_goal, user_id');
    console.log('Decks currently in DB:', decksData);

  } catch (err) {
    console.error('Fatal:', err);
  }
}

run();
