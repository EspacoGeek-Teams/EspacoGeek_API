-- Add custom_status_id FK column to user_media_list so a library entry can be placed
-- in one of the user's custom status lists (user_custom_statuses).
ALTER TABLE `user_media_list`
    ADD COLUMN IF NOT EXISTS `custom_status_id` INT DEFAULT NULL,
    ADD CONSTRAINT `FK_user_media_list_custom_status`
        FOREIGN KEY (`custom_status_id`) REFERENCES `user_custom_statuses` (`id`) ON DELETE SET NULL;
