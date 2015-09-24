#!/bin/bash
./combinePolicyClass.py $1'TwoAgentsNoCompromise_2by5'/ NoCompromise
./combinePolicyClass.py $1'TwoAgentsHall_3by5_noWalls'/ Hall
./combinePolicyClass.py $1'TwoAgentsIntersection_3by5'/ Intersection
./combinePolicyClass.py $1'TwoAgentsLongHall_1by7'/ LongHall
./combinePolicyClass.py $1'TwoAgentsDoor_3by5'/ Door
