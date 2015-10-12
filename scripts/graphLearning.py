#!/usr/bin/python


import sys
import csv
import string
import os
import numpy as np
from pylab import *
import combineData

endEpisodes = [1000,10000,20000]

def graphLearning(filename, label, a0Type, a1Type):
	# read in file
	numFound = 0.0

	ydata0 = []
	ydata1 = []
	ydata0_avg=[]
	ydata1_avg=[]

	# open the file with data pulled from agentRewards.csv file
	toRead = filename+label+"_rawData/"+label+'_combinedOverLearnReward.csv'
	with open(toRead, 'rb') as csvfile:
		f = csv.reader(csvfile, delimiter=',', quotechar='|')
		for row in f:
			# if this row matches the agent types
			if (row[2]==a0Type) and (row[3]==a1Type):
				numFound +=1.0
				#if this row is for agent0
				if row[1] is '0':
					if len(ydata0) == 0:
						for r in row[4:]:
							ydata0.append(float(r))
					else:
						for r in range(4,len(row)):
							ydata0[r-4]+=float(row[r])
				#if this row is for agent1
				elif row[1] is '1':
					if len(ydata1) == 0:
						for r in row[4:]:
							ydata1.append(float(r))
					else:
						for r in range(4,len(row)):
							ydata1[r-4]+=float(row[r])
	# if we found matching rows, calc the sums and graph
	if len(ydata0) > 0:
		for r in ydata0:
			ydata0_avg.append(r/numFound)
		for r in ydata1:
			ydata1_avg.append(r/numFound)

		y0Sum = calcLifetimeSum(ydata0_avg)
		y1Sum = calcLifetimeSum(ydata1_avg)
		xdata = [x for x in range(len(y1Sum))]
		#print xdata
		#print ydata0
		# make 2 lined graph
		plt.figure()

		l0, = plt.plot(xdata, y0Sum, '.-',label=a0Type)
		l1, = plt.plot(xdata, y1Sum, '.-',label=a1Type)
		plt.legend(handles=[l0,l1])
		plt.xlabel('Learning Iteration (in 100 games)')
		plt.ylabel('Total Lifetime Reward')
		plt.title('Lifetime Reward in '+label)
		plt.savefig(filename+'/'+label+"_lifetimeReward_"+a0Type+"_"+a1Type+".png",dpi=(640/8))
		plt.close()

		# Write a csv with the score after episode endEpisode
		toWrite = filename+label+"_rawData/"+label+'_partialLearnReward.csv'
		with open(toWrite, 'a') as csvOut:
			writer = csv.writer(csvOut, delimiter = ',')
			for endEpisode in endEpisodes:
				writer.writerow([a0Type] + [a1Type]+ [endEpisode]+[y0Sum[endEpisode/100]]+ [y1Sum[endEpisode/100]])


# calculates a running sum of the rewards
def calcLifetimeSum(dataIn):
	dataRow = []
	print dataIn
	for i in dataIn:
		dataRow.append(float(i))
	summed=[dataRow[0]]
	for d in range(1,len(dataRow)):
		tosum = dataRow[0:d]
		#print toavg
		a = np.sum(tosum)
		summed.append(a)
	return summed

def makeMatrix(inFileName, outFileName, types, label):
	partialRewardRows = [[["("+"{0:6.2f}".format(0.0)+','+"{0:6.2f}".format(0.0)+")" for x in range(3)] for x in range(len(types))] for x in range(len(types))]

	inFile = open(inFileName, 'r')
	reader = csv.reader(inFile, delimiter=',')
	outFile = open(outFileName, 'a')
	writer = csv.writer(outFile, delimiter=',')

	for row in inFile:
		inRow = row.split(',')
		partialRewardRows[types.index(inRow[0])][types.index(inRow[1])][endEpisodes.index(inRow[2])] = "("+"{0:6.2f}".format(float(inRow[3]))+','+"{0:6.2f}".format(float(inRow[4]))+")"
	inFile.close()
	for endEpisode in range(len(endEpisodes)):
		writer.writerow([label+" Reward After "+str(endEpisodes[endEpisode])+" Episodes"])
		writer.writerow(["     ",types])
		for agent0 in range(0, len(partialRewardRows)):
			row = [types[agent0]]
			for agent1 in range(0, len(partialRewardRows)):
				row.append(partialRewardRows[agent0][agent1][endEpisode])
			writer.writerow(row)
	outFile.close()




# handles graphing all combos. NOTE: this is currently only for symmetric games
def graphAllAgentsSymm(filename,label):
	types = ["ABCD", "ABDC", "BACD", "BADC", "ABCxD","BACxD", "AxBCD", "AxBDC", "AxBCxD"]

	toWrite = filename+label+"_rawData/"+label+'_partialLearnReward.csv'
	open(toWrite, 'w')
	close(toWrite)

	for i in range(0,len(types)):
		for j in range(i,len(types)):
			graphLearning(filename,label,types[i],types[j])
	makeMatrix(toWrite, filename+"../"+filename+label+'_results.csv', types, label)


if __name__ == "__main__":
	if len(sys.argv) >4:
    		graphLearning(sys.argv[1], sys.argv[2],sys.argv[3],sys.argv[4])
	else:
		graphAllAgentsSymm(sys.argv[1], sys.argv[2])
