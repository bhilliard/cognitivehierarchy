#!/bin/bash
#TYPES=(ABCD ABDC BACD BADC ABCxD BACxD AxBCD AxBDC AxBCxD)
TYPES=(ABDC AxBCxD)
# length-1
LAST=1
NUMATTEMPTS=1
#NUMGAMES=3
#GAMES=('6' '11' '12' '10')
NUMGAMES=1
GAMES=('6' '11')

for k in `seq 0 $NUMGAMES`;
do
	for a in `seq 1 $NUMATTEMPTS`;
	do
		for i in `seq 0 $LAST`;
		do
			for j in `seq $i $LAST`;
			do
			
				#echo ${TYPES[$i]} ${TYPES[$j]} $a${TYPES[$i]}${TYPES[$j]} ${GAMES[$k]}
				qsub -e /data/people/betsy/error/e.txt -o /data/people/betsy/output/o.txt -cwd ./runJavaExperiment.sh ${TYPES[$i]} ${TYPES[$j]} $a${TYPES[$i]}${TYPES[$j]} ${GAMES[$k]}
			done
		done
	done
done
	
