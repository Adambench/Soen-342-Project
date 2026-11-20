@echo off
REM Compile and start the PoC. Extra arguments are passed on, e.g. run.bat --demo-patients
cd /d %~dp0
chcp 65001 > nul
if exist out rmdir /s /q out
dir /s /b src\*.java > sources.txt
javac -encoding UTF-8 -d out @sources.txt && java -Dfile.encoding=UTF-8 -cp out ats.Main %*
del sources.txt
