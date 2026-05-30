require('dotenv').config();
const fs = require('fs');
const path = require('path');
const { supabase } = require('../config/supabase');

async function seed() {
  const files = ['toeic_1.json', 'toeic_2.json', 'ielts_1.json', 'ielts_2.json'];
  
  for (const file of files) {
    const filePath = path.join(__dirname, file);
    const data = JSON.parse(fs.readFileSync(filePath, 'utf-8'));
    
    const deckInfo = data.deck;
    const cards = data.cards;

    console.log(`Seeding deck: ${deckInfo.name}`);

    // Determine order_index
    const { data: lastDeck } = await supabase
      .from('decks')
      .select('order_index')
      .eq('target_goal', deckInfo.target_goal)
      .is('user_id', null)
      .order('order_index', { ascending: false })
      .limit(1)
      .maybeSingle();
      
    const nextOrderIndex = lastDeck ? lastDeck.order_index + 1 : 1;

    // Insert Deck
    const { data: newDeck, error: deckError } = await supabase
      .from('decks')
      .insert({
        name: deckInfo.name,
        icon: deckInfo.icon,
        tag: deckInfo.tag,
        total_words: cards.length,
        order_index: nextOrderIndex,
        target_goal: deckInfo.target_goal,
        user_id: null
      })
      .select()
      .single();

    if (deckError) {
      console.error(`Error creating deck ${deckInfo.name}:`, deckError.message);
      continue;
    }

    // Insert Cards
    const cardsToInsert = cards.map(c => ({
      deck_id: newDeck.id,
      word: c.word,
      pronunciation: c.pronunciation,
      meaning: c.meaning,
      description_en: c.description_en || '',
      example: c.example || ''
    }));

    const { error: cardsError } = await supabase
      .from('cards')
      .insert(cardsToInsert);
      
    if (cardsError) {
      console.error(`Error creating cards for ${deckInfo.name}:`, cardsError.message);
    } else {
      console.log(`Successfully seeded deck: ${deckInfo.name} with ${cards.length} cards.`);
    }
  }
}

seed().then(() => {
  console.log('Seeding complete.');
  process.exit(0);
}).catch(err => {
  console.error('Seeding failed:', err);
  process.exit(1);
});
