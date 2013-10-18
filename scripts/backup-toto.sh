#!/bin/sh

(cd /usr/share/tomcat7/ectworks/toto/ && \
  tar czvf ~/toto-backups/toto-backup-`date +%Y%m%d`.tgz toto.h2db.lck toto.h2db.log toto.h2db.properties toto.h2db.script)

s3cmd sync /home/mschaef/toto-backups s3://dunitall.com

