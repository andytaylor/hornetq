#!/bin/sh
for i in `find . -name hornetq | grep -v target | grep -v git | grep org`; do
	parentDir=`dirname $i`
	mkdir -p $parentDir/apache/activemq6
	for j in `find $i -type d`; do
	   newDir=`echo $j | sed 's/org\/hornetq/org\/apache\/activemq6/'`
	   mkdir -p $newDir
      echo making $newDir
	done
	for j in `find $i -type f`; do
	   newFile=`echo $j | sed 's/org\/hornetq/org\/apache\/activemq6/'`
	   git mv $j $newFile
      echo moving $newFile
	done
done
for i in `find . -name *.java | grep -v target | grep -v git`; do
	sed 's/org.hornetq/org.apache.activemq6/' -i $i
done