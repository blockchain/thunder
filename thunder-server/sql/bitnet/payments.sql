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

-- Exportiere Struktur von Tabelle bitnet.payments
CREATE TABLE IF NOT EXISTS `payments` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `channel_id_sender` int(11) NOT NULL,
  `channel_id_receiver` int(11) NOT NULL,
  `amount` bigint(20) NOT NULL,
  `fee` bigint(20) NOT NULL,
  `phase_sender` tinyint(4) NOT NULL,
  `phase_receiver` tinyint(4) NOT NULL,
  `include_in_sender_channel` tinyint(4) NOT NULL,
  `include_in_receiver_channel` tinyint(4) NOT NULL,
  `include_in_sender_channel_temp` tinyint(4) NOT NULL,
  `include_in_receiver_channel_temp` tinyint(4) NOT NULL,
  `secret_hash` varchar(500) NOT NULL,
  `secret` varchar(500) DEFAULT NULL,
  `settlement_tx_sender` int(11) DEFAULT NULL,
  `settlement_tx_sender_temp` int(11) DEFAULT NULL,
  `refund_tx_sender` int(11) DEFAULT NULL,
  `refund_tx_sender_temp` int(11) DEFAULT NULL,
  `add_tx_sender` int(11) DEFAULT NULL,
  `add_tx_sender_temp` int(11) DEFAULT NULL,
  `settlement_tx_receiver` int(11) DEFAULT NULL,
  `settlement_tx_receiver_temp` int(11) DEFAULT NULL,
  `refund_tx_receiver` int(11) DEFAULT NULL,
  `refund_tx_receiver_temp` int(11) DEFAULT NULL,
  `add_tx_receiver` int(11) DEFAULT NULL,
  `add_tx_receiver_temp` int(11) DEFAULT NULL,
  `timestamp_created` int(11) NOT NULL,
  `timestamp_added_to_receiver` int(11) NOT NULL,
  `timestamp_settled_receiver` int(11) NOT NULL,
  `timestamp_settled_sender` int(11) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Daten Export vom Benutzer nicht ausgewählt
/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;
/*!40014 SET FOREIGN_KEY_CHECKS=IF(@OLD_FOREIGN_KEY_CHECKS IS NULL, 1, @OLD_FOREIGN_KEY_CHECKS) */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
