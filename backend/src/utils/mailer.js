const nodemailer = require('nodemailer');
const fs = require('fs');
const path = require('path');

const transporter = nodemailer.createTransport({
  service: 'gmail',
  auth: {
    user: process.env.MAIL_USER || 'kietnguyenportfolio@gmail.com',
    pass: process.env.MAIL_PASS || 'xvtymdondclilsir'
  }
});

async function sendEmail({ to, subject, html, text }) {
  const fromName = 'MinLish English Learning';
  const fromEmail = process.env.MAIL_USER || 'kietnguyenportfolio@gmail.com';

  const mailOptions = {
    from: `"${fromName}" <${fromEmail}>`,
    to,
    subject,
    text: text || '',
    html: html || ''
  };

  try {
    const info = await transporter.sendMail(mailOptions);
    console.log(`Email sent successfully to ${to}: ${info.messageId}`);
    return { success: true, messageId: info.messageId };
  } catch (err) {
    console.error(`Failed to send real email to ${to} via SMTP:`, err.message);
    
    // Lưu vào scratch/email_logs.txt làm cơ chế dự phòng
    const scratchDir = path.join(__dirname, '../../scratch');
    if (!fs.existsSync(scratchDir)) {
      fs.mkdirSync(scratchDir, { recursive: true });
    }
    const logPath = path.join(scratchDir, 'email_logs.txt');
    const logContent = `
========================================
TIMESTAMP: ${new Date().toISOString()}
TO: ${to}
SUBJECT: ${subject}
TEXT: ${text}
HTML: ${html}
LỖI SMTP: ${err.message}
========================================
`;
    fs.appendFileSync(logPath, logContent, 'utf8');
    console.log(`Wrote simulated email log to ${logPath}`);
    return { success: false, error: err.message, loggedLocal: true };
  }
}

module.exports = { sendEmail };
