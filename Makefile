make:
	javac -cp .:External_Jars/jsch-0.1.55.jar:External_Jars/postgresql-42.2.24.jar Controller.java

run: 
	java -cp .:External_Jars/jsch-0.1.55.jar:External_Jars/postgresql-42.2.24.jar Controller