#!/usr/bin/python

import sys 
import csv
import string
import os

def combineClassData(fname,label):
	maxAttempt = 0
	
	outLoc = ''+fname+'/../'+fname+label+'_combinedPolicyData.csv'
	outFile = open(outLoc, 'w')
	writer = csv.writer(outFile)
	#print fname
	numfound = 0
	
	for subdir in os.listdir(fname):
		if os.path.isdir(fname+subdir):
			if os.path.isfile(fname+subdir+"/policyClasses.csv"):
				attNum = subdir.split('_')[0]
				#print attNum
				toRead = fname+subdir+"/policyClasses.csv"
				numfound+=1
				with open(toRead, 'rb') as csvfile:
					f = csv.reader(csvfile, delimiter=',', quotechar='|')
					for row in f:
						del row[-1]
						row.insert(0,attNum)
						writer.writerow(row)
			else:
				print fname+subdir+"/policyClasses.csv"
	print numfound

def main(filename,label):
	combineClassData(filename,label)


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
