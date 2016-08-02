-- Dumping structure for table lightning.channels
CREATE TABLE IF NOT EXISTS `meta_data`
  (
     `last_block_height`      INT(11),
     `network`                TEXT
  )
engine=innodb
DEFAULT charset=utf8;

INSERT INTO `meta_data`(last_block_height, network) VALUES(0, NULL);