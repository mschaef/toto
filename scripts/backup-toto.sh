#!/bin/sh

(cd db && \
  tar czvf ~/toto-backups/toto-backup-`date +%Y%m%d`.tgz \
     toto.h2db.lck toto.h2db.log toto.h2db.properties toto.h2db.script toto.h2db.data)

s3cmd sync /home/toto/toto-backups s3://dunitall.com


