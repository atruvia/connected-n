#!/bin/sh
docker run -it --network docker_default --rm mysql mysql -hmysql -uroot -p sql.sql

