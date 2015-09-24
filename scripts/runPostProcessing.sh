#!/bin/bash

#run all the agg scripts (combineData combinePolicyData, createPolicyClassMatricies, findCoopTimes, graphLearning)

./runCombineData.sh $1 &
./runCombinePolicyData.sh $1 &
wait
echo "DONE 1"
#wait here
./runCreatePolicyClassMatricies.sh $1 &
./runFindCoopTimes.sh $1 &
wait
echo "DONE 2"
./runGraphLearning.sh $1 &

#cat all the non _combined files to game results csvs to print
./combineCSVs.sh $1 &
wait
echo "DONE 3"
#move all the figs to one folder so easily viewed
./moveFilesToResultsFile.sh $1
echo "DONE ALL"

