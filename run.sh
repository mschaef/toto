#!/bin/bash

args=(${@// /\\ })

java ${args[*]} -cp target/toto-0.2.2-SNAPSHOT-standalone.jar toto.main 


