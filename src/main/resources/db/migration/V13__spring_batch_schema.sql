-- MySQL dump 10.13  Distrib 8.0.42, for Win64 (x86_64)
--
-- Host: localhost    Database: espacogeekdb
-- ------------------------------------------------------
-- Server version	8.0.42

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
-- Table structure for table `batch_job_execution`
--

DROP TABLE IF EXISTS `batch_job_execution`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `batch_job_execution`
(
    `JOB_EXECUTION_ID` bigint NOT NULL,
    `VERSION`          bigint        DEFAULT NULL,
    `JOB_INSTANCE_ID`  bigint NOT NULL,
    `CREATE_TIME`      datetime(6) NOT NULL,
    `START_TIME`       datetime(6) DEFAULT NULL,
    `END_TIME`         datetime(6) DEFAULT NULL,
    `STATUS`           varchar(10)   DEFAULT NULL,
    `EXIT_CODE`        varchar(2500) DEFAULT NULL,
    `EXIT_MESSAGE`     varchar(2500) DEFAULT NULL,
    `LAST_UPDATED`     datetime(6) DEFAULT NULL,
    PRIMARY KEY (`JOB_EXECUTION_ID`),
    KEY                `JOB_INST_EXEC_FK` (`JOB_INSTANCE_ID`),
    CONSTRAINT `JOB_INST_EXEC_FK` FOREIGN KEY (`JOB_INSTANCE_ID`) REFERENCES `batch_job_instance` (`JOB_INSTANCE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `batch_job_execution_context`
--

DROP TABLE IF EXISTS `batch_job_execution_context`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `batch_job_execution_context`
(
    `JOB_EXECUTION_ID`   bigint        NOT NULL,
    `SHORT_CONTEXT`      varchar(2500) NOT NULL,
    `SERIALIZED_CONTEXT` text,
    PRIMARY KEY (`JOB_EXECUTION_ID`),
    CONSTRAINT `JOB_EXEC_CTX_FK` FOREIGN KEY (`JOB_EXECUTION_ID`) REFERENCES `batch_job_execution` (`JOB_EXECUTION_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `batch_job_execution_params`
--

DROP TABLE IF EXISTS `batch_job_execution_params`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `batch_job_execution_params`
(
    `JOB_EXECUTION_ID` bigint       NOT NULL,
    `PARAMETER_NAME`   varchar(100) NOT NULL,
    `PARAMETER_TYPE`   varchar(100) NOT NULL,
    `PARAMETER_VALUE`  varchar(2500) DEFAULT NULL,
    `IDENTIFYING`      char(1)      NOT NULL,
    KEY                `JOB_EXEC_PARAMS_FK` (`JOB_EXECUTION_ID`),
    CONSTRAINT `JOB_EXEC_PARAMS_FK` FOREIGN KEY (`JOB_EXECUTION_ID`) REFERENCES `batch_job_execution` (`JOB_EXECUTION_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `batch_job_execution_seq`
--

DROP TABLE IF EXISTS `batch_job_execution_seq`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `batch_job_execution_seq`
(
    `ID`         bigint  NOT NULL,
    `UNIQUE_KEY` char(1) NOT NULL,
    UNIQUE KEY `UNIQUE_KEY_UN` (`UNIQUE_KEY`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `batch_job_instance`
--

DROP TABLE IF EXISTS `batch_job_instance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `batch_job_instance`
(
    `JOB_INSTANCE_ID` bigint       NOT NULL,
    `VERSION`         bigint DEFAULT NULL,
    `JOB_NAME`        varchar(100) NOT NULL,
    `JOB_KEY`         varchar(32)  NOT NULL,
    PRIMARY KEY (`JOB_INSTANCE_ID`),
    UNIQUE KEY `JOB_INST_UN` (`JOB_NAME`,`JOB_KEY`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `batch_job_seq`
--

DROP TABLE IF EXISTS `batch_job_seq`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `batch_job_seq`
(
    `ID`         bigint  NOT NULL,
    `UNIQUE_KEY` char(1) NOT NULL,
    UNIQUE KEY `UNIQUE_KEY_UN` (`UNIQUE_KEY`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `batch_step_execution`
--

DROP TABLE IF EXISTS `batch_step_execution`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `batch_step_execution`
(
    `STEP_EXECUTION_ID`  bigint       NOT NULL,
    `VERSION`            bigint       NOT NULL,
    `STEP_NAME`          varchar(100) NOT NULL,
    `JOB_EXECUTION_ID`   bigint       NOT NULL,
    `CREATE_TIME`        datetime(6) NOT NULL,
    `START_TIME`         datetime(6) DEFAULT NULL,
    `END_TIME`           datetime(6) DEFAULT NULL,
    `STATUS`             varchar(10)   DEFAULT NULL,
    `COMMIT_COUNT`       bigint        DEFAULT NULL,
    `READ_COUNT`         bigint        DEFAULT NULL,
    `FILTER_COUNT`       bigint        DEFAULT NULL,
    `WRITE_COUNT`        bigint        DEFAULT NULL,
    `READ_SKIP_COUNT`    bigint        DEFAULT NULL,
    `WRITE_SKIP_COUNT`   bigint        DEFAULT NULL,
    `PROCESS_SKIP_COUNT` bigint        DEFAULT NULL,
    `ROLLBACK_COUNT`     bigint        DEFAULT NULL,
    `EXIT_CODE`          varchar(2500) DEFAULT NULL,
    `EXIT_MESSAGE`       varchar(2500) DEFAULT NULL,
    `LAST_UPDATED`       datetime(6) DEFAULT NULL,
    PRIMARY KEY (`STEP_EXECUTION_ID`),
    KEY                  `JOB_EXEC_STEP_FK` (`JOB_EXECUTION_ID`),
    CONSTRAINT `JOB_EXEC_STEP_FK` FOREIGN KEY (`JOB_EXECUTION_ID`) REFERENCES `batch_job_execution` (`JOB_EXECUTION_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `batch_step_execution_context`
--

DROP TABLE IF EXISTS `batch_step_execution_context`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `batch_step_execution_context`
(
    `STEP_EXECUTION_ID`  bigint        NOT NULL,
    `SHORT_CONTEXT`      varchar(2500) NOT NULL,
    `SERIALIZED_CONTEXT` text,
    PRIMARY KEY (`STEP_EXECUTION_ID`),
    CONSTRAINT `STEP_EXEC_CTX_FK` FOREIGN KEY (`STEP_EXECUTION_ID`) REFERENCES `batch_step_execution` (`STEP_EXECUTION_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `batch_step_execution_seq`
--

DROP TABLE IF EXISTS `batch_step_execution_seq`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `batch_step_execution_seq`
(
    `ID`         bigint  NOT NULL,
    `UNIQUE_KEY` char(1) NOT NULL,
    UNIQUE KEY `UNIQUE_KEY_UN` (`UNIQUE_KEY`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping events for database 'espacogeekdb'
--

--
-- Dumping routines for database 'espacogeekdb'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-02-17 19:05:53
