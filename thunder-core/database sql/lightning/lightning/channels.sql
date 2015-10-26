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

-- Dumping structure for table lightning.channels
CREATE TABLE IF NOT EXISTS `channels` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `fragment_index` smallint(6) DEFAULT NULL,
  `node_id_a` int(11) NOT NULL DEFAULT '0',
  `node_id_b` int(11) NOT NULL DEFAULT '0',
  `secret_a_hash` tinyblob,
  `secret_b_hash` tinyblob,
  `pubkey_a1` tinyblob,
  `pubkey_a2` tinyblob,
  `pubkey_b1` tinyblob,
  `pubkey_b2` tinyblob,
  `txid_anchor` tinyblob,
  `signature_a` tinyblob,
  `signature_b` tinyblob,
  PRIMARY KEY (`id`),
  KEY `node_id_a_node_id_b` (`node_id_a`,`node_id_b`),
  KEY `fragment_index` (`fragment_index`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Data exporting was unselected.
/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;
/*!40014 SET FOREIGN_KEY_CHECKS=IF(@OLD_FOREIGN_KEY_CHECKS IS NULL, 1, @OLD_FOREIGN_KEY_CHECKS) */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
