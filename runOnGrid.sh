#!/bin/bash
TYPES=(ABCD ABDC BACD BADC ABCxD BACxD AxBCD AxBDC AxBCxD)
#TYPES=(ABCD ABDC BACD)
for a in `seq 1 5`;
do
	for i in `seq 0 8`;
	do
		for j in `seq $i 8`;
		do
			
			#echo ${TYPES[$i]} ${TYPES[$j]} $a
			qsub -cwd ./runJavaExperiment.sh ${TYPES[$i]} ${TYPES[$j]} $a		
		done
	done
done
	
