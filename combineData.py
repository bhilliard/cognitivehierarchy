#!/usr/bin/python

import sys 
import csv
import string
import os
import numpy as np

def combineData(fname,label):
	
	types = ["ABCD", "ABDC", "BACD", "BADC", "ABCxD","BACxD", "AxBCD", "AxBDC", "AxBCxD"] 
	numAgents = len(types)

	#create the matrices that will store the information aggregated
	matrix =[["" for x in range(numAgents)] for x in range(numAgents)]
	gmatrix = [[0.0 for x in range(numAgents)] for x in range(numAgents)]
	bmatrix = [[0.0 for x in range(numAgents)] for x in range(numAgents)]
	# matrix of lists of values, will be averaged and SD calculated
	gValues = [[[] for x in range(numAgents)] for x in range(numAgents)]
	bValues = [[[] for x in range(numAgents)] for x in range(numAgents)]
	#happiness values to be averaged
	ghappyVals = [[[] for x in range(numAgents)] for x in range(numAgents)]
	bhappyVals = [[[] for x in range(numAgents)] for x in range(numAgents)]
	#standard deviation results
	gSDmatrix = [[0.0 for x in range(numAgents)] for x in range(numAgents)]
	bSDmatrix = [[0.0 for x in range(numAgents)] for x in range(numAgents)]
	#happiness results
	ghappy = [[0.0 for x in range(numAgents)] for x in range(numAgents)]
	bhappy = [[0.0 for x in range(numAgents)] for x in range(numAgents)]
	mMinusSDMatrix= [[0.0 for x in range(numAgents)] for x in range(numAgents)]
	foundMatrix = [[0.0 for x in range(numAgents)] for x in range(numAgents)]
	
	#get names from folder
	outLoc = ''+fname+'/../'+fname+label+'_combinedData.csv'
	outFile = open(outLoc, 'w')
	writer = csv.writer(outFile)

	mOutFileName = ''+fname+'/../'+fname+label+'_matrix.csv'
	sdOutFileName = ''+fname+'/../'+fname+label+'_SDmatrix.csv'
	hOutFileName = ''+fname+'/../'+fname+label+'_happymatrix.csv'	
	#counting files found to check that all files have been found
	gfound = 0
	bfound = 0
	toFind = 0
	for i in range(1,numAgents+1):
		toFind+=i
	#for every subdirectory, find the meta.txt files
	for subdir in os.listdir(fname):
		#read in meta.txt
		
		if os.path.isdir(fname+subdir):
			for nameFile in os.listdir(fname+subdir):
				if os.path.isdir(fname+subdir+"/"+nameFile):
					
					#construct the line of the output csv file
					outLine = ['GNAME','BNAME','GSCORE','BSCORE','ATTNUM','GHAPPY','BHAPPY']
					attnum = subdir.split('_')[0]
					#print "file: "+nameFile
					gname = nameFile.split('_')[2]
					bname = nameFile.split('_')[5]
					if gname in types and bname in types:
						gnameLoc = types.index(gname)
						bnameLoc = types.index(bname)
						foundMatrix[gnameLoc][bnameLoc]+=1
						if bnameLoc!=gnameLoc:					
							foundMatrix[bnameLoc][gnameLoc]+=1					
						outLine[0] = gname
						outLine[1] = bname
						outLine[4] = attnum
						metaFile = fname+subdir+"/meta.txt"
						#If there is a meta file, pull out the data
						if os.path.isfile(metaFile):
							f = open(metaFile, 'r')
							line = f.readline()
							gscore=0
							bscore=0
							#loop through the lines of the meta file
							while line != '':
								#pull out the green agent's scores
								if 'Green Agent Scores:' in line:
									gscore = float(f.readline().split(' ')[2])
									outLine[2]=str(gscore)
									gfound+=1
								#pull out the blue agent's score
								if 'Blue Agent Scores:' in line:
									f.readline()
									bscore = float(f.readline().split(' ')[1])
									outLine[3]=str(bscore)
									bfound+=1
								
								line = f.readline()
							bValues[gnameLoc][bnameLoc].append(bscore)
							if bnameLoc!=gnameLoc:
								gValues[bnameLoc][gnameLoc].append(bscore)
							gValues[gnameLoc][bnameLoc].append(gscore)
							if bnameLoc != gnameLoc:
								bValues[bnameLoc][gnameLoc].append(gscore)
							ghappyVal = happiness(gscore, bscore, gname)
							bhappyVal = happiness(bscore, gscore, bname)
							outLine[5]=str(ghappyVal)
							outLine[6]=str(bhappyVal)
							ghappyVals[gnameLoc][bnameLoc].append(ghappyVal)
							if bnameLoc!=gnameLoc:
								bhappyVals[bnameLoc][gnameLoc].append(ghappyVal)
							bhappyVals[gnameLoc][bnameLoc].append(bhappyVal)
							if bnameLoc!=gnameLoc:
								ghappyVals[bnameLoc][gnameLoc].append(bhappyVal)
							#print outLine
							writer.writerow(outLine)
						else:
							print "________META FILE NOT FOUND_______"
							print metaFile

	#calculate the averages and standard deviations
	for i in range(0,len(bmatrix)):
		for j in range(0, len(bmatrix[0])):
			bmatrix[i][j] = np.mean(bValues[i][j])
			gmatrix[i][j] = np.mean(gValues[i][j])
			ghappy[i][j]=np.mean(ghappyVals[i][j])
			bhappy[i][j]=np.mean(bhappyVals[i][j])
			bSDmatrix[i][j] = np.std(bValues[i][j])
			gSDmatrix[i][j] = np.std(gValues[i][j])
			mMinusSDMatrix[i][j] = bmatrix[i][j]-bSDmatrix[i][j]

	#going to add rows to the 
	meanRow = ["{0:15s}".format('min m')]
	sdRow = ["{0:15s}".format('min(m-SD)')]
	for c in range(0,len(bmatrix[0])):
		meanRow.append("{0:15.2f}".format(findMinInCol(types[c], bmatrix, types)))
		sdRow.append("{0:15.2f}".format(findMinInCol(types[c], mMinusSDMatrix, types)))
	#create all the matrices to be output to files
	ftypes=[]
	for t in range(0,len(types)):
		ftypes.append("{0:6s}".format(types[t]))
	ftypes.insert(0,"{0:6s}".format(' '))
	outputMatrix(ghappy,bhappy,hOutFileName,ftypes,[],[],label+" happiness ")

	outputMatrix(gmatrix, bmatrix, mOutFileName, ftypes,meanRow,sdRow,label+" Scores ")

	outputMatrix(gSDmatrix, bSDmatrix, sdOutFileName, ftypes,[],[],label+" SD data ")
	prettyPrint(bmatrix, ftypes)
	prettyPrint(gmatrix, ftypes)
	prettyPrint(foundMatrix, ftypes)
	
	print meanRow
	print sdRow
	print gfound, bfound
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
