CREATE TABLE IF NOT EXISTS `user_media_list` (
  `id_user_media_list` INT NOT NULL AUTO_INCREMENT,
  `user_id` INT NOT NULL,
  `media_id` INT NOT NULL,
  `media_type` VARCHAR(50) NOT NULL,
  `status` VARCHAR(50) DEFAULT NULL,
  `score` FLOAT DEFAULT NULL,
  `progress` INT DEFAULT NULL,
  PRIMARY KEY (`id_user_media_list`),
  KEY `idx_user_media_list_user` (`user_id`),
  KEY `idx_user_media_list_media` (`media_id`),
  CONSTRAINT `FK_user_media_list_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id_user`) ON DELETE CASCADE,
  CONSTRAINT `FK_user_media_list_media` FOREIGN KEY (`media_id`) REFERENCES `medias` (`id_media`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
