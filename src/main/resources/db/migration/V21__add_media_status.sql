-- Create media_status table
CREATE TABLE IF NOT EXISTS `media_status` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Add status_id foreign key column to medias table
ALTER TABLE `medias`
  ADD COLUMN IF NOT EXISTS `status_id` bigint DEFAULT NULL,
  ADD CONSTRAINT `fk_medias_media_status` FOREIGN KEY (`status_id`) REFERENCES `media_status` (`id`);

-- Update existing media_categories type_category values to match CategoryType enum names
UPDATE `media_categories` SET `type_category` = 'SERIES' WHERE `type_category` = 'TVSerie';
UPDATE `media_categories` SET `type_category` = 'GAME' WHERE `type_category` = 'Game';
UPDATE `media_categories` SET `type_category` = 'VISUAL_NOVEL' WHERE `type_category` = 'Visual Novel';
UPDATE `media_categories` SET `type_category` = 'MOVIE' WHERE `type_category` = 'Movie';
UPDATE `media_categories` SET `type_category` = 'ANIME' WHERE `type_category` = 'Anime Serie';
UPDATE `media_categories` SET `type_category` = 'UNDEFINED' WHERE `type_category` = 'Undefined';
UPDATE `media_categories` SET `type_category` = 'ANIME_MOVIE' WHERE `type_category` = 'Anime Movie';
