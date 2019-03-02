#!/bin/sh

TOTO_USERNAME=toto

# Must be root

if [ "$(id -u)" != "0" ]; then
   echo "This script must be run as root" 1>&2
   exit 1
fi

# create user and group

egrep "^${TOTO_USERNAME}" /etc/passwd >/dev/null

if [ $? -eq 0 ]; then
    echo "User exists: ${TOTO_USERNAME}"
else
    echo "Creating user: ${TOTO_USERNAME}"
    
    useradd --user-group --system ${TOTO_USERNAME}

    if [ $? -ne 0 ]; then
        echo "Could not create user: ${TOTO_USERNAME}"
        exit 1
    fi
fi

# Install jar in /usr/share

install -v --group=root --owner=root --directory /usr/share/toto
install -v --group=root --owner=root toto-standalone.jar /usr/share/toto

# create log directory

install -v --group=toto --owner=toto --directory /var/log/toto

# create data directory

install -v --group=toto --owner=toto --directory /var/lib/toto

# Configuration Files

install -v --group=root --owner=root --directory /etc/toto
install -v --group=root --owner=root logback.xml /etc/toto
install -v --group=root --owner=root config.edn /etc/toto

# toto service configuration

install -v --group=root --owner=root toto /etc/init.d

update-rc.d toto defaults

