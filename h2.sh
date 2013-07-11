#!/bin/sh

java  -cp ./libs/hsqldb-2.2.9/hsqldb/lib/hsqldb.jar:./libs/hsqldb-2.2.9/hsqldb/lib/sqltool.jar  org.hsqldb.cmdline.SqlTool "$@"
