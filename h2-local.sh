#!/bin/sh

rlwrap java -cp ./hsqldb/hsqldb.jar:./hsqldb/sqltool.jar  org.hsqldb.cmdline.SqlTool --rcfile=./sqltool.rc local "$@"
