SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `book_chapter`;
CREATE TABLE `book_chapter` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `book_id` bigint NOT NULL,
  `chapter_title` varchar(255) NOT NULL,
  `sort_order` int NOT NULL DEFAULT 0,
  `word_count` int DEFAULT 0,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_book_sort` (`book_id`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='book chapter';

DROP TABLE IF EXISTS `book_chapter_content`;
CREATE TABLE `book_chapter_content` (
  `chapter_id` bigint NOT NULL,
  `content` longtext,
  PRIMARY KEY (`chapter_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='book chapter content';

DROP TABLE IF EXISTS `book_info`;
CREATE TABLE `book_info` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(255) NOT NULL,
  `author` varchar(100) NOT NULL,
  `cover_url` varchar(500) DEFAULT NULL,
  `original_file_url` varchar(500) DEFAULT NULL,
  `description` text,
  `tags` varchar(500) DEFAULT NULL,
  `publisher` bigint DEFAULT NULL,
  `status` tinyint DEFAULT 0,
  `error_msg` varchar(500) DEFAULT NULL,
  `word_count` int DEFAULT 0,
  `total_score` int DEFAULT 0,
  `rating_count` int DEFAULT 0,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='book info';

DROP TABLE IF EXISTS `book_paragraph_comment`;
CREATE TABLE `book_paragraph_comment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `book_id` bigint NOT NULL,
  `chapter_id` bigint NOT NULL,
  `paragraph_id` varchar(32) NOT NULL,
  `user_id` bigint NOT NULL,
  `content` varchar(1000) NOT NULL,
  `like_count` int DEFAULT 0,
  `parent_id` bigint DEFAULT 0,
  `reply_to_user_id` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_chapter_para` (`chapter_id`, `paragraph_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='paragraph comment';

DROP TABLE IF EXISTS `book_review`;
CREATE TABLE `book_review` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `book_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `rating` int NOT NULL,
  `content` text,
  `like_count` int DEFAULT 0,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_book` (`book_id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='book review';

DROP TABLE IF EXISTS `book_tag`;
CREATE TABLE `book_tag` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tag_name` varchar(50) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tag` (`tag_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='book tag';

DROP TABLE IF EXISTS `book_tag_relation`;
CREATE TABLE `book_tag_relation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `book_id` bigint NOT NULL,
  `tag_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_book_tag` (`book_id`, `tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='book tag relation';

DROP TABLE IF EXISTS `comment_like_record`;
CREATE TABLE `comment_like_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `comment_id` bigint NOT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_cmt` (`user_id`, `comment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='comment like record';

DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `user_id` bigint NOT NULL AUTO_INCREMENT,
  `password` varchar(255) DEFAULT NULL,
  `nickname` varchar(255) DEFAULT NULL,
  `avatar` varchar(255) DEFAULT NULL,
  `createtime` datetime DEFAULT NULL,
  `account` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='system user';

DROP TABLE IF EXISTS `user_read_progress`;
CREATE TABLE `user_read_progress` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `book_id` bigint NOT NULL,
  `current_chapter_id` bigint NOT NULL,
  `read_percentage` float DEFAULT 0,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_book` (`user_id`, `book_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='user read progress';

SET FOREIGN_KEY_CHECKS = 1;
