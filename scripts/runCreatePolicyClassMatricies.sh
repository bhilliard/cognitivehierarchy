#!/bin/bash
./createPolicyClassMatricies.py $1'TwoAgentsNoCompromise_2by5'/ NoCompromise
./createPolicyClassMatricies.py $1'TwoAgentsHall_3by5_noWalls'/ Hall
./createPolicyClassMatricies.py $1'TwoAgentsIntersection_3by5'/ Intersection
./createPolicyClassMatricies.py $1'TwoAgentsLongHall_1by7'/ LongHall
./createPolicyClassMatricies.py $1'TwoAgentsDoor_3by5'/ Door
