EE 382N Assignment 3

Authors:
	Puneet Bansal
	Sadaf Syed

Pre-Requisites
	1. Hadoop 1.1.2 binaries are on the path.
	2. Hadoop DFS already exists and is set up correctly.
	
Execution Instructions
	1. Unzip psanalyzer.zip to a folder.
	2. On the command line navigate to psanalyzer folder.
	3. Grant executable permissions to shell script and jar file using the following command
			
			sudo chmod 777 textanalyzer.*
			
	4. Execute using following syntax
	
			./textanalyzer.sh <contextword> <queryword>
			
			for example ./textanalyzer.sh Jane Elizabeth
			
		
Note - The code has been tested with Hadoop 1.1.2.			

Compilation and jar creation instructions:
	1. On command line navigate to psanalyzer/source/src folder.
	2. Execute the following commands
	
			javac -classpath ./hadoop-core-1.1.2.jar *.java -d ../bin
			cd ../bin
			jar -cfe textanalyzer.jar TextAnalyzer *.class
			
	4. The jar textanalyzer.jar will be placed in psanalyzer/source/bin folder. This jar can now be used with Hadoop.		
