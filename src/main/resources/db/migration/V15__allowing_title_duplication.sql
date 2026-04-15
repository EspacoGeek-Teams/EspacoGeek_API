CREATE INDEX idx_title ON alternative_titles (name_title(191));

DROP INDEX IF EXISTS idx_name_media ON medias;
CREATE INDEX idx_name ON medias (name_media(191));
