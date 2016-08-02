CREATE TABLE `NODES`
  (
     `id`     INT(11) NOT NULL auto_increment,
     `pubkey` BINARY(33) DEFAULT NULL,
     PRIMARY KEY (`id`),
     UNIQUE KEY `pubkey` (`pubkey`)
  )
engine=innodb
DEFAULT charset=utf8;