-- init.sql
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS languages (
                                         id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                         language VARCHAR(255) NOT NULL,
                                         extension VARCHAR(255) NOT NULL
);

INSERT INTO languages (language, extension) VALUES
                                                ('PrintScript 1.1', 'prs'),
                                                ('PrintScript 1.0', 'prs');