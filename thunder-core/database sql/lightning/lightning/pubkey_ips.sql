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

-- Dumping structure for table lightning.pubkey_ips
CREATE TABLE IF NOT EXISTS `pubkey_ips` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `fragment_index` smallint(6) NOT NULL DEFAULT '0',
  `node_id` int(11) DEFAULT NULL,
  `host` tinytext,
  `port` smallint(6) DEFAULT NULL,
  `timestamp` int(11) DEFAULT NULL,
  `signature` tinyblob,
  PRIMARY KEY (`id`),
  KEY `fragment_index` (`fragment_index`),
  KEY `node_id` (`node_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Data exporting was unselected.
/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;
/*!40014 SET FOREIGN_KEY_CHECKS=IF(@OLD_FOREIGN_KEY_CHECKS IS NULL, 1, @OLD_FOREIGN_KEY_CHECKS) */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
