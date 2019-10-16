CREATE DATABASE IF NOT EXISTS 4WINS;
USE 4WINS;
CREATE TABLE IF NOT EXISTS games(ts TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, player_id varchar(255), value double);
 
insert into games (player_id, value) values
 ("Peter", 25.5),
 ("Sebastian", 30),
 ("Andy", 1),
 ("Alex", 0.5),
 ("Daniel", 100);

insert into games (player_id, value) values
 ("Peter", 26),
 ("Andy", 2),
 ("Alex", 82);
