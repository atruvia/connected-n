#!/bin/bash
while :
do
  influx -host db -database fourWins -execute "INSERT games,player_id=a value=1"
  influx -host db -database fourWins -execute "INSERT games,player_id=a value=0"
  influx -host db -database fourWins -execute "INSERT games,player_id=b value=0"
  influx -host db -database fourWins -execute "INSERT games,player_id=b value=1"
	sleep 1
done
