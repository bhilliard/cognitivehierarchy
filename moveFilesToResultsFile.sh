#!/bin/bash

if [ ! -d $1 ]; then
	mkdir $1
fi
cp $1'TwoAgentsNoCompromise_2by5'/NoCompromise_results.csv ./$1
cp $1'TwoAgentsHall_3by5_noWalls'/Hall_results.csv ./$1
cp $1'TwoAgentsIntersection_3by5'/Intersection_results.csv ./$1
cp $1'TwoAgentsLongHall_1by7'/LongHall_results.csv ./$1
cp $1'TwoAgentsDoor_3by5'/Door_results.csv ./$1

cp $1'TwoAgentsNoCompromise_2by5'/*.png ./$1
cp $1'TwoAgentsHall_3by5_noWalls'/*.png ./$1
cp $1'TwoAgentsIntersection_3by5'/*.png ./$1
cp $1'TwoAgentsLongHall_1by7'/*.png ./$1
cp $1'TwoAgentsDoor_3by5'/*.png ./$1
