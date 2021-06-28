#!/bin/bash
# You can compile with windows and TSTP still works on linux
first="src/com/vincentcodes/multihandler/*.java"
javac --release 9 -encoding UTF-8 -d classes -cp ./lib/*:./src/ $first