-- Create table to store JWT tokens for multi-device support
CREATE TABLE IF NOT EXISTS `jwt_tokens` (
  `id_token` INT NOT NULL AUTO_INCREMENT,
  `token` VARCHAR(500) NOT NULL,
  `id_user` INT NOT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `expires_at` TIMESTAMP NOT NULL,
  `device_info` VARCHAR(255),
  PRIMARY KEY (`id_token`),
  UNIQUE KEY `UK_token` (`token`),
  KEY `idx_user_token` (`id_user`, `token`),
  CONSTRAINT `FK_jwt_tokens_user` FOREIGN KEY (`id_user`) REFERENCES `users` (`id_user`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
