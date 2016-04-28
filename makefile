JCC = javac
JFLAGS = -g
LIBRARY = java-json.jar
JVM= java
.SUFFIXES: .java .class

all:
	$(JCC) $(JFLAGS) -cp $(LIBRARY) *.java

runServer:
	$(JVM) -cp .:$(LIBRARY) Server

runClient:
	$(JVM) -cp .:$(LIBRARY) Client

clean:
	rm -f *.class
