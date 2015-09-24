#!/usr/bin/python


import sys 
import csv
import string
import os
import numpy as np
from pylab import *


def graphLearning(filename, label, a0Type, a1Type):
	# read in file
	numFound = 0.0
	
	ydata0 = []
	ydata1 = []
	ydata0_avg=[]
	ydata1_avg=[]
	toRead = filename+label+"_rawData/"+label+'_combinedOverLearnReward.csv'
	with open(toRead, 'rb') as csvfile:
		f = csv.reader(csvfile, delimiter=',', quotechar='|')
		for row in f:
			
			if (row[2]==a0Type) and (row[3]==a1Type):
				numFound +=1.0
				
				if row[1] is '0':
					if len(ydata0) == 0:
						for r in row[4:]:
							ydata0.append(float(r))
					else:
						for r in range(4,len(row)):
							ydata0[r-4]+=float(row[r])
					
				elif row[1] is '1':
					if len(ydata1) == 0:
						for r in row[4:]:
							ydata1.append(float(r))
					else:
						for r in range(4,len(row)):
							ydata1[r-4]+=float(row[r])
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

def calcLifetimeSum(dataIn):
	dataRow = []
	
	for i in dataIn:
		dataRow.append(float(i))
	summed=[dataRow[0]]
	for d in range(1,len(dataRow)):
		tosum = dataRow[0:d]
		#print toavg
		a = np.sum(tosum)
		summed.append(a)
	return summed

def graphAllAgentsSymm(filename,label):
	types = ["ABCD", "ABDC", "BACD", "BADC", "ABCxD","BACxD", "AxBCD", "AxBDC", "AxBCxD"] 
	
	for i in range(0,len(types)):
		for j in range(i,len(types)):
			graphLearning(filename,label,types[i],types[j])



if __name__ == "__main__":
	if len(sys.argv) >4:
    		graphLearning(sys.argv[1], sys.argv[2],sys.argv[3],sys.argv[4])
	else:
		graphAllAgentsSymm(sys.argv[1],sys.argv[2])

