@echo off

set jarname=multihandler-java-v2.0.1-beta
set libraries=../com/*
set structure=com/vincentcodes/*


:: with Manifest and library files
cp -r lib/com/ .
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