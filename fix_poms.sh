#!/bin/sh

for i in `find . -name pom.xml | grep -v target | grep -v git`; do
    echo $i
    sed 's/<groupId>org.hornetq/<groupId>org.apache.activemq6/' -i $i
    sed 's/<artifactId>hornetq-pom/<artifactId>activemq6-pom/' -i $i
    sed 's/<artifactId>hornetq-/<artifactId>activemq6-/' -i $i
    sed 's/<version>2.5.0-SNAPSHOT/<version>6.0.0-SNAPSHOT/' -i $i
    sed 's/<name>HornetQ/<name>ActiveMQ6/' -i $i
    sed 's/<module>hornetq-/<module>activemq6-/' -i $i
    sed 's/<module>integration\/hornetq-/<module>integration\/activemq6-/' -i $i
    sed 's/hornetq-server/activemq6-server/' -i $i
done
