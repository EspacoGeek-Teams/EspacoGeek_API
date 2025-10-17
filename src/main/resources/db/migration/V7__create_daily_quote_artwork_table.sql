-- Create table for storing daily quote and artwork
CREATE TABLE IF NOT EXISTS `daily_quote_artwork` (
  `id_daily_quote_artwork` int NOT NULL AUTO_INCREMENT,
  `quote` varchar(2000) DEFAULT NULL,
  `author` varchar(255) DEFAULT NULL,
  `url_artwork` varchar(500) DEFAULT NULL,
  `date` date NOT NULL,
  `created_at` datetime(6) DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id_daily_quote_artwork`),
  UNIQUE KEY `UK_date` (`date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
