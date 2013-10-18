#!/bin/sh

die () {
    echo >&2 "$1"
    exit 1
}

if [ ! -f "$1" ] ; then
   die "War file does not exist or is not specified: $1"
fi

if [ "`whoami`" != "root" ]
then
   die "Error: script must be run as root";
fi


service tomcat7 stop

rm -fr /var/lib/tomcat7/webapps/ROOT
rm -fr /var/lib/tomcat7/webapps/ROOT.war

cp $1 ROOT.war

cp ROOT.war /var/lib/tomcat7/webapps

service tomcat7 start

