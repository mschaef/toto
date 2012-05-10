#!/bin/sh

./libs/h2/bin/h2-shell.sh -url jdbc:h2:~/ectworks/toto/toto.h2db -user sa \
  < ./src/main/resources/schema.sql