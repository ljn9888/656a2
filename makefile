target:
	javac sender.java
	javac ACKReceiver.java
	javac receiver.java
	javac packet.java

clean:
	$(RM) *.class


