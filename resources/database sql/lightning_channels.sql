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

-- Dumping structure for table lightning.lightning_channels
DROP TABLE IF EXISTS `lightning_channels`;
CREATE TABLE IF NOT EXISTS `lightning_channels` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `node_id` int(11) NOT NULL,
  `key_client` binary(33) DEFAULT NULL,
  `key_client_a` binary(33) DEFAULT NULL,
  `key_server` binary(33) DEFAULT NULL,
  `key_server_a` binary(33) DEFAULT NULL,
  `master_priv_key_client` binary(20) DEFAULT NULL,
  `client_chain_depth` int(11) DEFAULT NULL,
  `master_priv_key_server` binary(20) DEFAULT NULL,
  `server_chain_depth` int(11) DEFAULT NULL,
  `server_chain_child` int(11) DEFAULT NULL,
  `channel_tx_version` int(11) DEFAULT NULL,
  `initial_amount_server` bigint(20) DEFAULT NULL,
  `initial_amount_client` bigint(20) DEFAULT NULL,
  `amount_server` bigint(20) DEFAULT NULL,
  `amount_client` bigint(20) DEFAULT NULL,
  `timestamp_open` int(11) DEFAULT NULL,
  `timestamp_force_close` int(11) DEFAULT NULL,
  `anchor_tx_hash_server` binary(32) DEFAULT NULL,
  `anchor_tx_hash_client` binary(32) DEFAULT NULL,
  `anchor_secret_server` binary(20) DEFAULT NULL,
  `anchor_secret_hash_server` binary(20) DEFAULT NULL,
  `anchor_secret_client` binary(20) DEFAULT NULL,
  `anchor_secret_hash_client` binary(20) DEFAULT NULL,
  `anchor_revocation_server` binary(20) DEFAULT NULL,
  `anchor_revocation_hash_server` binary(20) DEFAULT NULL,
  `anchor_revocation_client` binary(20) DEFAULT NULL,
  `anchor_revocation_hash_client` binary(20) DEFAULT NULL,
  `escape_tx_sig` blob,
  `escape_fast_tx_sig` blob,
  `channel_tx_sig` blob,
  `channel_tx_temp_sig` blob,
  `escape_tx_server` blob,
  `escape_tx_client` blob,
  `escape_fast_tx_server` blob,
  `escape_fast_tx_client` blob,
  `phase` varchar(50) DEFAULT NULL,
  `is_ready` tinyint(4) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Data exporting was unselected.
/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;
/*!40014 SET FOREIGN_KEY_CHECKS=IF(@OLD_FOREIGN_KEY_CHECKS IS NULL, 1, @OLD_FOREIGN_KEY_CHECKS) */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
