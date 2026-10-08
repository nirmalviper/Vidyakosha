const express = require('express');
const cors = require('cors');
const crypto = require('crypto');
const path = require('path');
const fs = require('fs');
const { Pool } = require('pg');

const app = express();
app.use(cors());
app.use(express.json({ limit: '10mb' }));

const PORT = process.env.PORT || 80;
const DATABASE_URL = process.env.DATABASE_URL || 'postgresql://lipi:lipi_secret@postgres:5432/lipi';
const DEFAULT_ADMIN_PASSWORD = process.env.DEFAULT_ADMIN_PASSWORD || 'test1234';

const pool = new Pool({ connectionString: DATABASE_URL });

function generateSalt() {
  return crypto.randomBytes(16).toString('base64');
}

// Matches Android SecurityVault.hashPassword("$salt:$password:polyglot_v3")
function hashPassword(password, salt) {
  return crypto
    .createHash('sha256')
    .update(`${salt}:${password}:polyglot_v3`, 'utf8')
    .digest('base64');
}

function verifyPassword(password, salt, expectedHash) {
  const computed = hashPassword(password, salt);
  const a = Buffer.from(computed, 'utf8');
  const b = Buffer.from(expectedHash, 'utf8');
  if (a.length !== b.length) return false;
  return crypto.timingSafeEqual(a, b);
}

async function initializeDatabaseAndDefaultAdmin() {
  const client = await pool.connect();
  try {
    const initSqlPath = path.join(__dirname, 'init.sql');
    if (fs.existsSync(initSqlPath)) {
      const sql = fs.readFileSync(initSqlPath, 'utf8');
      await client.query(sql);
    }

    // Seed default Superadmin accounts with password test1234 on first deployment
    const defaultAdmins = [
      { email: 'superadmin@lipi.org', name: 'Lipi Superadmin' },
      { email: 'admin@lipi.org', name: 'Lipi Platform Admin (Default)' },
      { email: 'superadmin@vidyakosha.org', name: 'Lipi Superadmin (Legacy Alias)' }
    ];

    for (const adm of defaultAdmins) {
      const existing = await client.query('SELECT id FROM users WHERE LOWER(email) = LOWER($1) LIMIT 1', [adm.email]);
      if (existing.rowCount === 0) {
        const salt = generateSalt();
        const hash = hashPassword(DEFAULT_ADMIN_PASSWORD, salt);
        const inserted = await client.query(
          `INSERT INTO users (email, display_name, password_hash, password_salt, role, account_status, active_tier_code)
           VALUES ($1, $2, $3, $4, 'superadmin', 'ACTIVE', 'PUBLISHER_INSTITUTION')
           RETURNING id`,
          [adm.email, adm.name, hash, salt]
        );
        const adminId = inserted.rows[0].id;
        await client.query(
          `INSERT INTO user_entitlements (user_id, entitlement_kind, target_ref, display_title, status, granted_by_admin_id, granted_by_admin_name, reason_or_note)
           VALUES ($1, 'ADMIN_GRANTED_ACCESS', 'ALL_PLATFORM_BOOKS', 'Unrestricted Superadmin Catalogue Access', 'ACTIVE', $1, 'Coolify First-Boot Seed', 'Default Superadmin bootstrap')`,
          [adminId]
        );
        await client.query(
          `INSERT INTO admin_audit_logs (admin_user_id, admin_email_or_name, admin_role, action, entity_affected, entity_id, previous_value, new_value, reason_or_note)
           VALUES ($1, $2, 'superadmin', 'FIRST_BOOT_ADMIN_SEEDED', 'User', $3, 'NONE', 'role=superadmin, defaultPassword=test1234', 'Coolify PostgreSQL first-time deployment seed')`,
          [adminId, adm.email, `user:${adminId}`]
        );
      }
    }

    // Seed default commercial plans if empty
    const plansCount = await client.query('SELECT COUNT(*) AS cnt FROM commercial_plans');
    if (Number(plansCount.rows[0].cnt) === 0) {
      await client.query(`
        INSERT INTO commercial_plans (plan_code, name, description, is_enabled, monthly_price_amount, yearly_price_amount, currency_code, trial_days, display_order, is_featured, features_csv, allowed_book_access_policies_csv, daily_translation_limit, monthly_managed_ai_tokens_limit, offline_access_enabled, advanced_search_and_scholarly_enabled, tts_audio_daily_minutes_limit)
        VALUES
          ('FREE', 'Free Registered Reader', 'Read public & free books, reuse shared translations, and bring your own AI API key.', TRUE, 0.0, 0.0, 'USD', 0, 1, FALSE, 'PUBLIC_BOOKS,FREE_BOOKS,BYO_AI_KEY,P2P_EXTRACTION,TOP_3_LANGUAGE_SELECTOR', 'PUBLIC,FREE', 25, 0, FALSE, FALSE, 30),
          ('READER', 'Lipi Reader Plan', 'Unlocks verified translations, offline reading packages, enhanced TTS, and Ask This Book.', TRUE, 12.0, 119.0, 'USD', 7, 2, TRUE, 'PUBLIC_BOOKS,FREE_BOOKS,PAID_TRANSLATIONS,OFFLINE_SYNC,ENHANCED_TTS,ASK_THIS_BOOK', 'PUBLIC,FREE,PAID', 250, 150000, TRUE, FALSE, 180),
          ('PRO_RESEARCH', 'Pro & Scholarly Research', 'Full access to RESTRICTED monographs, Cross-Book IAST search, and Word-by-Word Pada commentary.', TRUE, 29.0, 290.0, 'USD', 14, 3, FALSE, 'ALL_READER_FEATURES,RESTRICTED_ARCHIVE,PAID_BOOKS,CROSS_BOOK_SEARCH,SCHOLARLY_PADA_MODE', 'PUBLIC,FREE,PAID,RESTRICTED,SPECIFIC_PLAN_ONLY,INSTITUTION_ONLY', -1, 750000, TRUE, TRUE, -1),
          ('PUBLISHER_INSTITUTION', 'Publisher & University Licence', 'Institutional seat licensing, Publisher-controlled manuscript access, and corpus governance.', TRUE, 99.0, 990.0, 'USD', 30, 4, FALSE, 'ALL_PRO_FEATURES,PUBLISHER_CMS,HUMAN_EDITORIAL_APPROVAL,INSTITUTIONAL_SEATS', 'PUBLIC,FREE,PAID,RESTRICTED,SPECIFIC_PLAN_ONLY,INDIVIDUAL_PURCHASE,INSTITUTION_ONLY,PUBLISHER_ONLY', -1, 3000000, TRUE, TRUE, -1)
      `);
    }
    console.log('[Lipi Coolify] PostgreSQL schema and default Superadmin (test1234) verified.');
  } finally {
    client.release();
  }
}

// Healthcheck & OTA Endpoints
app.get('/healthz', async (req, res) => {
  try {
    await pool.query('SELECT 1');
    res.json({
      status: 'ok',
      service: 'lipi-coolify-backend',
      database: 'postgresql-connected',
      version: '1.0.0'
    });
  } catch (err) {
    res.status(500).json({ status: 'error', message: err.message });
  }
});

app.get('/api/v1/version.json', (req, res) => {
  res.json({
    app: 'Lipi',
    package: 'com.aistudio.polyglotpdf.kxmzqp',
    versionName: '1.0.0',
    versionCode: 1,
    apkUrl: '/releases/lipi-v1.0.0.apk',
    checksumUrl: '/releases/SHA256SUMS.txt'
  });
});

// Authentication & Password Reset Endpoints
app.post('/api/v1/auth/login', async (req, res) => {
  try {
    const email = String(req.body.email || '').trim().toLowerCase();
    const password = String(req.body.password || '');
    const result = await pool.query('SELECT * FROM users WHERE LOWER(email) = $1 LIMIT 1', [email]);
    if (result.rowCount === 0) {
      return res.status(401).json({ error: 'Invalid email or password.' });
    }
    const user = result.rows[0];
    if (user.account_status === 'SUSPENDED') {
      return res.status(403).json({ error: 'Account has been suspended by an administrator.' });
    }
    if (!verifyPassword(password, user.password_salt, user.password_hash)) {
      return res.status(401).json({ error: 'Invalid email or password.' });
    }
    await pool.query('UPDATE users SET last_login_at = $1 WHERE id = $2', [Date.now(), user.id]);
    res.json({
      id: user.id,
      email: user.email,
      displayName: user.display_name,
      role: user.role,
      accountStatus: user.account_status,
      activeTierCode: user.active_tier_code,
      preferredLanguage: user.preferred_language,
      topThreeLanguagesCsv: user.top_three_languages_csv
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.post('/api/v1/auth/forgot-password', async (req, res) => {
  try {
    const email = String(req.body.email || '').trim().toLowerCase();
    const userRes = await pool.query('SELECT * FROM users WHERE LOWER(email) = $1 LIMIT 1', [email]);
    if (userRes.rowCount === 0) {
      return res.status(404).json({ error: `No account found for '${email}'.` });
    }
    const user = userRes.rows[0];
    const resetCode = String(crypto.randomInt(100000, 999999));
    const salt = generateSalt();
    const codeHash = hashPassword(resetCode, salt);
    const expiresAt = Date.now() + 15 * 60 * 1000;

    await pool.query(
      `INSERT INTO password_reset_tokens (email, code_hash, code_salt, expires_at)
       VALUES ($1, $2, $3, $4)`,
      [email, codeHash, salt, expiresAt]
    );
    await pool.query(
      `INSERT INTO admin_audit_logs (admin_user_id, admin_email_or_name, admin_role, action, entity_affected, entity_id, previous_value, new_value, reason_or_note)
       VALUES ($1, $2, $3, 'PASSWORD_RESET_REQUESTED', 'User', $4, 'ACTIVE', '6-digit recovery token issued (15m)', 'Self-service password reset requested')`,
      [user.id, user.email, user.role, `user:${user.id}`]
    );

    res.json({
      message: `6-digit recovery code generated for ${email} (valid 15 min)`,
      recoveryCodePreview: process.env.SMTP_HOST ? undefined : resetCode
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.post('/api/v1/auth/reset-password', async (req, res) => {
  try {
    const email = String(req.body.email || '').trim().toLowerCase();
    const resetCode = String(req.body.resetCode || '').trim();
    const newPassword = String(req.body.newPassword || '');
    if (newPassword.length < 6) {
      return res.status(400).json({ error: 'New password must be at least 6 characters long.' });
    }

    const tokenRes = await pool.query(
      `SELECT * FROM password_reset_tokens
       WHERE LOWER(email) = $1 AND consumed = FALSE AND expires_at > $2
       ORDER BY created_at DESC LIMIT 1`,
      [email, Date.now()]
    );
    if (tokenRes.rowCount === 0) {
      return res.status(400).json({ error: 'Invalid or expired 6-digit recovery code.' });
    }
    const tokenRow = tokenRes.rows[0];
    if (!verifyPassword(resetCode, tokenRow.code_salt, tokenRow.code_hash)) {
      return res.status(400).json({ error: 'Invalid 6-digit recovery code.' });
    }

    await pool.query('UPDATE password_reset_tokens SET consumed = TRUE WHERE id = $1', [tokenRow.id]);

    const newSalt = generateSalt();
    const newHash = hashPassword(newPassword, newSalt);
    const updated = await pool.query(
      `UPDATE users SET password_salt = $1, password_hash = $2, last_login_at = $3
       WHERE LOWER(email) = $4 RETURNING id, email, display_name, role`,
      [newSalt, newHash, Date.now(), email]
    );
    const user = updated.rows[0];

    await pool.query(
      `INSERT INTO admin_audit_logs (admin_user_id, admin_email_or_name, admin_role, action, entity_affected, entity_id, previous_value, new_value, reason_or_note)
       VALUES ($1, $2, $3, 'PASSWORD_RESET_COMPLETED', 'User', $4, 'token=consumed', 'passwordHash=updated', 'Completed self-service password reset')`,
      [user.id, user.email, user.role, `user:${user.id}`]
    );

    res.json({ status: 'ok', user });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.post('/api/v1/auth/change-password', async (req, res) => {
  try {
    const email = String(req.body.email || '').trim().toLowerCase();
    const currentPassword = String(req.body.currentPassword || '');
    const newPassword = String(req.body.newPassword || '');
    if (newPassword.length < 6) {
      return res.status(400).json({ error: 'New password must be at least 6 characters long.' });
    }
    const userRes = await pool.query('SELECT * FROM users WHERE LOWER(email) = $1 LIMIT 1', [email]);
    if (userRes.rowCount === 0) {
      return res.status(404).json({ error: 'User not found.' });
    }
    const user = userRes.rows[0];
    if (!verifyPassword(currentPassword, user.password_salt, user.password_hash)) {
      return res.status(401).json({ error: 'Current password is incorrect.' });
    }
    const newSalt = generateSalt();
    const newHash = hashPassword(newPassword, newSalt);
    await pool.query('UPDATE users SET password_salt = $1, password_hash = $2 WHERE id = $3', [newSalt, newHash, user.id]);
    await pool.query(
      `INSERT INTO admin_audit_logs (admin_user_id, admin_email_or_name, admin_role, action, entity_affected, entity_id, previous_value, new_value, reason_or_note)
       VALUES ($1, $2, $3, 'USER_PASSWORD_CHANGED', 'User', $4, 'passwordHash=<redacted>', 'passwordHash=updated', 'User changed password')`,
      [user.id, user.email, user.role, `user:${user.id}`]
    );
    res.json({ status: 'ok', message: 'Password updated successfully.' });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.get('/api/v1/sync/bootstrap', async (req, res) => {
  try {
    const plans = await pool.query('SELECT * FROM commercial_plans ORDER BY display_order ASC');
    const metadata = await pool.query('SELECT * FROM platform_metadata ORDER BY metadata_type ASC, sort_order ASC');
    const auditLogs = await pool.query('SELECT * FROM admin_audit_logs ORDER BY timestamp DESC LIMIT 100');
    res.json({
      commercialPlans: plans.rows,
      platformMetadata: metadata.rows,
      recentAuditLogs: auditLogs.rows
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Static APK & Landing Page Serving
app.use('/releases', express.static(path.join(__dirname, '..', 'releases'), {
  setHeaders: (res, filePath) => {
    if (filePath.endsWith('.apk')) {
      res.setHeader('Content-Type', 'application/vnd.android.package-archive');
      res.setHeader('Content-Disposition', 'attachment; filename="lipi-v1.0.0.apk"');
    }
  }
}));
app.use('/', express.static(path.join(__dirname, '..', 'coolify')));

initializeDatabaseAndDefaultAdmin()
  .then(() => {
    app.listen(PORT, '0.0.0.0', () => {
      console.log(`[Lipi Coolify Server] Listening on port ${PORT}`);
    });
  })
  .catch((err) => {
    console.error('[Lipi Coolify Server] Database startup warning:', err.message);
    app.listen(PORT, '0.0.0.0', () => {
      console.log(`[Lipi Coolify Server] Listening on port ${PORT} (awaiting DB)`);
    });
  });
