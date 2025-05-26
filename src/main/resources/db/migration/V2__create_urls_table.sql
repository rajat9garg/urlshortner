-- Create URLs table for URL shortening service
CREATE TABLE urls (
    id BIGSERIAL PRIMARY KEY,
    short_code VARCHAR(10) NOT NULL UNIQUE,
    original_url TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    total_clicks BIGINT NOT NULL DEFAULT 0
);

-- Index for fast lookup by short_code
CREATE INDEX idx_urls_short_code ON urls(short_code);
