-- Table for email verification tokens
CREATE TABLE `email_verification_tokens` (
  `id_token` INT NOT NULL AUTO_INCREMENT,
  `token` VARCHAR(255) NOT NULL,
  `user_id` INT NOT NULL,
  `token_type` VARCHAR(50) NOT NULL,
  `new_email` VARCHAR(50) NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `expires_at` TIMESTAMP NOT NULL,
  `used` BOOLEAN DEFAULT FALSE,
  PRIMARY KEY (`id_token`),
  UNIQUE KEY `UK_token` (`token`),
  KEY `FK_email_verification_user` (`user_id`),
  CONSTRAINT `FK_email_verification_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id_user`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Add index for efficient token lookup
CREATE INDEX `idx_token_type` ON `email_verification_tokens` (`token_type`);
CREATE INDEX `idx_expires_at` ON `email_verification_tokens` (`expires_at`);
