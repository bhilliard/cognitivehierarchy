#!/usr/bin/python

import sys 
import csv
import string
import os
import numpy as np

def calcGamesToCooperate(fname, label):

	types = ["ABCD", "ABDC", "BACD", "BADC", "ABCxD","BACxD", "AxBCD", "AxBDC", "AxBCxD"] 
	numAgents = len(types)
	
	learn = fname+label+'_timeToLearn.csv'
	learnOut = open(learn, 'w')
	coop = fname+label+'_timeToCoop.csv'
	coopOut = open(coop, 'w')
	numcoop = fname+label+'_numCoop.csv'
	numCoopOut = open(numcoop, 'w')

	#print fname
	numfound = 0

	
	numCoop = [[0.0 for x in range(numAgents)] for x in range(numAgents)]
	firstCoop = [[[] for x in range(numAgents)] for x in range(numAgents)]
	learnTime = [[[] for x in range(numAgents)] for x in range(numAgents)]
	
	coopMatrix = [[0.0 for x in range(numAgents)] for x in range(numAgents)]
	learnMatrix = [[0.0 for x in range(numAgents)] for x in range(numAgents)]

	for subdir in os.listdir(fname):
		if os.path.isdir(fname+subdir):
			if os.path.isfile(fname+subdir+"/agentRewards.csv"):
				attNum = subdir.split('_')[0]
				#print attNum
				toRead = fname+subdir+"/agentRewards.csv"
				numfound+=1
				#print subdir
				gname = subdir.split('_')[1]
				bname = subdir.split('_')[2]
				gLoc = types.index(gname)
				bLoc = types.index(bname)
				lines = []
				with open(toRead, 'rb') as csvfile:
					f = csv.reader(csvfile, delimiter=',', quotechar='|')
					for row in f:
						lines.append(row)

				timeToCoop = findFirstCoop(lines)
				timeToLearn = findTimeToLearn(lines)
				print "coop "+str(timeToCoop)+" learn "+str(timeToLearn)
				if timeToCoop>-1:
					numCoop[gLoc][bLoc]+=1;
					firstCoop[gLoc][bLoc].append(timeToCoop)
				if timeToLearn>-1:
					learnTime[gLoc][bLoc].append(timeToLearn)
	
					
			else:
				print fname+subdir+"/agentRewards.csv"

	outputMatrix(numCoop,numCoopOut, types, label)
	for i in range(0,len(firstCoop)):
		for j in range(0, len(firstCoop[0])):
			coopMatrix[i][j] = np.mean(firstCoop[i][j])
			learnMatrix[i][j] = np.mean(learnTime[i][j])
	outputMatrix(coopMatrix, coopOut, types, label)
	outputMatrix(learnMatrix, learnOut, types, label)

def outputMatrix(matrix, outFile, types, label):
	writer = csv.writer(outFile)
	types.insert(0,"{0:6s}".format(" "))
	writer.writerow(types)
	for i in range(0,len(matrix)):
		row = [types[i+1]]
		for j in range(0,len(matrix[0])):
			row.append("{0:6.2f}".format(matrix[i][j]))
		writer.writerow(row)
	del types[0]
	writer.writerow([label])


def findFirstCoop(lines):
	print lines
	for i in range(1,len(lines[1])-1):
		if float(lines[1][i])>0 and float(lines[2][i])>0:
			return i
	return -1

def findTimeToLearn(lines):
	#print lines
	found = False
	for i in range(1, len(lines[1])-1):
		#print ""+str(lines[1][i])+" "+str(lines[2][i])
		if float(lines[1][i])>0 and float(lines[2][i])>0 and not found:
			learnTime = i
			found = True
		if float(lines[1][i])==0 or float(lines[2][i])==0:
			found = False
			learnTime = -1
	return learnTime
				

def main(filename,label):
	calcGamesToCooperate(filename,label)


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
