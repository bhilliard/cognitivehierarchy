#!/bin/bash
# USE THIS FOR ANY GAMES, JUST MAKE SURE YOU GET SYMMETRIC AND NON SYMMETRIC CORRECT
# first arg is day or hour

SYMGAMES=('6' '10' '14') #'5' '6' '10' '11' '12' '13' '14'
NONSYMGAMES=('7' '8') #'7' '8'

TYPES=(ABCD ABDC BACD BADC ABCxD BACxD AxBCD AxBDC AxBCxD)
#TYPES=(ABCD ABDC BACD BADC AxBCxD)

#true will run with random start states, false don't
BOOLS=('true')

NUMATTEMPTS=3

#don't touch the remaining parameters, they just control the ends of the loops
# length-1
NUMSYMGAMES=${#GAMES[@]}
LASTSYM=`expr $NUMSYMGAMES - 1`
NUMNONSYMGAMES=${#GAMES[@]}
LASTNONSYM=`expr $NUMNONSYMGAMES - 1`
NUMTYPES=${#TYPES[@]}
LAST=`expr $NUMTYPES - 1`
NUMBOOLS=${#BOOLS[@]}
LASTBOOL=`expr $NUMBOOLS - 1`

#don't use this loop until the files indicate which condition is being used
for b in `seq 0 $LASTBOOL`;
do
	for a in `seq 1 $NUMATTEMPTS`;
	do
			
		for i in `seq 0 $LAST`;
		do
				
			for j in `seq $i $LAST`;
				do

				for k in `seq 0 $LASTSYM`;
				do
					#echo $j $1
					qsub -cwd -l $1 ./runJavaExperiment.sh ${TYPES[$i]} ${TYPES[$j]} $a'_'${TYPES[$i]}'_'${TYPES[$j]}'_'${BOOLS[$b]}  ${SYMGAMES[$k]} ${BOOLS[$b]}
				
				done
			done
			
			
			for j in `seq 0 $LAST`;
			do	
				for k in `seq 0 $LASTNONSYM`;
				do
					#echo $j $1
					qsub -cwd -l $1 ./runJavaExperiment.sh ${TYPES[$i]} ${TYPES[$j]} $a'_'${TYPES[$i]}'_'${TYPES[$j]}'_'${BOOLS[$b]}  ${NONSYMGAMES[$k]} ${BOOLS[$b]}
				
				done
			done
			

		done
	done
done
	
