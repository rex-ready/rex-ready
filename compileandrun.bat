javac -classpath . com/botbox/util/*.java
javac -classpath . se/sics/tac/aw/*.java
javac -classpath . se/sics/tac/util/*.java
javac -classpath .;lib/charts4j-1.3.jar rexready/*.java
jar cfm tacagent.jar AWManifest.txt com/botbox/util/*.class se/sics/tac/aw/*.class se/sics/tac/util/*.class
java -cp tacagent.jar;lib/charts4j-1.3.jar se/sics/tac/aw/TACAgent