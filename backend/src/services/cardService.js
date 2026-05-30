const { supabase } = require('../config/supabase');

async function createCard(userId, { deckId, word, pronunciation, meaning, descriptionEn, example, wordType, collocation, relatedWords, note }) {
  // 1. Verify deck exists and check ownership
  const { data: deck, error: deckError } = await supabase
    .from('decks')
    .select('id, total_words, user_id')
    .eq('id', deckId)
    .single();

  if (deckError || !deck) {
    throw new Error('Deck not found.');
  }

  if (deck.user_id !== userId) {
    throw new Error('Access denied. You can only add cards to your own decks.');
  }

  // 2. Insert the new card
  const { data: newCard, error: insertError } = await supabase
    .from('cards')
    .insert({
      deck_id: deckId,
      word: word.trim(),
      pronunciation: pronunciation || '',
      meaning: meaning.trim(),
      description_en: descriptionEn || '',
      example: example || '',
      word_type: wordType || '',
      collocation: collocation || '',
      related_words: relatedWords || '',
      note: note || ''
    })
    .select()
    .single();

  if (insertError) {
    throw new Error('Failed to create card: ' + insertError.message);
  }

  // 3. Update total_words count in the deck
  await supabase
    .from('decks')
    .update({ total_words: (deck.total_words || 0) + 1 })
    .eq('id', deckId);

  return newCard;
}

async function updateCard(cardId, userId, { word, pronunciation, meaning, descriptionEn, example, wordType, collocation, relatedWords, note }) {
  // 1. Verify card ownership via deck
  const { data: card, error: cardError } = await supabase
    .from('cards')
    .select('id, deck_id, decks(user_id)')
    .eq('id', cardId)
    .single();

  if (cardError || !card) {
    throw new Error('Card not found.');
  }

  if (card.decks && card.decks.user_id !== userId) {
    throw new Error('Access denied. You do not own the deck this card belongs to.');
  }

  // 2. Build update payload dynamically
  const updates = {};
  if (word !== undefined) updates.word = word.trim();
  if (pronunciation !== undefined) updates.pronunciation = pronunciation;
  if (meaning !== undefined) updates.meaning = meaning.trim();
  if (descriptionEn !== undefined) updates.description_en = descriptionEn;
  if (example !== undefined) updates.example = example;
  if (wordType !== undefined) updates.word_type = wordType;
  if (collocation !== undefined) updates.collocation = collocation;
  if (relatedWords !== undefined) updates.related_words = relatedWords;
  if (note !== undefined) updates.note = note;

  if (Object.keys(updates).length === 0) {
    throw new Error('At least one field is required to update.');
  }

  const { data: updatedCard, error } = await supabase
    .from('cards')
    .update(updates)
    .eq('id', cardId)
    .select()
    .single();

  if (error) {
    throw new Error('Failed to update card: ' + error.message);
  }

  return updatedCard;
}

async function deleteCard(cardId, userId) {
  // 1. Fetch card to get its deck_id and verify ownership
  const { data: card, error: cardError } = await supabase
    .from('cards')
    .select('id, deck_id, decks(user_id)')
    .eq('id', cardId)
    .single();

  if (cardError || !card) {
    throw new Error('Card not found.');
  }

  if (card.decks && card.decks.user_id !== userId) {
    throw new Error('Access denied. You do not own the deck this card belongs to.');
  }

  // 2. Cascade delete: word_progress for this card
  await supabase
    .from('word_progress')
    .delete()
    .eq('card_id', cardId);

  // 3. Delete the card
  const { error: deleteError } = await supabase
    .from('cards')
    .delete()
    .eq('id', cardId);

  if (deleteError) {
    throw new Error('Failed to delete card: ' + deleteError.message);
  }

  // 4. Update total_words count in the deck
  const { count: remainingCards } = await supabase
    .from('cards')
    .select('*', { count: 'exact', head: true })
    .eq('deck_id', card.deck_id);

  await supabase
    .from('decks')
    .update({ total_words: remainingCards || 0 })
    .eq('id', card.deck_id);
}

async function createBulkCards(deckId, userId, cardsData) {
  // 1. Verify deck exists and check ownership
  const { data: deck, error: deckError } = await supabase
    .from('decks')
    .select('id, total_words, user_id')
    .eq('id', deckId)
    .single();

  if (deckError || !deck) {
    throw new Error('Deck not found.');
  }

  if (deck.user_id !== userId) {
    throw new Error('Access denied. You can only add cards to your own decks.');
  }

  // 2. Prepare cards to insert
  const cardsToInsert = cardsData.map(c => ({
    deck_id: deckId,
    word: c.word ? c.word.trim() : '',
    pronunciation: c.pronunciation || '',
    meaning: c.meaning ? c.meaning.trim() : '',
    description_en: c.descriptionEn || c.description_en || '',
    example: c.example || '',
    word_type: c.wordType || c.word_type || '',
    collocation: c.collocation || '',
    related_words: c.relatedWords || c.related_words || '',
    note: c.note || ''
  })).filter(c => c.word && c.meaning);

  if (cardsToInsert.length === 0) {
    throw new Error('No valid cards to insert.');
  }

  // 3. Insert the new cards
  const { error: insertError } = await supabase
    .from('cards')
    .insert(cardsToInsert);

  if (insertError) {
    throw new Error('Failed to create cards in bulk: ' + insertError.message);
  }

  // 4. Update total_words count in the deck
  await supabase
    .from('decks')
    .update({ total_words: (deck.total_words || 0) + cardsToInsert.length })
    .eq('id', deckId);
    
  return cardsToInsert.length;
}

module.exports = {
  createCard,
  updateCard,
  deleteCard,
  createBulkCards
};
