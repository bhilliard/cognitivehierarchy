#!/bin/bash

cat $1'TwoAgentsNoCompromise_2by5'/*.csv >> $1'TwoAgentsNoCompromise_2by5'/NoCompromise_results.csv
cat $1'TwoAgentsHall_3by5_noWalls'/*.csv >>  $1'TwoAgentsHall_3by5_noWalls'/Hall_results.csv
cat $1'TwoAgentsIntersection_3by5'/*.csv >>  $1'TwoAgentsIntersection_3by5'/Intersection_results.csv
cat $1'TwoAgentsLongHall_1by7'/*.csv >>  $1'TwoAgentsLongHall_1by7'/LongHall_results.csv
cat $1'TwoAgentsDoor_3by5'/*.csv >>  $1'TwoAgentsDoor_3by5'/Door_results.csv
