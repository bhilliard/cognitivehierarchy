#!/bin/bash
# USE THIS FOR SYMMETRIC GAMES

#GAMES=('6')
GAMES=('6')

#TYPES=(ABCD ABDC BACD BADC ABCxD BACxD AxBCD AxBDC AxBCxD)
TYPES=(ABCD ABDC BACD BADC AxBCxD)

#true will run with random start states, false don't
BOOLS=('true')

NUMATTEMPTS=2

#don't touch these parameters, they just control the ends of the loops
# length-1
NUMGAMES=${#GAMES[@]}
LASTGAME=`expr $NUMGAMES - 1`
NUMTYPES=${#TYPES[@]}
LAST=`expr $NUMTYPES - 1`
NUMBOOLS=${#BOOLS[@]}
LASTBOOL=`expr $NUMBOOLS - 1`

#don't use this loop until the filesindicate which condition is being used
for b in `seq 0 $LASTBOOL`;
do
	for k in `seq 0 $LASTGAME`;
	do
		for a in `seq 1 $NUMATTEMPTS`;
		do
			for i in `seq 0 $LAST`;
			do
				for j in `seq $i $LAST`;
				do
					qsub -cwd ./runJavaExperiment.sh ${TYPES[$i]} ${TYPES[$j]} $a'_'${TYPES[$i]}'_'${TYPES[$j]}'_'${BOOLS[$b]}  ${GAMES[$k]} ${BOOLS[$b]}
				
				done
			done
		done
	done
done
	
