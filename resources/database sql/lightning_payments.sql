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

-- Dumping structure for table lightning.lightning_payments
DROP TABLE IF EXISTS `lightning_payments`;
CREATE TABLE IF NOT EXISTS `lightning_payments` (
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
  `secret_hash` binary(20) NOT NULL,
  `secret` binary(20) DEFAULT NULL,
  `timestamp_added_sender` int(11) NOT NULL,
  `timestamp_added_receiver` int(11) NOT NULL,
  `timestamp_settled_receiver` int(11) NOT NULL,
  `timestamp_settled_sender` int(11) NOT NULL,
  `version_added_sender` int(11) NOT NULL,
  `version_added_receiver` int(11) NOT NULL,
  `version_settled_sender` int(11) NOT NULL,
  `version_settled_receiver` int(11) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Data exporting was unselected.
/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;
/*!40014 SET FOREIGN_KEY_CHECKS=IF(@OLD_FOREIGN_KEY_CHECKS IS NULL, 1, @OLD_FOREIGN_KEY_CHECKS) */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
