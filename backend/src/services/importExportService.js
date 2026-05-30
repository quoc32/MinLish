const { supabase } = require('../config/supabase');
const deckService = require('./deckService');

async function exportDeck(deckId, userId) {
  const { data: deck, error: deckError } = await supabase
    .from('decks')
    .select('*')
    .eq('id', deckId)
    .single();

  if (deckError || !deck) {
    throw new Error('Deck not found.');
  }

  // Check ownership
  if (deck.user_id && deck.user_id !== userId) {
    throw new Error('Access denied. This deck belongs to another user.');
  }

  const { data: cards, error: cardsError } = await supabase
    .from('cards')
    .select('word, pronunciation, meaning, description_en, example, word_type, collocation, related_words, note')
    .eq('deck_id', deckId);

  if (cardsError) {
    throw new Error('Failed to fetch cards: ' + cardsError.message);
  }

  return {
    deck: {
      name: deck.name,
      icon: deck.icon,
      tag: deck.tag,
      target_goal: deck.target_goal
    },
    cards: cards
  };
}

async function importDeck(userId, { deck, cards }) {
  if (!deck || !deck.name) {
    throw new Error('Deck name is required for import.');
  }

  // Create deck
  const newDeck = await deckService.createDeck(userId, {
    name: deck.name,
    icon: deck.icon,
    tag: deck.tag
  });

  // Insert cards
  if (cards && cards.length > 0) {
    const cardsToInsert = cards.map((c, index) => {
      const word = c.word ? c.word.trim() : '';
      const meaning = c.meaning ? c.meaning.trim() : '';

      if (!word) throw new Error(`Row/Item ${index + 1} is missing the required 'word' field.`);
      if (!meaning) throw new Error(`Row/Item ${index + 1} (word: ${word}) is missing the required 'meaning' field.`);

      return {
        deck_id: newDeck.id,
        word,
        pronunciation: c.pronunciation || '',
        meaning,
        description_en: c.description_en || '',
        example: c.example || '',
        word_type: c.word_type || '',
        collocation: c.collocation || '',
        related_words: c.related_words || '',
        note: c.note || ''
      };
    });

    if (cardsToInsert.length > 0) {
      const { error: insertError } = await supabase
        .from('cards')
        .insert(cardsToInsert);

      if (insertError) {
        throw new Error('Failed to import cards: ' + insertError.message);
      }

      // Update total_words
      await supabase
        .from('decks')
        .update({ total_words: cardsToInsert.length })
        .eq('id', newDeck.id);
        
      newDeck.total_words = cardsToInsert.length;
    }
  }

  return newDeck;
}

/**
 * Import multiple decks from a list of cards (used for CSV import with deck_name column)
 */
async function importMultipleDecks(userId, defaultDeckName, cards) {
  if (!cards || cards.length === 0) {
    throw new Error('No cards to import. Please check your file.');
  }

  // Pre-validate all cards before processing
  cards.forEach((c, index) => {
    if (!c.word || !c.word.trim()) {
      throw new Error(`Row ${index + 1} is missing the required 'word' field.`);
    }
    if (!c.meaning || !c.meaning.trim()) {
      throw new Error(`Row ${index + 1} (word: ${c.word}) is missing the required 'meaning' field.`);
    }
  });

  // Group cards by deck_name
  const groupedCards = {};
  cards.forEach(card => {
    const dName = (card.deck_name || defaultDeckName || 'Imported Deck').trim();
    if (!groupedCards[dName]) {
      groupedCards[dName] = { 
        cards: [], 
        tag: card.deck_tag || card.tag || null 
      };
    }
    groupedCards[dName].cards.push(card);
  });

  const createdDecks = [];
  for (const [dName, data] of Object.entries(groupedCards)) {
    const newDeck = await importDeck(userId, { deck: { name: dName, tag: data.tag }, cards: data.cards });
    createdDecks.push(newDeck);
  }

  return createdDecks;
}

/**
 * Escape a field value for CSV output.
 * Wraps in double quotes if the value contains commas, quotes, or newlines.
 */
function escapeCsvField(value) {
  const str = value == null ? '' : String(value);
  if (str.includes(',') || str.includes('"') || str.includes('\n') || str.includes('\r')) {
    return '"' + str.replace(/"/g, '""') + '"';
  }
  return str;
}

/**
 * Export a deck as a CSV string.
 * Columns: word, pronunciation, meaning, description_en, example, word_type, collocation, related_words, note, deck_name, deck_tag
 */
async function exportDeckCsv(deckId, userId) {
  const { deck, cards } = await exportDeck(deckId, userId);

  const header = 'word,pronunciation,meaning,description_en,example,word_type,collocation,related_words,note,deck_name,deck_tag';
  const rows = cards.map(c => {
    return [
      escapeCsvField(c.word),
      escapeCsvField(c.pronunciation),
      escapeCsvField(c.meaning),
      escapeCsvField(c.description_en),
      escapeCsvField(c.example),
      escapeCsvField(c.word_type),
      escapeCsvField(c.collocation),
      escapeCsvField(c.related_words),
      escapeCsvField(c.note),
      escapeCsvField(deck.name),
      escapeCsvField(deck.tag)
    ].join(',');
  });

  const csvString = [header, ...rows].join('\n');
  return { deckName: deck.name, csv: csvString };
}

/**
 * Parse a single CSV line into an array of field values.
 */
function parseCSVLine(line) {
  const fields = [];
  let current = '';
  let inQuotes = false;
  let i = 0;

  while (i < line.length) {
    const ch = line[i];
    if (inQuotes) {
      if (ch === '"') {
        // Check for escaped quote ("")
        if (i + 1 < line.length && line[i + 1] === '"') {
          current += '"';
          i += 2;
        } else {
          inQuotes = false;
          i++;
        }
      } else {
        current += ch;
        i++;
      }
    } else {
      if (ch === '"') {
        inQuotes = true;
        i++;
      } else if (ch === ',') {
        fields.push(current);
        current = '';
        i++;
      } else {
        current += ch;
        i++;
      }
    }
  }
  fields.push(current);
  return fields;
}

/**
 * Parse a CSV string into an array of card objects.
 * Handles quoted fields containing commas and escaped double quotes.
 */
function parseCsv(csvData) {
  const columns = ['word', 'pronunciation', 'meaning', 'description_en', 'example', 'word_type', 'collocation', 'related_words', 'note', 'deck_name', 'deck_tag'];
  const results = [];

  // Split into lines, handling both \r\n and \n, but respecting quoted fields
  const lines = [];
  let current = '';
  let inQuotes = false;
  for (let i = 0; i < csvData.length; i++) {
    const ch = csvData[i];
    if (ch === '"') {
      inQuotes = !inQuotes;
      current += ch;
    } else if ((ch === '\n' || ch === '\r') && !inQuotes) {
      if (current.trim().length > 0) {
        lines.push(current);
      }
      current = '';
      // skip \r\n pair
      if (ch === '\r' && i + 1 < csvData.length && csvData[i + 1] === '\n') {
        i++;
      }
    } else {
      current += ch;
    }
  }
  if (current.trim().length > 0) {
    lines.push(current);
  }

  // Determine start index: skip header if it matches column names
  let startIndex = 0;
  if (lines.length > 0) {
    const firstLineLower = lines[0].toLowerCase().replace(/\s/g, '');
    if (firstLineLower.startsWith('word,')) {
      startIndex = 1;
    }
  }

  for (let i = startIndex; i < lines.length; i++) {
    const fields = parseCSVLine(lines[i]);
    if (fields.length === 0) continue;

    const card = {};
    for (let j = 0; j < columns.length; j++) {
      card[columns[j]] = j < fields.length ? fields[j] : '';
    }
    results.push(card);
  }

  return results;
}

module.exports = {
  exportDeck,
  importDeck,
  importMultipleDecks,
  exportDeckCsv,
  parseCsv
};
