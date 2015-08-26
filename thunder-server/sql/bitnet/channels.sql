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

-- Exportiere Struktur von Tabelle bitnet.channels
CREATE TABLE IF NOT EXISTS `channels` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `pub_key_client` varchar(500) DEFAULT NULL,
  `pub_key_server` varchar(500) DEFAULT NULL,
  `change_address_server` varchar(500) DEFAULT NULL,
  `change_address_client` varchar(500) DEFAULT NULL,
  `master_priv_key_client` varchar(500) DEFAULT NULL,
  `master_priv_key_server` varchar(500) DEFAULT NULL,
  `initial_amount_server` bigint(20) DEFAULT NULL,
  `initial_amount_client` bigint(20) DEFAULT NULL,
  `amount_server` bigint(20) DEFAULT NULL,
  `amount_client` bigint(20) DEFAULT NULL,
  `timestamp_open` int(11) DEFAULT NULL,
  `timestamp_close` int(11) DEFAULT NULL,
  `timestamp_force_close` int(11) DEFAULT NULL,
  `key_chain_depth` int(11) DEFAULT NULL,
  `key_chain_child` int(11) DEFAULT NULL,
  `master_chain_depth` int(11) DEFAULT NULL,
  `opening_tx_hash` varchar(500) DEFAULT NULL,
  `opening_tx` int(11) DEFAULT NULL,
  `refund_tx_server` int(11) DEFAULT NULL,
  `refund_tx_client` int(11) DEFAULT NULL,
  `channel_tx_server` int(11) DEFAULT NULL,
  `channel_tx_revoke_server` int(11) DEFAULT NULL,
  `channel_tx_client` int(11) DEFAULT NULL,
  `channel_tx_revoke_client` int(11) DEFAULT NULL,
  `channel_tx_server_temp` int(11) DEFAULT NULL,
  `channel_tx_client_temp` int(11) DEFAULT NULL,
  `channel_tx_revoke_client_temp` int(11) DEFAULT NULL,
  `channel_tx_revoke_server_temp` int(11) DEFAULT NULL,
  `has_open_payments` tinyint(4) DEFAULT NULL,
  `establish_phase` tinyint(4) DEFAULT NULL,
  `payment_phase` tinyint(4) DEFAULT NULL,
  `is_ready` tinyint(4) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Daten Export vom Benutzer nicht ausgewählt
/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;
/*!40014 SET FOREIGN_KEY_CHECKS=IF(@OLD_FOREIGN_KEY_CHECKS IS NULL, 1, @OLD_FOREIGN_KEY_CHECKS) */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
