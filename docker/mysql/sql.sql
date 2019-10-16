CREATE DATABASE IF NOT EXISTS 4WINS;
USE 4WINS;
CREATE TABLE IF NOT EXISTS games(ts TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, player_id varchar(255), value double);
 
insert into games (player_id, value) values
 ("A", 25.5),
 ("B", 30),
 ("C", 1),
 ("D", 0.5),
 ("E", 100);
