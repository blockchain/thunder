DROP TABLE IF EXISTS `p2p_channel_static`;

CREATE TABLE IF NOT EXISTS `p2p_channel_static`
  (
     `id`             INT(11) NOT NULL auto_increment,
     `fragment_index` SMALLINT(6) NOT NULL,
     `hash`           BINARY(20) NOT NULL UNIQUE,
     `node_id_a`      INT(11) NOT NULL,
     `node_id_b`      INT(11) NOT NULL,
     `pubkey_a`       BINARY(33) NOT NULL,
     `pubkey_b`       BINARY(33) NOT NULL,
     `txid_anchor`    TINYBLOB NOT NULL,
     `signature_a`    TINYBLOB NOT NULL,
     `signature_b`    TINYBLOB NOT NULL,
     `timestamp`      INT(11) NOT NULL,
     PRIMARY KEY (`id`),
     KEY `node_id_a_node_id_b` (`node_id_a`, `node_id_b`),
     KEY `fragment_index_static` (`fragment_index`),
     KEY `hash_index` (`hash`)
  )
engine=innodb
DEFAULT charset=utf8;

DROP TABLE IF EXISTS `p2p_channel_dynamic`;

CREATE TABLE IF NOT EXISTS `p2p_channel_dynamic`
  (
     `id`             INT(11) NOT NULL auto_increment,
     `fragment_index` SMALLINT(6) NOT NULL,
     `hash`           BINARY(20) NOT NULL UNIQUE,
     `channel_id`     INT(11) NOT NULL,
     `info_a`         TINYBLOB NOT NULL,
     `info_b`         TINYBLOB NOT NULL,
     `timestamp`      INT(11) NOT NULL,
     `signature_a`    TINYBLOB NOT NULL,
     `signature_b`    TINYBLOB NOT NULL,
     PRIMARY KEY (`id`),
     KEY `fragment_index_dynamic` (`fragment_index`),
     KEY `channel_id_dynamic` (`channel_id`),
     KEY `hash_index_dynamic` (`hash`)
  )
engine=innodb
DEFAULT charset=utf8;

DROP TABLE IF EXISTS `pubkey_ips`;

CREATE TABLE IF NOT EXISTS `pubkey_ips`
  (
     `id`             INT(11) NOT NULL auto_increment,
     `fragment_index` SMALLINT(6) NOT NULL,
     `hash`           BINARY(20) NOT NULL UNIQUE,
     `node_id`        INT(11) NOT NULL,
     `host`           TINYTEXT NOT NULL,
     `port`           INT(11) NOT NULL,
     `timestamp`      INT(11) NOT NULL,
     `signature`      TINYBLOB NOT NULL,
     PRIMARY KEY (`id`),
     KEY `fragment_index_ips` (`fragment_index`),
     KEY `node_id_static` (`node_id`),
     KEY `hash_index_static` (`hash`)
  )
engine=innodb
DEFAULT charset=utf8;