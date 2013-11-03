#!/bin/bash

args=(${@// /\\ })

java ${args[*]} -cp target/toto-standalone.jar toto.main 


