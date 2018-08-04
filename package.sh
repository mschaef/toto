#!/bin/sh

POM_PROPERTIES_FILE=./target/classes/META-INF/maven/toto/toto/pom.properties

lein clean && lein uberjar

if [ $? -ne 0 ]
then
    echo "Build failed."
    exit 1
fi

if [ ! -r $POM_PROPERTIES_FILE ]
then
    echo "Cannot find POM properties file."
    exit 1
fi

cp target/toto-standalone.jar toto-install

PROJECT_VERSION=`grep version ${POM_PROPERTIES_FILE} | cut -d= -f2`

tar czvf toto-install-${PROJECT_VERSION}.tgz --exclude="*~" toto-install 
