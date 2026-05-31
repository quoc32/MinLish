const fs = require('fs');
const path = require('path');

const cacheFilePath = path.join(__dirname, '../../scratch/user_emails.json');

function saveUserEmailLocal(userId, email) {
  try {
    const dir = path.dirname(cacheFilePath);
    if (!fs.existsSync(dir)) {
      fs.mkdirSync(dir, { recursive: true });
    }
    
    let cache = {};
    if (fs.existsSync(cacheFilePath)) {
      cache = JSON.parse(fs.readFileSync(cacheFilePath, 'utf8'));
    }
    
    cache[userId] = email;
    fs.writeFileSync(cacheFilePath, JSON.stringify(cache, null, 2), 'utf8');
  } catch (err) {
    console.error('Failed to save email locally:', err);
  }
}

function getUserEmailLocal(userId) {
  try {
    if (fs.existsSync(cacheFilePath)) {
      const cache = JSON.parse(fs.readFileSync(cacheFilePath, 'utf8'));
      return cache[userId];
    }
  } catch (err) {
    console.error('Failed to read email locally:', err);
  }
  return null;
}

function getAllUserEmailsLocal() {
  try {
    if (fs.existsSync(cacheFilePath)) {
      return JSON.parse(fs.readFileSync(cacheFilePath, 'utf8'));
    }
  } catch (err) {
    console.error('Failed to read all emails locally:', err);
  }
  return {};
}

module.exports = {
  saveUserEmailLocal,
  getUserEmailLocal,
  getAllUserEmailsLocal
};
