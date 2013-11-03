#!/bin/bash

args=(${@// /\\ })

java ${args[*]} -cp target/toto-standalone.jar:config/local toto.main 


