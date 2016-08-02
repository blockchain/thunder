CREATE TABLE IF NOT EXISTS `messages`
  (
     `message_id`        INT(11) DEFAULT NULL,
     `node_key` BINARY(33) NOT NULL,
     `sent`   SMALLINT NOT NULL,
     `processed`   SMALLINT NOT NULL,
     `acked`   SMALLINT NOT NULL,
     `response_message_id`   SMALLINT NOT NULL,
     `timestamp`   INT(11) NOT NULL,
     `message_class`   VARCHAR(100) NOT NULL,
     `message_data`       BLOB NOT NULL,
     PRIMARY KEY (`message_id`, `node_key`, `sent`)
  )
engine=innodb
DEFAULT charset=utf8;