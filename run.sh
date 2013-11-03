#!/bin/bash

args=(${@// /\\ })

java ${args[*]} -cp config/local:target/toto-standalone.jar toto.main 


