DROP TABLE IF EXISTS `revocation_hashes`;

CREATE TABLE IF NOT EXISTS `revocation_hashes`
  (
     `channel_hash` BINARY(32) NOT NULL,
     `depth`        INT(11) DEFAULT NULL,
     `hash`         BINARY(20) NOT NULL,
     `secret`       BINARY(20),
     PRIMARY KEY (`hash`)
  )
engine=innodb
DEFAULT charset=utf8;