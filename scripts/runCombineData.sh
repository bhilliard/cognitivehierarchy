#!/bin/bash

./combineData.py $1'TwoAgentsNoCompromise_2by5'/ NoCompromise
./combineData.py $1'TwoAgentsCompromise_2by5'/ Compromise
./combineData.py $1'TwoAgentsHall_3by5_noWalls'/ Hall
./combineData.py $1'TwoAgentsIntersection_3by5'/ Intersection false
./combineData.py $1'TwoAgentsLongHall_1by7'/ LongHall false
./combineData.py $1'TwoAgentsDoor_3by5'/ Door
