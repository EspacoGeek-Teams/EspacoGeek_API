CREATE INDEX idx_title ON alternative_titles (name_title(191));

ALTER TABLE medias DROP INDEX idx_name_media;
CREATE INDEX idx_name ON medias (name_media(191));
