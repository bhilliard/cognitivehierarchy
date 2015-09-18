#!/usr/bin/python

import sys 
import csv
import string
import os
import math

def createMatrix(fname, label, typeToMake):
	maxAttempt = 0
	types = ["ABCD", "ABDC", "BACD", "BADC", "ABCxD","BACxD", "AxBCD", "AxBDC", "AxBCxD"] 
	#types = ["ABDC", "BADC", "AxBCxD"]
	numAgents = len(types)
	matrix =[["" for x in range(numAgents)] for x in range(numAgents)]
	gmatrix = [[0.0 for x in range(numAgents)] for x in range(numAgents)]
	bmatrix = [[0.0 for x in range(numAgents)] for x in range(numAgents)]
	foundMatrix = [[0.0 for x in range(numAgents)] for x in range(numAgents)]
	outLoc = ''+fname+'/../'+fname+label+'_policy_'+typeToMake+'.csv'
	outFile = open(outLoc, 'w')
	
	#print fname
	found = 0
	toFind = 0
	for i in range(1,numAgents+1):
		toFind+=i

	toRead = fname+'/'+label+'_combinedPolicyData.csv'
	with open(toRead, 'rb') as csvfile:
		f = csv.reader(csvfile, delimiter=',', quotechar='|')
		for row in f:
			#print row
			found += 1
			#increment correct matrices
			if float(row[0])>maxAttempt:
				maxAttempt = int(row[0])
			gname = row[1].strip()
			bname = row[2].strip()
			gc = float(row[3])
			gd = float(row[4])
			bc = float(row[5])
			bd = float(row[6])
			gnameLoc = types.index(gname)
			bnameLoc = types.index(bname)
			toAddG = 0
			toAddB = 0
			if typeToMake=="C":
				toAddG = gc
				toAddB = bc
			elif typeToMake == "D":
				toAddG = gd
				toAddB = bd
			elif typeToMake == "CD":
				toAddG = math.floor((gc+gd)/2)
				toAddB = math.floor((bc+bd)/2)
			gmatrix[gnameLoc][bnameLoc]+=toAddG
			bmatrix[gnameLoc][bnameLoc]+=toAddB
			foundMatrix[gnameLoc][bnameLoc]+= 1
	label+=" "+str(maxAttempt)
	for i in range(0,len(bmatrix)):
		for j in range(0, len(bmatrix[0])):
			if(foundMatrix[i][j]>0):
				bmatrix[i][j] = "{0:6.2f}".format(bmatrix[i][j]/foundMatrix[i][j])
				gmatrix[i][j] = "{0:6.2f}".format(gmatrix[i][j]/foundMatrix[i][j])
			else:	
				bmatrix[i][j] = "{0:6.2f}".format(bmatrix[i][j])
				gmatrix[i][j] = "{0:6.2f}".format(gmatrix[i][j])
	ftypes=[]	
	types.insert(0," ")
	for t in range(0,len(types)):
		ftypes.append("{0:6s}".format(types[t]))
	outputMatrix(bmatrix, gmatrix, outFile, ftypes,label+" "+typeToMake)
	prettyPrint(foundMatrix,ftypes)
	toFind = maxAttempt * toFind
	print maxAttempt, toFind, found
					
def prettyPrint(matrix, types):
	
	print types
	for i in range(0,len(matrix)):
		row = [types[i+1]]
		for j in range(0,len(matrix)):
			row.append(str(matrix[i][j])+" ")
		print row		

def outputMatrix(bmatrix, gmatrix, outFile, types,label):

	writer = csv.writer(outFile)
	
	writer.writerow(types)
	for i in range(0,len(gmatrix)):
		row = [types[i+1]]
		for j in range(0,len(bmatrix)):
			row.append("("+str(gmatrix[i][j])+","+str(bmatrix[i][j])+")")
		writer.writerow(row)
	writer.writerow([label])

def main(filename,label,typeToMake):
	createMatrix(filename, label, typeToMake)

def main(filename,label):
	createMatrix(filename, label, 'C')
	createMatrix(filename, label, 'D')
	createMatrix(filename, label, 'CD')

if __name__ == "__main__":
	if len(sys.argv) >3:
    		main(sys.argv[1], sys.argv[2],sys.argv[3])
	else:
		main(sys.argv[1], sys.argv[2])
