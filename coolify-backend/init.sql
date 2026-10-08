-- ============================================================================
-- Lipi Knowledge Commons — PostgreSQL 16 Schema & First-Boot Seed
-- Automatically executed on Coolify PostgreSQL first deployment
-- ============================================================================

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email TEXT NOT NULL UNIQUE,
    display_name TEXT NOT NULL,
    password_hash TEXT NOT NULL,
    password_salt TEXT NOT NULL,
    role TEXT NOT NULL DEFAULT 'user',
    account_status TEXT NOT NULL DEFAULT 'ACTIVE',
    preferred_language TEXT NOT NULL DEFAULT 'Gujarati',
    top_three_languages_csv TEXT NOT NULL DEFAULT 'Gujarati,Hindi,English',
    active_tier_code TEXT NOT NULL DEFAULT 'FREE',
    institution_code TEXT NOT NULL DEFAULT '',
    has_accepted_contributor_license BOOLEAN NOT NULL DEFAULT TRUE,
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    last_login_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT
);

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    email TEXT NOT NULL,
    code_hash TEXT NOT NULL,
    code_salt TEXT NOT NULL,
    expires_at BIGINT NOT NULL,
    consumed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT
);
CREATE INDEX IF NOT EXISTS idx_password_reset_email ON password_reset_tokens(email);

CREATE TABLE IF NOT EXISTS commercial_plans (
    id BIGSERIAL PRIMARY KEY,
    plan_code TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    description TEXT NOT NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    monthly_price_amount DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    yearly_price_amount DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    currency_code TEXT NOT NULL DEFAULT 'USD',
    trial_days INTEGER NOT NULL DEFAULT 0,
    display_order INTEGER NOT NULL DEFAULT 1,
    is_featured BOOLEAN NOT NULL DEFAULT FALSE,
    features_csv TEXT NOT NULL DEFAULT '',
    allowed_book_access_policies_csv TEXT NOT NULL DEFAULT 'PUBLIC,FREE',
    daily_translation_limit INTEGER NOT NULL DEFAULT 25,
    monthly_managed_ai_tokens_limit INTEGER NOT NULL DEFAULT 0,
    offline_access_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    advanced_search_and_scholarly_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    tts_audio_daily_minutes_limit INTEGER NOT NULL DEFAULT 30,
    updated_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT
);

CREATE TABLE IF NOT EXISTS user_entitlements (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    entitlement_kind TEXT NOT NULL,
    target_ref TEXT NOT NULL,
    display_title TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    granted_by_admin_id BIGINT,
    granted_by_admin_name TEXT NOT NULL DEFAULT 'System',
    reason_or_note TEXT NOT NULL DEFAULT '',
    granted_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    expires_at BIGINT,
    revoked_at BIGINT
);

CREATE TABLE IF NOT EXISTS books (
    id BIGSERIAL PRIMARY KEY,
    title TEXT NOT NULL,
    subtitle TEXT NOT NULL DEFAULT '',
    author_or_source TEXT NOT NULL DEFAULT '',
    contributors TEXT NOT NULL DEFAULT '',
    publisher_name TEXT NOT NULL DEFAULT 'Lipi Commons',
    description TEXT NOT NULL DEFAULT '',
    original_language TEXT NOT NULL DEFAULT 'Sanskrit',
    category TEXT NOT NULL DEFAULT 'Classical Wisdom',
    genres_csv TEXT NOT NULL DEFAULT 'Philosophy',
    tags_csv TEXT NOT NULL DEFAULT 'Bilingual',
    work_id TEXT NOT NULL DEFAULT 'WORK-DEFAULT',
    edition_label TEXT NOT NULL DEFAULT '1st Digital Critical Edition',
    isbn_or_identifier TEXT NOT NULL DEFAULT '',
    publication_year TEXT NOT NULL DEFAULT '2026',
    access_policy TEXT NOT NULL DEFAULT 'PUBLIC',
    required_plan_code TEXT NOT NULL DEFAULT '',
    rights_category TEXT NOT NULL DEFAULT 'PUBLIC_DOMAIN',
    publication_status TEXT NOT NULL DEFAULT 'PUBLISHED',
    price_display TEXT NOT NULL DEFAULT 'Free',
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT
);

CREATE TABLE IF NOT EXISTS paragraph_translations (
    id BIGSERIAL PRIMARY KEY,
    book_id BIGINT NOT NULL,
    paragraph_id BIGINT NOT NULL,
    source_language TEXT NOT NULL,
    target_language TEXT NOT NULL,
    translated_text TEXT NOT NULL,
    transliteration_or_notes TEXT NOT NULL DEFAULT '',
    version_number INTEGER NOT NULL DEFAULT 1,
    is_active_version BOOLEAN NOT NULL DEFAULT TRUE,
    provider_used TEXT NOT NULL DEFAULT 'Shared Corpus',
    model_used TEXT NOT NULL DEFAULT 'Canonical v1',
    review_status TEXT NOT NULL DEFAULT 'AI_GENERATED',
    access_policy TEXT NOT NULL DEFAULT 'PUBLIC',
    created_by_user_id BIGINT NOT NULL DEFAULT 0,
    translator_display_name TEXT NOT NULL DEFAULT 'Lipi Scholar',
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT
);

CREATE TABLE IF NOT EXISTS platform_metadata (
    id BIGSERIAL PRIMARY KEY,
    metadata_type TEXT NOT NULL,
    key_code TEXT NOT NULL,
    display_label TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    sort_order INTEGER NOT NULL DEFAULT 1,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    extra_attributes_json TEXT NOT NULL DEFAULT '{}',
    updated_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    UNIQUE(metadata_type, key_code)
);

CREATE TABLE IF NOT EXISTS admin_audit_logs (
    id BIGSERIAL PRIMARY KEY,
    admin_user_id BIGINT NOT NULL,
    admin_email_or_name TEXT NOT NULL,
    admin_role TEXT NOT NULL,
    action TEXT NOT NULL,
    entity_affected TEXT NOT NULL,
    entity_id TEXT NOT NULL,
    previous_value TEXT NOT NULL DEFAULT '',
    new_value TEXT NOT NULL DEFAULT '',
    reason_or_note TEXT NOT NULL DEFAULT '',
    device_or_context_info TEXT NOT NULL DEFAULT 'Coolify Backend API',
    timestamp BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT
);
