-- --------------------------------------------------------
-- Host:                         127.0.0.1
-- Server Version:               5.6.21-log - MySQL Community Server (GPL)
-- Server Betriebssystem:        Win64
-- HeidiSQL Version:             9.2.0.4947
-- --------------------------------------------------------

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET NAMES utf8mb4 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;

-- Exportiere Struktur von Tabelle bitnet.storedkeys
CREATE TABLE IF NOT EXISTS `storedkeys` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `channel_id` int(11) DEFAULT NULL,
  `owner` tinyint(4) DEFAULT NULL,
  `pub_key` tinyblob NOT NULL,
  `priv_key` tinyblob,
  `current_channel` tinyint(4) DEFAULT NULL,
  `current_channel_temp` tinyint(4) DEFAULT NULL,
  `channel_version` int(11) DEFAULT NULL,
  `used` tinyint(4) DEFAULT NULL,
  `exposed` tinyint(4) DEFAULT NULL,
  `key_chain_depth` int(11) DEFAULT NULL,
  `key_chain_child` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `channel_pub_key` (`channel_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Daten Export vom Benutzer nicht ausgewählt
/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;
/*!40014 SET FOREIGN_KEY_CHECKS=IF(@OLD_FOREIGN_KEY_CHECKS IS NULL, 1, @OLD_FOREIGN_KEY_CHECKS) */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
