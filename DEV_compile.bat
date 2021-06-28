@echo off

set first=src/com/vincentcodes/multihandler/*.java
:: .java files are in encoding UTF-8
javac --release 9 -encoding UTF-8 -d classes -cp ./lib/*;./src/ %first%

pause