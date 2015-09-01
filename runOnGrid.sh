#!/bin/bash
TYPES=(ABCD ABDC BACD BADC ABCxD BACxD AxBCD AxBDC AxBCxD)

for a in `seq 1 100`;
do
	for i in `seq 1 9`;
	do
		for j in `seq i 9`
		do
			qsub -cwd ./runJavaExperiment.sh ${TYPES[$i]} ${TYPES[$j]) $a
		done
	done
done
	
