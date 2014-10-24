#!/bin/sh
for i in `find . -maxdepth 2 -name pom.xml | grep hornetq`; do
	dir=`dirname $i`
	newDir=`echo $dir | sed 's/hornetq-/activemq6-/'`
	echo moving $dir to $newDir
	git mv $dir $newDir
done

for i in `find . -maxdepth 3 -name pom.xml | grep hornetq`; do
	dir=`dirname $i`
	newDir=`echo $dir | sed 's/hornetq-/activemq6-/'`
	echo moving $dir to $newDir
	git mv $dir $newDir
done
for i in `find . -maxdepth 4 -name pom.xml | grep hornetq`; do
	dir=`dirname $i`
	newDir=`echo $dir | sed 's/hornetq-/activemq6-/'`
	echo moving $dir to $newDir
	git mv $dir $newDir
done
for i in `find . -maxdepth 5 -name pom.xml | grep hornetq`; do
	dir=`dirname $i`
	newDir=`echo $dir | sed 's/hornetq-/activemq6-/'`
	echo moving $dir to $newDir
	git mv $dir $newDir
done
for i in `find . -maxdepth 3 -name pom.xml | grep hornetq`; do
	dir=`dirname $i`
	newDir=`echo $dir | sed 's/hornetq-/activemq6-/'`
	echo moving $dir to $newDir
	 mv $dir/* $newDir
done