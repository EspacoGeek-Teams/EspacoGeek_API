-- Add performance indexes for frequently searched text columns.
-- medias.name_media: searched by title in findMediaByNameOrAlternativeTitleAndMediaCategory
CREATE INDEX idx_name_media ON medias(name_media);

-- alternative_titles.name_title: searched by alternative title in the same query
CREATE INDEX idx_name_title ON alternative_titles(name_title);

-- jwt_tokens.expires_at: used in token cleanup/expiry validation queries
CREATE INDEX idx_jwt_expires_at ON jwt_tokens(expires_at);
