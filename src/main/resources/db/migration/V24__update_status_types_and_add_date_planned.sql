-- Add date_planned column to user_media_list
ALTER TABLE `user_media_list`
    ADD COLUMN IF NOT EXISTS `date_planned` DATETIME DEFAULT NULL;

-- Update status values in media_status table: rename WATCHING -> IN_PROGRESS and PLAN_TO_WATCH -> PLANNING
UPDATE `media_status` SET `name` = 'IN_PROGRESS' WHERE `name` = 'WATCHING';
UPDATE `media_status` SET `name` = 'PLANNING' WHERE `name` = 'PLAN_TO_WATCH';

-- Insert PAUSED status if it does not already exist
INSERT INTO `media_status` (`name`) SELECT 'PAUSED' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `media_status` WHERE `name` = 'PAUSED');

-- Update existing user_media_list entries: migrate old status strings to new values
UPDATE `user_media_list` SET `status` = 'IN_PROGRESS' WHERE UPPER(`status`) = 'WATCHING';
UPDATE `user_media_list` SET `status` = 'PLANNING' WHERE UPPER(`status`) = 'PLAN_TO_WATCH';

-- Set date_planned for all existing PLANNING entries that don't have it yet
UPDATE `user_media_list` SET `date_planned` = NOW() WHERE UPPER(`status`) = 'PLANNING' AND `date_planned` IS NULL;

-- Create user_custom_statuses table
CREATE TABLE IF NOT EXISTS `user_custom_statuses` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL,
    `user_id` INT NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_user_custom_statuses_user` (`user_id`),
    CONSTRAINT `FK_user_custom_statuses_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id_user`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
