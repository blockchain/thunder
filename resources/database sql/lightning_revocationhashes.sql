-- --------------------------------------------------------
-- Host:                         127.0.0.1
-- Server version:               5.6.27 - MySQL Community Server (GPL)
-- Server OS:                    osx10.8
-- HeidiSQL Version:             9.3.0.4984
-- --------------------------------------------------------

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET NAMES utf8mb4 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;

-- Dumping structure for table lightning.lightning_revocationhashes
DROP TABLE IF EXISTS `lightning_revocationhashes`;
CREATE TABLE IF NOT EXISTS `lightning_revocationhashes` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `channel_id` int(11) DEFAULT NULL,
  `channel_version` int(11) DEFAULT NULL,
  `owner` tinyint(4) DEFAULT NULL,
  `secret` binary(20) NOT NULL,
  `secretHash` binary(20) DEFAULT NULL,
  `current_channel` tinyint(4) DEFAULT NULL,
  `current_channel_temp` tinyint(4) DEFAULT NULL,
  `used` tinyint(4) DEFAULT NULL,
  `exposed` tinyint(4) DEFAULT NULL,
  `key_chain_depth` int(11) DEFAULT NULL,
  `key_chain_child` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `channel_pub_key` (`channel_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Data exporting was unselected.
/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;
/*!40014 SET FOREIGN_KEY_CHECKS=IF(@OLD_FOREIGN_KEY_CHECKS IS NULL, 1, @OLD_FOREIGN_KEY_CHECKS) */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
