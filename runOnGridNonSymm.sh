#!/bin/bash
TYPES=(ABCD ABDC BACD BADC ABCxD BACxD AxBCD AxBDC AxBCxD)
#TYPES=(AxBCD)
# length-1
LAST=8
NUMATTEMPTS=50

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
	
