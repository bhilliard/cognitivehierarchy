#!/bin/bash
#TYPES=(ABCD ABDC BACD BADC ABCxD BACxD AxBCD AxBDC AxBCxD)
TYPES=(ABDC ABCxD AxBCxD)
# length-1
LAST=2
NUMATTEMPTS=3

for a in `seq 1 $NUMATTEMPTS`;
do
	for i in `seq 0 $LAST`;
	do
		for j in `seq 0 $LAST`;
		do
			
			#echo ${TYPES[$i]} ${TYPES[$j]} $a
			qsub -cwd ./runJavaExperiment.sh ${TYPES[$i]} ${TYPES[$j]} $a${TYPES[$i]}${TYPES[$j]}		
		done
	done
done
	
