CREATE TABLE urls (
    short_code   VARCHAR(255) PRIMARY KEY,
    original_url TEXT         NOT NULL,
    expiration   TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL
);
