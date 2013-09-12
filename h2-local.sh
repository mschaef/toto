#!/bin/sh

java -cp ./hsqldb/hsqldb.jar:./hsqldb/sqltool.jar  org.hsqldb.cmdline.SqlTool local "$@"
