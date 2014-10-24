#!/bin/sh
for i in `find . -maxdepth 3 -name *.iml | grep activemq`; do
	dir=`dirname $i`
	newDir=`echo $dir | sed 's/hornetq-/hornetq-/'`
	echo moving $dir to $newDir
	 mv $dir/* $newDir
done
