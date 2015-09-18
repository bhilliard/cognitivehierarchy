#!/usr/bin/python

import sys 
import csv
import string
import os
import numpy as np

def combineData(fname,label):
	maxAttempt = 0
	types = ["ABCD", "ABDC", "BACD", "BADC", "ABCxD","BACxD", "AxBCD", "AxBDC", "AxBCxD"] 
	ftypes = ["  ABCD", "  ABDC", "  BACD", "  BADC", " ABCxD"," BACxD", " AxBCD", " AxBDC", "AxBCxD"] 
	#types = ["ABDC", "BADC", "AxBCxD"]
	numAgents = len(types)
	matrix =[["" for x in range(numAgents)] for x in range(numAgents)]
	gmatrix = [[0.0 for x in range(numAgents)] for x in range(numAgents)]
	bmatrix = [[0.0 for x in range(numAgents)] for x in range(numAgents)]
	gValues = [[[] for x in range(numAgents)] for x in range(numAgents)]
	bValues = [[[] for x in range(numAgents)] for x in range(numAgents)]
	ghappyVals = [[[] for x in range(numAgents)] for x in range(numAgents)]
	bhappyVals = [[[] for x in range(numAgents)] for x in range(numAgents)]
	gSDmatrix = [[0.0 for x in range(numAgents)] for x in range(numAgents)]
	bSDmatrix = [[0.0 for x in range(numAgents)] for x in range(numAgents)]
	ghappy = [[0.0 for x in range(numAgents)] for x in range(numAgents)]
	bhappy = [[0.0 for x in range(numAgents)] for x in range(numAgents)]
	mMinusSDMatrix= [[0.0 for x in range(numAgents)] for x in range(numAgents)]
	foundMatrix = [[0.0 for x in range(numAgents)] for x in range(numAgents)]
	#for folder
	#get names from folder
	outLoc = ''+fname+'/../'+fname+label+'_combinedData.csv'
	outFile = open(outLoc, 'w')
	mOutFileName = ''+fname+'/../'+fname+label+'_matrix.csv'
	sdOutFileName = ''+fname+'/../'+fname+label+'_SDmatrix.csv'
	hOutFileName = ''+fname+'/../'+fname+label+'_happymatrix.csv'	
	writer = csv.writer(outFile)
	#print fname
	gfound = 0
	bfound = 0
	toFind = numAgents*numAgents
	for subdir in os.listdir(fname):
		#read in meta.txt
		#print "subdir: "+subdir
		if os.path.isdir(fname+subdir):
			for nameFile in os.listdir(fname+subdir):
				if os.path.isdir(fname+subdir+"/"+nameFile):

					outLine = ['GNAME','BNAME','GSCORE','BSCORE','ATTNUM','GHAPPY','BHAPPY']
					attnum = subdir.split('_')[0]
					if int(attnum) > maxAttempt:
						maxAttempt = int(attnum)
					#print "file: "+nameFile
					gname = nameFile.split('_')[2]
					bname = nameFile.split('_')[5]
					if gname in types and bname in types:
						gnameLoc = types.index(gname)
						bnameLoc = types.index(bname)
						foundMatrix[gnameLoc][bnameLoc]+=1				
						outLine[0] = gname
						outLine[1] = bname
						outLine[4] = attnum
						metaFile = fname+subdir+"/meta.txt"
						#print metaFile
						if os.path.isfile(metaFile):
							f = open(metaFile, 'r')
							line = f.readline()
							gscore=0
							bscore=0
							while line != '':
								
								if 'Green Agent Scores:' in line:
									gscore = float(f.readline().split(' ')[2])
									
									outLine[2]=str(gscore)
									if float(gscore)>=50.0:
										print metaFile+" "+str(gscore)
									gfound+=1
								if 'Blue Agent Scores:' in line:
									f.readline()
									bscore = float(f.readline().split(' ')[1])
									#bmatrix[gnameLoc][bnameLoc]+=float(bscore)
									
									outLine[3]=str(bscore)
									if float(bscore)>=50.0:
										print metaFile+" "+str(bscore)
									bfound+=1
								
								line = f.readline()
							# fix this!!
							bValues[gnameLoc][bnameLoc].append(bscore)
							#gmatrix[gnameLoc][bnameLoc]+=float(gscore)
							gValues[gnameLoc][bnameLoc].append(gscore)
							ghappyVal = happiness(gscore, bscore, gname)
							bhappyVal = happiness(bscore, gscore, bname)
							outLine[5]=str(ghappyVal)
							outLine[6]=str(bhappyVal)
							ghappyVals[gnameLoc][bnameLoc].append(ghappyVal)
							#print 'happy '+gname+' '+str(happiness(gscore, bscore, gname))
							bhappyVals[gnameLoc][bnameLoc].append(bhappyVal)
							#print 'happy '+bname+' '+str(happiness(bscore, gscore, bname))
							#print outLine
							writer.writerow(outLine)
					else:
						print "________META FILE NOT FOUND_______"
						print metaFile
	
	for i in range(0,len(bmatrix)):
		for j in range(0, len(bmatrix[0])):
			bmatrix[i][j] = np.mean(bValues[i][j])
			gmatrix[i][j] = np.mean(gValues[i][j])
			ghappy[i][j]=np.mean(ghappyVals[i][j])
			bhappy[i][j]=np.mean(bhappyVals[i][j])
			bSDmatrix[i][j] = np.std(bValues[i][j])
			gSDmatrix[i][j] = np.std(gValues[i][j])
			mMinusSDMatrix[i][j] = bmatrix[i][j]-bSDmatrix[i][j]

	meanRow = ["{0:15s}".format('min m')]
	sdRow = ["{0:15s}".format('min(m-SD)')]
	label+=" "+str(maxAttempt)
	for c in range(0,len(bmatrix[0])):
		meanRow.append("{0:15.2f}".format(findMinInCol(types[c], bmatrix, types)))
		sdRow.append("{0:15.2f}".format(findMinInCol(types[c], mMinusSDMatrix, types)))
	
	for t in range(0,len(types)):
		ftypes[t] = "{0:6s}".format(types[t])
	ftypes.insert(0,"{0:6s}".format(' '))
	outputMatrix(ghappy,bhappy,hOutFileName,ftypes,[],[],label+" happiness ")
	del ftypes[0]
	for t in range(0,len(types)):
		ftypes[t] = "{0:15s}".format(types[t])
	ftypes.insert(0,"               ")
	outputMatrix(gmatrix, bmatrix, mOutFileName, ftypes,meanRow,sdRow,label+" Scores ")
	del ftypes[0]
	for t in range(0,len(types)):
		ftypes[t] = "{0:6s}".format(types[t])
	ftypes.insert(0,"      ")
	outputMatrix(gSDmatrix, bSDmatrix, sdOutFileName, ftypes,[],[],label+" SD data ")
	prettyPrint(bmatrix, ftypes)
	prettyPrint(gmatrix, ftypes)
	prettyPrint(foundMatrix, ftypes)
	toFind = maxAttempt * toFind
	print meanRow
	print sdRow
	print maxAttempt, toFind, gfound, bfound
	#spit out line


def happiness(aScore, oScore, agentType):
	if agentType=='ABCD':
		p=[1.0,.25,0.0,0.0, 0.0]
	elif agentType=='ABDC':
		p=[1.0,0.0,-.25,0.0, 0.0]
	elif agentType=='BACD':
		p=[1.0,0.0,0.25,0.0, 0.0]
	elif agentType=='BADC':
		p=[1.0,-.25,0.0,0.0, 0.0]
	elif agentType=='ABCxD':
		p=[1.0,0.0,0.0,-.25, 0.0]
	elif agentType=='BACxD':
		p=[1.0,0.0,0.0,.25, 0.0]
	elif agentType=='AxBCD':
		p=[1.0,.25,0.0,0.25, 0.0]
	elif agentType=='AxBDC':
		p=[1.0,0.0,0.0,0.0,-.25]
	elif agentType=='AxBCxD':
		p=[1.0,0.0,0.0,0.0,0.0]
		#print p
	else:
		p=[0.0,0.0,0.0,0.0,0.0]
	
	#[0]*myR + [1]*otherR +[2]*max(myR-otherR, otherR-myR)+[3]*max(myR-otherR, 0)* +[4]*max(0, otherR-myR)
	happy = p[0]*aScore+p[1]*oScore+p[2]*max(aScore-oScore, oScore-aScore)+p[3]*max(aScore-oScore, 0.0)+p[4]*max(0.0, oScore-aScore)
	
	return happy
	
		

def findMinInCol(agentType, matrix, types):
	minV = float('inf')	
	for row in matrix:
		for i in range(0,len(row)):	
			if matrix[i][types.index(agentType)]<minV:
				minV = matrix[i][types.index(agentType)]
	return minV


def prettyPrint(matrix, types):
	
	print types
	for i in range(0,len(matrix)):
		row = [types[i+1]]
		for j in range(0,len(matrix)):
			row.append(str(matrix[i][j])+" ")
		print row

def outputMatrix(gmatrix, bmatrix, outFileName, types, meanRow, sdRow,label):
	outFile = open(outFileName, 'w')
	writer = csv.writer(outFile)
	
	writer.writerow(types)
	for i in range(0,len(gmatrix)):
		row = [types[i+1]]
		for j in range(0,len(gmatrix[0])):
			row.append("("+"{0:6.2f}".format(gmatrix[i][j])+','+"{0:6.2f}".format(bmatrix[i][j])+")")
		writer.writerow(row)
	writer.writerow(meanRow)
	writer.writerow(sdRow)
	writer.writerow([label])

def main(filename,label):
	combineData(filename,label)


if __name__ == "__main__":
    main(sys.argv[1],sys.argv[2])
