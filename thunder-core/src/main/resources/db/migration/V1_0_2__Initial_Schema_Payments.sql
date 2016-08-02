CREATE TABLE IF NOT EXISTS `payments`
  (
     `id`                INT(11) NOT NULL auto_increment,
     `channel_hash`      BINARY(32) NOT NULL,
     `sending`           SMALLINT(5) NOT NULL,
     `amount`            BIGINT(20) NOT NULL,
     `phase`             TINYINT(4) NOT NULL,
     `secret_id`         INT(11) NOT NULL,
     `onion_id`          INT(11) NOT NULL,
     `timestamp_added`   INT(11) NOT NULL,
     `timestamp_refund`  INT(11) NOT NULL,
     `timestamp_settled` INT(11) NOT NULL,
     `version_added`     INT(11) NOT NULL,
     `version_settled`   INT(11) NOT NULL,
     PRIMARY KEY (`id`)
  )
engine=innodb
DEFAULT charset=utf8;


CREATE TABLE IF NOT EXISTS `payment_secrets`
  (
     `hash`       BINARY(20) NOT NULL,
     `secret`     BINARY(20),
     PRIMARY KEY (`hash`)
  )
engine=innodb
DEFAULT charset=utf8;

CREATE TABLE IF NOT EXISTS `payment_onion_objects`
  (
     `id`         INT(11) NOT NULL auto_increment,
     `data`       BLOB NOT NULL,
     PRIMARY KEY (`id`)
  )
engine=innodb
DEFAULT charset=utf8;


CREATE TABLE IF NOT EXISTS `payment_settlements`
  (
     `id`                  INT(11) NOT NULL auto_increment,
     `channel_hash`        BINARY(32) NOT NULL,
     `phase`               TINYINT(4) NOT NULL,
     `timestamp_to_settle` INT(11) NOT NULL,
     `our_channel_tx`      SMALLINT NOT NULL,
     `cheated`             SMALLINT NOT NULL,
     `is_payment`             SMALLINT NOT NULL,
     `revocation_hash`     BINARY(20) NOT NULL,
     `payment_id`          INT,
     `channel_tx`          BLOB,
     `second_tx`           BLOB,
     `third_tx`            BLOB,
     `channel_tx_height`   INT,
     `second_tx_height`    INT,
     `third_tx_height`     INT,
     `channel_tx_output`   BIGINT,
     `second_tx_output`    BIGINT,
     `third_tx_output`     BIGINT,
     PRIMARY KEY (`id`)
  )
engine=innodb
DEFAULT charset=utf8;