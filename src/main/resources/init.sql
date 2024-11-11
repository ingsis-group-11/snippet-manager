CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS languages (
                                         id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                         language VARCHAR(255) NOT NULL,
                                         extension VARCHAR(255) NOT NULL
);

-- Insert data only if it does not exist
INSERT INTO languages (language, extension)
SELECT 'PrintScript 1.1', 'prs'
WHERE NOT EXISTS (
    SELECT 1 FROM languages WHERE language = 'PrintScript 1.1' AND extension = 'prs'
);

INSERT INTO languages (language, extension)
SELECT 'PrintScript 1.0', 'prs'
WHERE NOT EXISTS (
    SELECT 1 FROM languages WHERE language = 'PrintScript 1.0' AND extension = 'prs'
);

