CREATE TABLE IF NOT EXISTS `channel`
  (
     `id`                     INT(11) NOT NULL auto_increment,
     `hash`                   BINARY(32) NOT NULL,
     `node_id`                INT(11) NOT NULL,
     `key_client`             BINARY(33) NOT NULL,
     `key_server`             BINARY(33) NOT NULL,
     `address_client`         TEXT NOT NULL,
     `address_server`         TEXT NOT NULL,
     `master_priv_key_client` BINARY(20) NOT NULL,
     `master_priv_key_server` BINARY(20) NOT NULL,
     `sha_chain_depth`        INT(11) NOT NULL,
     `amount_server`          BIGINT(20) NOT NULL,
     `amount_client`          BIGINT(20) NOT NULL,
     `timestamp_open`         INT(11) NOT NULL,
     `timestamp_force_close`  INT(11) NOT NULL,
     `anchor_tx_hash`         BINARY(32) NOT NULL,
     `anchor_tx_blockheight`  INT(11) NOT NULL,
     `anchor_tx_min_conf`     INT(11) NOT NULL,
     `channel_tx_on_chain`    BLOB,
     `channel_tx_signatures`  TEXT NOT NULL,
     `csv_delay`              INT(11) NOT NULL,
     `fee_per_byte`           INT(11) NOT NULL,
     `phase`                  TEXT NOT NULL,
     PRIMARY KEY (`id`),
     KEY `hash` (`hash`)
  )
engine=innodb
DEFAULT charset=utf8;