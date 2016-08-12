-- Dumping structure for table lightning.channels
CREATE TABLE IF NOT EXISTS `meta_data`
  (
     `last_block_height`      INT(11),
     `network`                TEXT,
     `server_node_key`        BLOB
  )
engine=innodb
DEFAULT charset=utf8;

INSERT INTO `meta_data`(last_block_height, network, server_node_key) VALUES(0, NULL, NULL);