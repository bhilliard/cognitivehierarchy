#!/bin/bash
#USE THIS SCRIPT FOR NON SYMMETRIC GAMES
#GAMES=('7')
GAMES=('7' '8')

TYPES=(ABCD ABDC BACD BADC ABCxD BACxD AxBCD AxBDC AxBCxD)
#TYPES=(ABCD)

BOOLS=('true')

NUMATTEMPTS=50

# length-1
NUMGAMES=${#GAMES[@]}
LASTGAME=`expr $NUMGAMES - 1`
NUMTYPES=${#TYPES[@]}
LAST=`expr $NUMTYPES - 1`
NUMBOOLS=${#BOOLS[@]}
LASTBOOL=`expr $NUMBOOLS - 1`

for b in `seq 0 $LASTBOOL`;
do

	for k in `seq 0 $LASTGAME`;
	do
		for a in `seq 1 $NUMATTEMPTS`;
		do
			for i in `seq 0 $LAST`;
			do
				for j in `seq 0 $LAST`;
				do
			
					qsub -cwd -l day ./runJavaExperiment.sh ${TYPES[$i]} ${TYPES[$j]} $a'_'${TYPES[$i]}'_'${TYPES[$j]}'_'${BOOLS[$b]}  ${GAMES[$k]} ${BOOLS[$b]}
				
				done
			done
		done
	done
done
