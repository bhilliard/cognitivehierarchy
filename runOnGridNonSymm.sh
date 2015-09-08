#!/bin/bash
TYPES=(ABCD ABDC BACD BADC ABCxD BACxD AxBCD AxBDC AxBCxD)
#TYPES=(ABDC ABCxD)
# length-1
LAST=8
NUMATTEMPTS=25
NUMGAMES=1
GAMES=('7' '8')

for k in `seq 0 $NUMGAMES`;
do
	for a in `seq 1 $NUMATTEMPTS`;
	do
		for i in `seq 0 $LAST`;
		do
			for j in `seq 0 $LAST`;
			do
			
				#echo ${TYPES[$i]} ${TYPES[$j]} $a${TYPES[$i]}${TYPES[$j]} ${GAMES[$k]}
				qsub -e /data/people/betsy/error/ -o /data/people/betsy/output/ -cwd ./runJavaExperiment.sh ${TYPES[$i]} ${TYPES[$j]} $a${TYPES[$i]}${TYPES[$j]} ${GAMES[$k]}	
			done
		done
	done
done
	
