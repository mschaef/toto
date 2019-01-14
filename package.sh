#!/bin/sh

if [ -z "$1" ] ; then
   echo "No Release Level Specified"
   exit 1
fi

if [ "$1" != "major" ] && [ "$1" != "minor" ] && [ "$1" != "patch" ] && \
   [ "$1" != "alpha" ] && [ "$1" != "beta" ] && [ "$1" != "rc" ] ; then

    echo "Release level \"$1\" must be one of :major , :minor , :patch , :alpha , :beta , or :rc."
    exit 1
fi
          
echo "Releasing level: $1"

lein clean && lein release $1

if [ $? -ne 0 ]
then
    echo "Build failed."
    exit 1
fi

POM_PROPERTIES_FILE=$(find . -name pom.properties)

if [ ! -r $POM_PROPERTIES_FILE ]
then
    echo "Cannot find POM properties file."
    exit 1
fi

PROJECT_VERSION=`grep version ${POM_PROPERTIES_FILE} | cut -d= -f2`
PROJECT_GROUPID=`grep groupId ${POM_PROPERTIES_FILE} | cut -d= -f2`
PROJECT_ARTIFACTID=`grep artifactId ${POM_PROPERTIES_FILE} | cut -d= -f2`

PROJECT_NAME=${PROJECT_GROUPID}-${PROJECT_ARTIFACTID}
ARTIFACT_NAME=${PROJECT_NAME}-${PROJECT_VERSION}

echo "Artifact Name: ${ARTIFACT_NAME}"

UBERJAR_FILE=$(find target -name "*-standalone.jar")

if [ ! -r $UBERJAR_FILE ]
then
    echo "Cannot find uberjar file."
    exit 1
fi

cp ${UBERJAR_FILE} install

INSTALL_PACKAGE_NAME=${PROJECT_NAME}-install

ln -s install ${INSTALL_PACKAGE_NAME}

tar czvf ${INSTALL_PACKAGE_NAME}-${PROJECT_VERSION}.tgz -h --exclude="*~" ${INSTALL_PACKAGE_NAME}

rm ${INSTALL_PACKAGE_NAME}
