
CREATE TABLE IF NOT EXISTS `channels` (
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
  `is_ready` tinyint(4) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



CREATE TABLE IF NOT EXISTS `constants` (
  `key_count` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



CREATE TABLE IF NOT EXISTS `messages` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `type` text NOT NULL,
  `message` longtext NOT NULL,
  `timestamp` int(11) NOT NULL,
  `pubkey` varchar(100) NOT NULL,
  `signature` varchar(100) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE IF NOT EXISTS `outputs` (
  `transaction_hash` varchar(100) DEFAULT NULL,
  `vout` int(11) DEFAULT NULL,
  `value` bigint(20) DEFAULT NULL,
  `private_key` varchar(100) DEFAULT NULL,
  `transaction_output` varchar(100) DEFAULT NULL,
  `timestamp_locked` int(11) DEFAULT NULL,
  `channel_pubkey` varchar(100) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE IF NOT EXISTS `payments` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `pub_key_sender` varchar(500) DEFAULT NULL,
  `pub_key_receiver` varchar(500) NOT NULL,
  `amount` bigint(20) NOT NULL,
  `phase` tinyint(4) NOT NULL,
  `include_in_sender_channel` tinyint(4) NOT NULL,
  `include_in_receiver_channel` tinyint(4) NOT NULL,
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
  `timestamp_settled` int(11) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `storedkeys` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `channel_pub_key` varchar(60) DEFAULT NULL,
  `owner` tinyint(4) DEFAULT NULL,
  `pub_key` varchar(60) NOT NULL,
  `priv_key` varchar(60) DEFAULT NULL,
  `current_channel` tinyint(4) DEFAULT NULL,
  `used` tinyint(4) DEFAULT NULL,
  `exposed` tinyint(4) DEFAULT NULL,
  `key_chain_depth` int(11) DEFAULT NULL,
  `key_chain_child` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `pub_key` (`pub_key`),
  KEY `channel_pub_key` (`channel_pub_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE IF NOT EXISTS `transactions` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `hash` varchar(500) NOT NULL,
  `channel_pub_key` varchar(500) NOT NULL,
  `payment_id` int(11) DEFAULT NULL,
  `data` mediumblob,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



TRUNCATE TABLE channels;
TRUNCATE TABLE messages;
TRUNCATE TABLE payments;
TRUNCATE TABLE storedkeys;
TRUNCATE TABLE transactions;

INSERT INTO constants VALUES (1);

COMMIT;