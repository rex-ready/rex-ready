@echo off
For /f "tokens=1-4 delims=/ " %%a in ('date /t') do (set mydate=%%a%%b%%c)
For /f "tokens=1-2 delims=/:" %%a in ('time /t') do (set mytime=%%a%%b)
@echo on

javac -classpath . com/botbox/util/*.java
javac -classpath . se/sics/tac/aw/*.java
javac -classpath . se/sics/tac/util/*.java
javac -classpath .;lib/charts4j-1.3.jar rexready/*.java
jar cfm tacagent.jar AWManifest.txt com/botbox/util/*.class se/sics/tac/aw/*.class se/sics/tac/util/*.class
java -cp tacagent.jar;lib/charts4j-1.3.jar se/sics/tac/aw/TACAgent > miningData/%mytime%_%mydate%_testData.txt 2>&1