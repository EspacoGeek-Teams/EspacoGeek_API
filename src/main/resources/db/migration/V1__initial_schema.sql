-- MySQL dump 10.13  Distrib 8.0.34, for Win64 (x86_64)
--
-- ------------------------------------------------------
-- Server version	8.4.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `alternative_titles`
--

DROP TABLE IF EXISTS `alternative_titles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `alternative_titles` (
  `id_alternative_title` int NOT NULL AUTO_INCREMENT,
  `name_title` varchar(1000) DEFAULT NULL,
  `id_media` int DEFAULT NULL,
  PRIMARY KEY (`id_alternative_title`),
  KEY `FK83l2mt6jjneoh5sjaplq383h8` (`id_media`),
  CONSTRAINT `FK83l2mt6jjneoh5sjaplq383h8` FOREIGN KEY (`id_media`) REFERENCES `medias` (`id_media`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `api_keys`
--

DROP TABLE IF EXISTS `api_keys`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `api_keys` (
  `id_api_key` int NOT NULL,
  `api_key` varchar(255) DEFAULT NULL,
  `name_api` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id_api_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `companies`
--

DROP TABLE IF EXISTS `companies`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `companies` (
  `id_company` int NOT NULL AUTO_INCREMENT,
  `name_company` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id_company`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `externals_references`
--

DROP TABLE IF EXISTS `externals_references`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `externals_references` (
  `id_external_reference` int NOT NULL AUTO_INCREMENT,
  `reference` varchar(255) DEFAULT NULL,
  `medias_id_media` int DEFAULT NULL,
  `type_reference` int NOT NULL,
  PRIMARY KEY (`id_external_reference`),
  KEY `FK8pknhg4ms458dlxwc29rpnhx9` (`medias_id_media`),
  KEY `FK8035glbs4hk83dgecytsinse0` (`type_reference`),
  CONSTRAINT `FK8035glbs4hk83dgecytsinse0` FOREIGN KEY (`type_reference`) REFERENCES `type_reference` (`id_type_reference`),
  CONSTRAINT `FK8pknhg4ms458dlxwc29rpnhx9` FOREIGN KEY (`medias_id_media`) REFERENCES `medias` (`id_media`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `genres`
--

DROP TABLE IF EXISTS `genres`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `genres` (
  `id_genre` int NOT NULL AUTO_INCREMENT,
  `name_genre` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id_genre`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `media_categories`
--

DROP TABLE IF EXISTS `media_categories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `media_categories` (
  `id_media_category` int NOT NULL AUTO_INCREMENT,
  `type_category` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id_media_category`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `medias`
--

DROP TABLE IF EXISTS `medias`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `medias` (
  `id_media` int NOT NULL AUTO_INCREMENT,
  `about` varchar(10000) DEFAULT NULL,
  `url_banner` varchar(255) DEFAULT NULL,
  `url_cover` varchar(255) DEFAULT NULL,
  `episode_length_in_minutes` int DEFAULT NULL,
  `id_category` int DEFAULT NULL,
  `name_media` varchar(255) DEFAULT NULL,
  `episode_count` int DEFAULT NULL,
  `update_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id_media`),
  KEY `fk_medias_media_categories_idx` (`id_category`),
  CONSTRAINT `fk_medias_media_categories` FOREIGN KEY (`id_category`) REFERENCES `media_categories` (`id_media_category`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `medias_has_companies`
--

DROP TABLE IF EXISTS `medias_has_companies`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `medias_has_companies` (
  `medias_id_media` int NOT NULL,
  `companies_id_company` int NOT NULL,
  KEY `FKf8a41q6lcdpn5yrklkm5qhh16` (`companies_id_company`),
  KEY `FKpwr14py6t7ggu05fmba75swl3` (`medias_id_media`),
  CONSTRAINT `FKf8a41q6lcdpn5yrklkm5qhh16` FOREIGN KEY (`companies_id_company`) REFERENCES `companies` (`id_company`),
  CONSTRAINT `FKpwr14py6t7ggu05fmba75swl3` FOREIGN KEY (`medias_id_media`) REFERENCES `medias` (`id_media`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `medias_has_genres`
--

DROP TABLE IF EXISTS `medias_has_genres`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `medias_has_genres` (
  `medias_id_media` int NOT NULL,
  `genres_id_genre` int NOT NULL,
  KEY `FKsd2uj0cp17vny7ai2t750tiw4` (`genres_id_genre`),
  KEY `FKic2exdmk8o9p04rvkjrnfu7ru` (`medias_id_media`),
  CONSTRAINT `FKic2exdmk8o9p04rvkjrnfu7ru` FOREIGN KEY (`medias_id_media`) REFERENCES `medias` (`id_media`),
  CONSTRAINT `FKsd2uj0cp17vny7ai2t750tiw4` FOREIGN KEY (`genres_id_genre`) REFERENCES `genres` (`id_genre`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `medias_has_people`
--

DROP TABLE IF EXISTS `medias_has_people`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `medias_has_people` (
  `medias_id_media` int NOT NULL,
  `people_id_person` int NOT NULL,
  KEY `FKhvnnh4jpqffihwcu35ufnq8qy` (`people_id_person`),
  KEY `FK3vcbw1pwg5d36jme8uf2atd2c` (`medias_id_media`),
  CONSTRAINT `FK3vcbw1pwg5d36jme8uf2atd2c` FOREIGN KEY (`medias_id_media`) REFERENCES `medias` (`id_media`),
  CONSTRAINT `FKhvnnh4jpqffihwcu35ufnq8qy` FOREIGN KEY (`people_id_person`) REFERENCES `people` (`id_person`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `people`
--

DROP TABLE IF EXISTS `people`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `people` (
  `id_person` int NOT NULL AUTO_INCREMENT,
  `name_person` varchar(255) DEFAULT NULL,
  `type_person` int DEFAULT NULL,
  PRIMARY KEY (`id_person`),
  KEY `FK94gmvt5a8i1b0e6028gsvdbdn` (`type_person`),
  CONSTRAINT `FK94gmvt5a8i1b0e6028gsvdbdn` FOREIGN KEY (`type_person`) REFERENCES `types_person` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `seasons`
--

DROP TABLE IF EXISTS `seasons`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `seasons` (
  `id_season` int NOT NULL AUTO_INCREMENT,
  `about_season` varchar(10000) DEFAULT NULL,
  `air_date` datetime(6) DEFAULT NULL,
  `path_cover` varchar(255) DEFAULT NULL,
  `end_air_date` datetime(6) DEFAULT NULL,
  `episode_count` int DEFAULT NULL,
  `name_season` varchar(255) DEFAULT NULL,
  `season_number` int DEFAULT NULL,
  `medias_id_medias` int DEFAULT NULL,
  PRIMARY KEY (`id_season`),
  KEY `FK5rqeql12uopcxqf61ni31t1nu` (`medias_id_medias`),
  CONSTRAINT `FK5rqeql12uopcxqf61ni31t1nu` FOREIGN KEY (`medias_id_medias`) REFERENCES `medias` (`id_media`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `type_reference`
--

DROP TABLE IF EXISTS `type_reference`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `type_reference` (
  `id_type_reference` int NOT NULL AUTO_INCREMENT,
  `name_reference` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id_type_reference`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `types_person`
--

DROP TABLE IF EXISTS `types_person`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `types_person` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name_type_person` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `types_status`
--

DROP TABLE IF EXISTS `types_status`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `types_status` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name_status` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id_user` int NOT NULL AUTO_INCREMENT,
  `email` varchar(50) NOT NULL,
  `password` varbinary(255) NOT NULL,
  `username` varchar(20) NOT NULL,
  PRIMARY KEY (`id_user`),
  UNIQUE KEY `UK_6dotkott2kjsp8vw4d0m25fb7` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `users_library`
--

DROP TABLE IF EXISTS `users_library`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users_library` (
  `id_user_library` int NOT NULL AUTO_INCREMENT,
  `added_at` datetime(6) DEFAULT NULL,
  `progress` int DEFAULT NULL,
  `id_media` int DEFAULT NULL,
  `status` int DEFAULT NULL,
  `id_user` int DEFAULT NULL,
  PRIMARY KEY (`id_user_library`),
  KEY `FKqcbai82o2ha17y49p8r0746lx` (`id_media`),
  KEY `FK2tb0prugrh9rna7lxdcl09wtm` (`status`),
  KEY `FKblnoj2oh5tfmtdctkgm62s23a` (`id_user`),
  CONSTRAINT `FK2tb0prugrh9rna7lxdcl09wtm` FOREIGN KEY (`status`) REFERENCES `types_status` (`id`),
  CONSTRAINT `FKblnoj2oh5tfmtdctkgm62s23a` FOREIGN KEY (`id_user`) REFERENCES `users` (`id_user`),
  CONSTRAINT `FKqcbai82o2ha17y49p8r0746lx` FOREIGN KEY (`id_media`) REFERENCES `medias` (`id_media`),
  CONSTRAINT `users_library_chk_1` CHECK ((`progress` <= 4))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2024-11-09 22:51:21
