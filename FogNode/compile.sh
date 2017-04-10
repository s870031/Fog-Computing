#!/bin/bash
PROJDIR=$(pwd)
# Compile java code and save byte code to bin
#javac -sourcepath $PROJDIR/src/  -d  $PROJDIR/bin/ $PROJDIR/src/Project1.java
javac -d $PROJDIR/bin $PROJDIR/src/*.java
