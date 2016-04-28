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

-- Dumping database structure for lightning
DROP DATABASE IF EXISTS `lightning`;
CREATE DATABASE IF NOT EXISTS `lightning` /*!40100 DEFAULT CHARACTER SET latin1 */;
USE `lightning`;


-- Dumping structure for table lightning.channels
DROP TABLE IF EXISTS `channels`;
CREATE TABLE IF NOT EXISTS `channels` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `fragment_index` smallint(6) DEFAULT NULL,
  `hash` binary(20) DEFAULT NULL,
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
  KEY `fragment_index` (`fragment_index`),
  KEY `hash` (`hash`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Data exporting was unselected.


-- Dumping structure for table lightning.channel_status
DROP TABLE IF EXISTS `channel_status`;
CREATE TABLE IF NOT EXISTS `channel_status` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `fragment_index` smallint(6) DEFAULT NULL,
  `hash` binary(20) DEFAULT NULL,
  `channel_id` int(11) DEFAULT NULL,
  `info_a` tinyblob,
  `info_b` tinyblob,
  `timestamp` int(11) DEFAULT NULL,
  `signature_a` tinyblob,
  `signature_b` tinyblob,
  PRIMARY KEY (`id`),
  KEY `fragment_index` (`fragment_index`),
  KEY `channel_id` (`channel_id`),
  KEY `hash` (`hash`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Data exporting was unselected.


-- Dumping structure for table lightning.nodes
DROP TABLE IF EXISTS `nodes`;
CREATE TABLE IF NOT EXISTS `nodes` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `pubkey` binary(33) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `pubkey` (`pubkey`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Data exporting was unselected.


-- Dumping structure for table lightning.pubkey_ips
DROP TABLE IF EXISTS `pubkey_ips`;
CREATE TABLE IF NOT EXISTS `pubkey_ips` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `fragment_index` smallint(6) NOT NULL DEFAULT '0',
  `hash` binary(20) NOT NULL DEFAULT '0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0',
  `node_id` int(11) DEFAULT NULL,
  `host` tinytext,
  `port` smallint(6) DEFAULT NULL,
  `timestamp` int(11) DEFAULT NULL,
  `signature` tinyblob,
  PRIMARY KEY (`id`),
  KEY `fragment_index` (`fragment_index`),
  KEY `node_id` (`node_id`),
  KEY `hash` (`hash`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Data exporting was unselected.
/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;
/*!40014 SET FOREIGN_KEY_CHECKS=IF(@OLD_FOREIGN_KEY_CHECKS IS NULL, 1, @OLD_FOREIGN_KEY_CHECKS) */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
