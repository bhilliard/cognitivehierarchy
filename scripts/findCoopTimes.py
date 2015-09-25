#!/usr/bin/python

import sys 
import csv
import string
import os
import numpy as np

def calcGamesToCooperate(fname, label):

	types = ["ABCD", "ABDC", "BACD", "BADC", "ABCxD","BACxD", "AxBCD", "AxBDC", "AxBCxD"] 
	numAgents = len(types)
	
	#create and open all files
	data = fname+label+"_rawData/"+label+"_combinedOverLearnReward.csv"
	dataOut = open(data, 'w')
	writer = csv.writer(dataOut)

	learn = fname+label+'_timeToLearn.csv'
	learnOut = open(learn, 'w')
	coop = fname+label+'_timeToCoop.csv'
	coopOut = open(coop, 'w')
	numcoop = fname+label+'_numCoop.csv'
	numCoopOut = open(numcoop, 'w')

	numGNf = fname+label+'_numGN.csv'
	numGNOut = open(numGNf, 'w')
	numNGf = fname+label+'_numNG.csv'
	numNGOut = open(numNGf, 'w')

	settleGNf = fname+label+'_settleGN.csv'
	settleGNOut = open(settleGNf, 'w')
	settleNGf = fname+label+'_settleNG.csv'
	settleNGOut = open(settleNGf, 'w')
	#print fname
	numfound = 0

	#create matrices for data and results
	foundMatrix = [[0.0 for x in range(numAgents)] for x in range(numAgents)]
	numCoop = [[0.0 for x in range(numAgents)] for x in range(numAgents)]
	numGN = [[0.0 for x in range(numAgents)] for x in range(numAgents)]
	numNG = [[0.0 for x in range(numAgents)] for x in range(numAgents)]

	firstCoop = [[[] for x in range(numAgents)] for x in range(numAgents)]
	learnTime = [[[] for x in range(numAgents)] for x in range(numAgents)]
	GNTime = [[[] for x in range(numAgents)] for x in range(numAgents)]
	NGTime = [[[] for x in range(numAgents)] for x in range(numAgents)]

	coopMatrix = [[0.0 for x in range(numAgents)] for x in range(numAgents)]
	learnMatrix = [[0.0 for x in range(numAgents)] for x in range(numAgents)]
	GNMatrix = [[0.0 for x in range(numAgents)] for x in range(numAgents)]
	NGMatrix = [[0.0 for x in range(numAgents)] for x in range(numAgents)]

	percCoop = [[0.0 for x in range(numAgents)] for x in range(numAgents)]
	percGN = [[0.0 for x in range(numAgents)] for x in range(numAgents)]
	percNG = [[0.0 for x in range(numAgents)] for x in range(numAgents)]
	
	# for all agentRewards files, pull the data into one file and add data to calculate
	# learn and "converge" points	
	for subdir in os.listdir(fname):
		if os.path.isdir(fname+subdir):
			if os.path.isfile(fname+subdir+"/agentRewards.csv"):
				outLineG=[]
				outLineB=[]
				#pull out the attribute number from file name
				attNum = subdir.split('_')[0]
				outLineG.append(attNum)
				outLineG.append('0')
				outLineB.append(attNum)
				outLineB.append('1')
				#print attNum
				toRead = fname+subdir+"/agentRewards.csv"
				numfound+=1
				#print subdir
				# pull out agent preferences from file name
				gname = subdir.split('_')[1]
				bname = subdir.split('_')[2]
				# add names to two outfile lines that will be written
				outLineG.append(gname)
				outLineG.append(bname)
				outLineB.append(gname)
				outLineB.append(bname)
				# get the index of the agent's type
				gLoc = types.index(gname)
				bLoc = types.index(bname)
				foundMatrix[gLoc][bLoc]+=1;
				lines = []
				# read in the file and split up the data into the right out lines
				with open(toRead, 'rb') as csvfile:
					f = csv.reader(csvfile, delimiter=',', quotechar='|')
					
					for row in f:
						lines.append(row)
					for i in range(2,len(lines[0])):
						outLineG.append(lines[0][i])
						outLineB.append(lines[1][i])
						

				timeToCoop = findFirstCoop(lines)
				timeToLearn = findTimeToLearn(lines)
				timeToGN = findTimeToGN(lines)
				timeToNG = findTimeToNG(lines)
				#print gname+" "+bname+"coop "+str(timeToCoop)+" learn "+str(timeToLearn)
				# if the conditions were found, add the data					
				if timeToCoop>-1:
					firstCoop[gLoc][bLoc].append(timeToCoop)
				if timeToLearn>-1:
					numCoop[gLoc][bLoc]+=1;
					learnTime[gLoc][bLoc].append(timeToLearn)
				if timeToGN>-1:
					numGN[gLoc][bLoc]+=1;
					GNTime[gLoc][bLoc].append(timeToGN)
				if timeToNG>-1:
					numNG[gLoc][bLoc]+=1;
					NGTime[gLoc][bLoc].append(timeToNG)
				writer.writerow(outLineG)
				writer.writerow(outLineB)
			else:
				print fname+subdir+"/agentRewards.csv"

	
	# calculate the means across the learning trials
	for i in range(0,len(firstCoop)):
		for j in range(0, len(firstCoop[0])):
			if len(firstCoop[i][j])>0:
				coopMatrix[i][j] = np.mean(firstCoop[i][j])
			if len(learnTime[i][j])>0:
				learnMatrix[i][j] = np.mean(learnTime[i][j])
			if len(GNTime[i][j])>0:
				GNMatrix[i][j] = np.mean(GNTime[i][j])
			if len(NGTime[i][j])>0:
				NGMatrix[i][j] = np.mean(NGTime[i][j])
			if foundMatrix[i][j]>0:
				percGN[i][j] = numGN[i][j]/foundMatrix[i][j]
				percNG[i][j] = numNG[i][j]/foundMatrix[i][j]
				percCoop[i][j] = numCoop[i][j]/foundMatrix[i][j]
				#print percCoop[i][j]
	outputMatrix(percCoop, numCoopOut, types, label+" perc. Coop")
	outputMatrix(coopMatrix, coopOut, types, label+" avg. 1st Coop")
	outputMatrix(learnMatrix, learnOut, types, label+" avg. learn  time")

	outputMatrix(percGN, numGNOut, types, label+" perc GN")
	outputMatrix(GNMatrix, settleGNOut, types, label+" avg. learn GN")
	outputMatrix(percNG, numNGOut, types, label+" perc NG")
	outputMatrix(NGMatrix, settleNGOut, types, label+" avg. learn NG")

def outputMatrix(matrix, outFile, types, label):
	writer = csv.writer(outFile)
	types.insert(0,"{0:6s}".format(" "))
	writer.writerow([label])
	writer.writerow(types)
	for i in range(0,len(matrix)):
		row = [types[i+1]]
		for j in range(0,len(matrix[0])):
			#print matrix[i][j]
			row.append("{0:6.2f}".format(matrix[i][j]))
		writer.writerow(row)
	del types[0]
	writer.writerow([])
	writer.writerow([])


def findFirstCoop(lines):
	#print lines
	for i in range(2,len(lines[1])):
		if float(lines[0][i])>0 and float(lines[1][i])>0:
			return i
	return -1

def findTimeToLearn(lines):
	#print lines
	found = False
	for i in range(2, len(lines[1])):
		#print ""+str(lines[1][i])+" "+str(lines[2][i])
		if float(lines[0][i])>0 and float(lines[1][i])>0 and not found:
			learnTime = i
			found = True
		if float(lines[0][i])==0 or float(lines[1][i])==0:
			found = False
			learnTime = -1
	return learnTime
				

def findTimeToGN(lines):
	#print lines
	found = False
	for i in range(2, len(lines[1])):
		#print ""+str(lines[1][i])+" "+str(lines[2][i])
		if float(lines[0][i])>0 and float(lines[1][i])==0 and not found:
			learnTime = i
			found = True
		if float(lines[0][i])== 0 or float(lines[1][i])>0:
			found = False
			learnTime = -1
	return learnTime

def findTimeToNG(lines):
	#print lines
	found = False
	for i in range(2, len(lines[1])):
		#print ""+str(lines[1][i])+" "+str(lines[2][i])
		if float(lines[0][i])==0 and float(lines[1][i])>0 and not found:
			learnTime = i
			found = True
		if float(lines[0][i])>0 or float(lines[1][i])==0:
			found = False
			learnTime = -1
	return learnTime

def main(filename,label):
	calcGamesToCooperate(filename,label)


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
