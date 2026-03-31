-- Drop the foreign key before altering column types
ALTER TABLE `medias` DROP FOREIGN KEY `fk_medias_media_status`;

-- Change media_status.id from BIGINT to INT
ALTER TABLE `media_status` MODIFY COLUMN `id` int NOT NULL AUTO_INCREMENT;

-- Change medias.status_id from BIGINT to INT
ALTER TABLE `medias` MODIFY COLUMN `status_id` int DEFAULT NULL;

-- Re-add the foreign key
ALTER TABLE `medias`
  ADD CONSTRAINT `fk_medias_media_status` FOREIGN KEY (`status_id`) REFERENCES `media_status` (`id`);
