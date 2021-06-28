@echo off

set jarname=multihandler-java-v1.1.0
set libraries=../com/*
set structure=com/vincentcodes/*

cp -r lib/com/ .

:: with Manifest and library files
cd classes
jar -cvfm %jarname%.jar Manifest.txt %libraries% %structure%
mv %jarname%.jar ..

rm -r ../com/

:: with Manifest, but without lib files
:: cd classes
:: jar -cvfm %jarname%.jar Manifest.txt %structure%
:: mv %jarname%.jar ..

cd ../src
jar -cvf %jarname%-sources.jar %structure%
mv %jarname%-sources.jar ..

pause